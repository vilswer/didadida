package com.didadida.danmaku.service.impl;

import com.alibaba.fastjson.JSON;
import com.didadida.danmaku.constant.DanmakuRedisKey;
import com.didadida.danmaku.dto.DanmakuDTO;
import com.didadida.danmaku.dto.DanmakuMessageDTO;
import com.didadida.danmaku.entity.Danmaku;
import com.didadida.danmaku.feign.UserFeignClient;
import com.didadida.danmaku.mapper.DanmakuMapper;
import com.didadida.danmaku.service.DanmakuService;
import com.didadida.danmaku.vo.DanmakuVO;
import com.didadida.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 弹幕服务实现类
 */
@Service
@Slf4j
public class DanmakuServiceImpl implements DanmakuService {

    @Autowired
    @Qualifier("danmakuRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private DanmakuMapper danmakuMapper;

    @Autowired
    private UserFeignClient userFeignClient;

    /**
     * 弹幕topic
     */
    private static final String DANMAKU_SYNC_TOPIC = "danmaku_sync_topic";

    /**
     * 发送弹幕
     * @param danmakuDTO 弹幕数据
     * @param userId 用户ID
     * @return 弹幕ID
     */
    @Override
    public Long sendDanmaku(DanmakuDTO danmakuDTO, Long userId) {
        // 生成弹幕ID（使用时间戳+随机数）
        Long danmakuId = System.currentTimeMillis() + (long) (Math.random() * 1000);

        // 构建弹幕VO
        DanmakuVO danmakuVO = new DanmakuVO();
        BeanUtils.copyProperties(danmakuDTO, danmakuVO);
        danmakuVO.setDanmakuId(danmakuId);
        danmakuVO.setUserId(userId);
        danmakuVO.setCreateTime(LocalDateTime.now());

        // 获取用户信息
        try {
            Result<Map<String, Object>> userResult = userFeignClient.getUserInfo(userId);
            if (userResult.getCode() == 200 && userResult.getData() != null) {
                Map<String, Object> userInfo = userResult.getData();
                danmakuVO.setNickname((String) userInfo.get("nickname"));
                danmakuVO.setAvatar((String) userInfo.get("avatar"));
            }
        } catch (Exception e) {
            log.error("获取用户信息失败，userId: {}", userId, e);
            // 降级处理
            danmakuVO.setNickname("未知用户");
            danmakuVO.setAvatar("");
        }

        // 将弹幕添加到Redis列表
        String redisKey = DanmakuRedisKey.buildDanmakuVideoListKey(danmakuDTO.getVideoId());
        redisTemplate.opsForList().rightPush(redisKey, JSON.toJSONString(danmakuVO));
        // 设置过期时间为24小时
        redisTemplate.expire(redisKey, 24, TimeUnit.HOURS);

        // 增加视频弹幕计数
        String countKey = DanmakuRedisKey.buildDanmakuVideoCountKey(danmakuDTO.getVideoId());
        redisTemplate.opsForValue().increment(countKey);
        // 设置过期时间为24小时
        redisTemplate.expire(countKey, 24, TimeUnit.HOURS);

        // 构建Kafka消息
        DanmakuMessageDTO messageDTO = new DanmakuMessageDTO();
        BeanUtils.copyProperties(danmakuDTO, messageDTO);
        messageDTO.setUserId(userId);
        messageDTO.setTimestamp(System.currentTimeMillis());

        // 异步发送到Kafka
        new Thread(() -> {
            try {
                kafkaTemplate.send(DANMAKU_SYNC_TOPIC, JSON.toJSONString(messageDTO));
                log.info("弹幕消息发送到Kafka成功，danmakuId: {}", danmakuId);
            } catch (Exception e) {
                log.error("弹幕消息发送到Kafka失败，danmakuId: {}", danmakuId, e);
                // 可以考虑添加到本地队列，稍后重试
            }
        }).start();

        return danmakuId;
    }

    /**
     * 获取视频的弹幕列表
     * @param videoId 视频ID
     * @param startTime 开始时间（秒）
     * @param endTime 结束时间（秒）
     * @return 弹幕列表
     */
    @Override
    public List<DanmakuVO> getVideoDanmakus(Long videoId, Double startTime, Double endTime) {
        List<DanmakuVO> danmakuList = new ArrayList<>();

        try {
            // 从Redis获取弹幕列表
            String redisKey = DanmakuRedisKey.buildDanmakuVideoListKey(videoId);
            List<Object> danmakuJsonList = redisTemplate.opsForList().range(redisKey, 0, -1);

            if (danmakuJsonList != null && !danmakuJsonList.isEmpty()) {
                for (Object danmakuJsonObj : danmakuJsonList) {
                    String danmakuJson = String.valueOf(danmakuJsonObj);
                    DanmakuVO danmakuVO = JSON.parseObject(danmakuJson, DanmakuVO.class);
                    // 过滤时间范围内的弹幕
                    if (danmakuVO.getVideoTime().doubleValue() >= startTime && 
                        danmakuVO.getVideoTime().doubleValue() <= endTime) {
                        danmakuList.add(danmakuVO);
                    }
                }
                return danmakuList;
            }
        } catch (Exception e) {
            log.error("从Redis获取弹幕列表失败，videoId: {}", videoId, e);
            // 降级到数据库查询
        }

        // 从数据库查询
        try {
            List<Danmaku> danmakus = danmakuMapper.selectByVideoIdAndTimeRange(videoId, startTime, endTime);
            for (Danmaku danmaku : danmakus) {
                DanmakuVO danmakuVO = new DanmakuVO();
                BeanUtils.copyProperties(danmaku, danmakuVO);
                // 获取用户信息
                try {
                    Result<Map<String, Object>> userResult = userFeignClient.getUserInfo(danmaku.getUserId());
                    if (userResult.getCode() == 200 && userResult.getData() != null) {
                        Map<String, Object> userInfo = userResult.getData();
                        danmakuVO.setNickname((String) userInfo.get("nickname"));
                        danmakuVO.setAvatar((String) userInfo.get("avatar"));
                    }
                } catch (Exception ex) {
                    log.error("获取用户信息失败，userId: {}", danmaku.getUserId(), ex);
                    // 降级处理
                    danmakuVO.setNickname("未知用户");
                    danmakuVO.setAvatar("");
                }
                danmakuList.add(danmakuVO);
            }
        } catch (Exception e) {
            log.error("从数据库获取弹幕列表失败，videoId: {}", videoId, e);
        }

        return danmakuList;
    }

    /**
     * 获取视频的弹幕数量
     * @param videoId 视频ID
     * @return 弹幕数量
     */
    @Override
    public Integer getVideoDanmakuCount(Long videoId) {
        try {
            // 从Redis获取弹幕数量
            String countKey = DanmakuRedisKey.buildDanmakuVideoCountKey(videoId);
            Object countObj = redisTemplate.opsForValue().get(countKey);
            if (countObj != null) {
                String countStr = String.valueOf(countObj);
                return Integer.parseInt(countStr);
            }
        } catch (Exception e) {
            log.error("从Redis获取弹幕数量失败，videoId: {}", videoId, e);
            // 降级到数据库查询
        }

        // 从数据库查询
        try {
            Integer count = danmakuMapper.selectCountByVideoId(videoId);
            // 将结果缓存到Redis
            String countKey = DanmakuRedisKey.buildDanmakuVideoCountKey(videoId);
            redisTemplate.opsForValue().set(countKey, count, 24, TimeUnit.HOURS);
            return count;
        } catch (Exception e) {
            log.error("从数据库获取弹幕数量失败，videoId: {}", videoId, e);
            return 0;
        }
    }

    /**
     * 同步弹幕到数据库
     * @param danmakuMessageDTO 弹幕消息
     */
    @Override
    public void syncDanmakuToMysql(DanmakuMessageDTO danmakuMessageDTO) {
        try {
            Danmaku danmaku = new Danmaku();
            danmaku.setVideoId(danmakuMessageDTO.getVideoId());
            danmaku.setUserId(danmakuMessageDTO.getUserId());
            danmaku.setContent(danmakuMessageDTO.getContent());
            danmaku.setVideoTime(danmakuMessageDTO.getVideoTime());
            danmaku.setColor(danmakuMessageDTO.getColor());
            danmaku.setFontSize(danmakuMessageDTO.getFontSize());
            danmaku.setMode(danmakuMessageDTO.getMode());
            danmaku.setCreateTime(LocalDateTime.now());

            danmakuMapper.insert(danmaku);
            log.info("弹幕同步到数据库成功，videoId: {}, userId: {}", 
                     danmakuMessageDTO.getVideoId(), danmakuMessageDTO.getUserId());
        } catch (Exception e) {
            log.error("弹幕同步到数据库失败", e);
        }
    }
}
