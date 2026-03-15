package com.didadida.video.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.didadida.video.constant.VideoRedisKey;
import com.didadida.video.dto.VideoInteractionDTO;
import com.didadida.video.dto.VideoViewReportDTO;
import com.didadida.video.entity.Video;
import com.didadida.video.entity.VideoEpisode;
import com.didadida.video.mapper.VideoEpisodeMapper;
import com.didadida.video.mapper.VideoMapper;
import com.didadida.video.service.VideoService;
import com.didadida.video.storage.StorageService;
import com.didadida.video.vo.VideoEpisodeVO;
import com.didadida.video.vo.VideoPlayDetailVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.BloomFilter;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final VideoMapper videoMapper;
    private final VideoEpisodeMapper videoEpisodeMapper;
    private final StorageService storageService;
    private final StringRedisTemplate stringRedisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final BloomFilter<Long> videoIdBloomFilter;
    /**
     * 预签名URL过期时间（12小时，足够看完视频，防盗链）
     */
    private static final int PLAY_URL_EXPIRE_SECONDS =43200;

    // 分布式锁Key前缀
    private static final String LOCK_KEY_PREFIX = "video:lock:info:";
    // 空值缓存过期时间（分钟）
    private static final long NULL_VALUE_EXPIRE_MINUTE = 5;

    @Override
    public VideoPlayDetailVO getVideoPlayDetail(Long videoId) {
        long startTime = System.currentTimeMillis();
        log.info("开始处理getVideoPlayDetail，videoId:{}, 开始时间:{}", videoId, startTime);

        // 1. 布隆过滤器：过滤不存在的视频ID（防缓存穿透）
        long bloomFilterStart = System.currentTimeMillis();
        if (!videoIdBloomFilter.mightContain(videoId)) {
            log.info("布隆过滤器过滤，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - bloomFilterStart);
            throw new RuntimeException("视频不存在");
        }
        log.info("布隆过滤器检查完成，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - bloomFilterStart);

        String videoInfoKey = VideoRedisKey.VIDEO_INFO_PREFIX + videoId;
        String videoInfoJson;
        Video video = null;

        // 2. 查Redis缓存（带超时处理）
        long redisStart = System.currentTimeMillis();
        try {
            videoInfoJson = stringRedisTemplate.opsForValue().get(videoInfoKey);
            if (videoInfoJson != null) {
                // 缓存命中：检查是否为空值（防穿透）
                if ("null".equals(videoInfoJson)) {
                    log.info("Redis缓存命中空值，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - redisStart);
                    throw new RuntimeException("视频不存在");
                }
                // 反序列化
                try {
                    video = objectMapper.readValue(videoInfoJson, Video.class);
                    log.info("Redis缓存命中并反序列化成功，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - redisStart);
                } catch (Exception e) {
                    log.error("反序列化失败，videoId:{}", videoId, e);
                    log.info("反序列化失败，开始加锁查库，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - redisStart);
                    long lockStart = System.currentTimeMillis();
                    video = getVideoWithLock(videoId); // 加锁查库（防击穿）
                    log.info("加锁查库完成，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - lockStart);
                }
            } else {
                // 缓存未命中：加分布式锁查库（防缓存击穿）
                log.info("Redis缓存未命中，开始加锁查库，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - redisStart);
                long lockStart = System.currentTimeMillis();
                video = getVideoWithLock(videoId);
                log.info("加锁查库完成，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - lockStart);
            }
        } catch (Exception e) {
            log.error("Redis操作失败，videoId:{}", videoId, e);
            log.info("Redis操作失败，开始直接从数据库查询，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - redisStart);
            // Redis失败时，直接从数据库查询
            long dbStart = System.currentTimeMillis();
            video = getVideoFromMysql(videoId);
            log.info("数据库查询完成，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - dbStart);
        }
        log.info("Redis缓存操作完成，videoId:{}, 总耗时:{}ms", videoId, System.currentTimeMillis() - redisStart);

        // 3. 校验视频状态：审核通过+公开可见
        long statusCheckStart = System.currentTimeMillis();
        if (video == null || video.getAuditStatus() != 1 || video.getStatus() != 1) {
            log.info("视频状态校验失败，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - statusCheckStart);
            throw new RuntimeException("视频不存在或已下架");
        }
        log.info("视频状态校验完成，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - statusCheckStart);

        // 4. 生成预签名播放地址（缓存）
        String playUrlKey = "video:play:url:" + video.getVideoUrl();
        String playUrl = null;
        long presignedUrlStart = System.currentTimeMillis();
        try {
            playUrl = stringRedisTemplate.opsForValue().get(playUrlKey);
            if (playUrl == null) {
                // 生成新的预签名URL
                log.info("预签名URL缓存未命中，开始生成新URL，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - presignedUrlStart);
                try {
                    long minioStart = System.currentTimeMillis();
                    playUrl = storageService.generatePresignedPlayUrl(video.getVideoUrl(), PLAY_URL_EXPIRE_SECONDS);
                    log.info("MinIO生成预签名URL完成，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - minioStart);
                    // 缓存预签名URL，过期时间比预签名URL短1小时，确保URL始终有效
                    stringRedisTemplate.opsForValue().set(playUrlKey, playUrl, PLAY_URL_EXPIRE_SECONDS - 3600, TimeUnit.SECONDS);
                    log.info("预签名URL缓存完成，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - presignedUrlStart);
                } catch (Exception e) {
                    log.error("生成预签名URL失败，videoId:{}, videoUrl:{}", videoId, video.getVideoUrl(), e);
                    // MinIO失败时，使用占位符URL
                    playUrl = "#";
                    log.info("MinIO失败，使用占位符URL，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - presignedUrlStart);
                }
            } else {
                log.info("预签名URL缓存命中，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - presignedUrlStart);
            }
        } catch (Exception e) {
            log.error("Redis操作失败，videoId:{}", videoId, e);
            // Redis失败时，尝试生成预签名URL
            log.info("Redis操作失败，开始尝试生成预签名URL，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - presignedUrlStart);
            try {
                long minioStart = System.currentTimeMillis();
                playUrl = storageService.generatePresignedPlayUrl(video.getVideoUrl(), PLAY_URL_EXPIRE_SECONDS);
                log.info("MinIO生成预签名URL完成，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - minioStart);
            } catch (Exception ex) {
                log.error("生成预签名URL失败，videoId:{}, videoUrl:{}", videoId, video.getVideoUrl(), ex);
                // MinIO也失败时，使用占位符URL
                playUrl = "#";
                log.info("MinIO也失败，使用占位符URL，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - presignedUrlStart);
            }
        }
        log.info("预签名URL生成完成，videoId:{}, 总耗时:{}ms", videoId, System.currentTimeMillis() - presignedUrlStart);

        // 5. 异步检查文件存在（不阻塞主请求）
        long asyncCheckStart = System.currentTimeMillis();
        final Long finalVideoId = videoId;
        final String finalVideoUrl = video.getVideoUrl();
        new Thread(() -> {
            long threadStart = System.currentTimeMillis();
            try {
                boolean fileExists = storageService.checkFileExists(finalVideoUrl);
                if (!fileExists) {
                    log.warn("视频源文件不存在，videoId:{}, videoUrl:{}, 耗时:{}ms", finalVideoId, finalVideoUrl, System.currentTimeMillis() - threadStart);
                } else {
                    log.info("视频源文件存在，videoId:{}, videoUrl:{}, 耗时:{}ms", finalVideoId, finalVideoUrl, System.currentTimeMillis() - threadStart);
                }
            } catch (Exception e) {
                log.error("检查文件存在失败，videoId:{}, videoUrl:{}, 耗时:{}ms", finalVideoId, finalVideoUrl, System.currentTimeMillis() - threadStart, e);
            }
        }).start();
        log.info("异步检查文件存在任务已启动，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - asyncCheckStart);

        // 6. 批量从Redis获取实时互动数据（比MySQL更准）
        Long realViewCount = video.getViewCount();
        Long realLikeCount = video.getLikeCount();
        Long realCoinCount = video.getCoinCount();
        Long realCollectCount = video.getCollectCount();
        Long realShareCount = video.getShareCount();

        long interactionDataStart = System.currentTimeMillis();
        try {
            String viewCountKey = VideoRedisKey.VIDEO_VIEW_COUNT_PREFIX + videoId;
            String likeCountKey = VideoRedisKey.VIDEO_LIKE_COUNT_PREFIX + videoId;
            String coinCountKey = VideoRedisKey.VIDEO_COIN_COUNT_PREFIX + videoId;
            String collectCountKey = VideoRedisKey.VIDEO_COLLECT_COUNT_PREFIX + videoId;
            String shareCountKey = VideoRedisKey.VIDEO_SHARE_COUNT_PREFIX + videoId;

            // 批量获取互动数据
            List<String> keys = Arrays.asList(viewCountKey, likeCountKey, coinCountKey, collectCountKey, shareCountKey);
            List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);

            // 解析数据
            if (values != null) {
                realViewCount = values.get(0) != null ? Long.parseLong(values.get(0)) : video.getViewCount();
                realLikeCount = values.get(1) != null ? Long.parseLong(values.get(1)) : video.getLikeCount();
                realCoinCount = values.get(2) != null ? Long.parseLong(values.get(2)) : video.getCoinCount();
                realCollectCount = values.get(3) != null ? Long.parseLong(values.get(3)) : video.getCollectCount();
                realShareCount = values.get(4) != null ? Long.parseLong(values.get(4)) : video.getShareCount();
                log.info("批量获取互动数据成功，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - interactionDataStart);
            } else {
                log.info("批量获取互动数据为空，使用数据库数据，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - interactionDataStart);
            }
        } catch (Exception e) {
            log.error("获取互动数据失败，videoId:{}", videoId, e);
            // Redis失败时，使用数据库中的数据
            log.info("获取互动数据失败，使用数据库数据，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - interactionDataStart);
        }
        log.info("互动数据获取完成，videoId:{}, 总耗时:{}ms", videoId, System.currentTimeMillis() - interactionDataStart);

        // 7. 封装返回VO
        long voStart = System.currentTimeMillis();
        VideoPlayDetailVO playDetailVO = new VideoPlayDetailVO();
        BeanUtils.copyProperties(video, playDetailVO);
        playDetailVO.setPlayUrl(playUrl);
        playDetailVO.setRealViewCount(realViewCount);
        playDetailVO.setLikeCount(realLikeCount);
        playDetailVO.setCoinCount(realCoinCount);
        playDetailVO.setCollectCount(realCollectCount);
        playDetailVO.setShareCount(realShareCount);
        log.info("封装返回VO完成，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - voStart);

        log.info("getVideoPlayDetail处理完成，videoId:{}, 总耗时:{}ms", videoId, System.currentTimeMillis() - startTime);
        return playDetailVO;
    }

    @Override
    @SentinelResource(value = "reportVideoView", blockHandler = "handleBlockException")
    public void reportVideoView(Long videoId) {
        // 高并发核心：直接操作Redis，毫秒级响应，不写MySQL
        String viewCountKey = VideoRedisKey.VIDEO_VIEW_COUNT_PREFIX + videoId;

        // 1. Redis原子递增播放量
        Long increment = stringRedisTemplate.opsForValue().increment(viewCountKey);
        // 首次递增，初始化过期时间（24小时，避免冷数据永久占用内存）
        if (increment != null && increment == 1) {
            stringRedisTemplate.expire(viewCountKey, 24, TimeUnit.HOURS);
        }

        // 2. 发送Kafka消息，异步批量同步到MySQL，解耦高并发写库
        try {
            VideoViewReportDTO reportDTO = new VideoViewReportDTO(videoId, System.currentTimeMillis());
            // 异步发送Kafka消息，不阻塞主请求
            new Thread(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    kafkaTemplate.send(
                            VideoRedisKey.VIDEO_VIEW_SYNC_TOPIC,
                            objectMapper.writeValueAsString(reportDTO)
                    );
                    log.info("播放量上报Kafka消息发送成功，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - startTime);
                } catch (Exception e) {
                    log.error("播放量上报Kafka消息发送失败，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - startTime, e);
                    // Kafka失败不影响主流程
                }
            }).start();
        } catch (Exception e) {
            log.error("播放量上报Kafka消息准备失败，videoId:{}", videoId, e);
        }

        log.debug("视频播放量上报成功，videoId:{}, 当前播放量:{}", videoId, increment);
    }

    @Override
    @SentinelResource(value = "likeVideo", blockHandler = "handleBlockException")
    public void likeVideo(Long videoId, Long userId) {
        // 1. 布隆过滤器：过滤不存在的视频ID
        if (!videoIdBloomFilter.mightContain(videoId)) {
            throw new RuntimeException("视频不存在");
        }

        // 2. 检查用户是否已点赞
        String userLikeKey = VideoRedisKey.VIDEO_USER_LIKE_PREFIX + userId + ":" + videoId;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(userLikeKey))) {
            throw new RuntimeException("已经点过赞了");
        }

        // 3. 原子操作：增加点赞数
        String likeCountKey = VideoRedisKey.VIDEO_LIKE_COUNT_PREFIX + videoId;
        Long increment = stringRedisTemplate.opsForValue().increment(likeCountKey);
        if (increment != null && increment == 1) {
            // 首次点赞，初始化过期时间
            stringRedisTemplate.expire(likeCountKey, VideoRedisKey.INTERACTION_EXPIRE_DAY, TimeUnit.DAYS);
        }

        // 4. 记录用户点赞状态
        stringRedisTemplate.opsForValue().set(userLikeKey, "1", VideoRedisKey.INTERACTION_EXPIRE_DAY, TimeUnit.DAYS);

        // 5. 发送Kafka消息，异步同步到MySQL
        try {
            VideoInteractionDTO interactionDTO = new VideoInteractionDTO(videoId, userId, "like", System.currentTimeMillis());
            String message = objectMapper.writeValueAsString(interactionDTO);
            // 异步发送Kafka消息，不阻塞主请求
            new Thread(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    kafkaTemplate.send(
                            VideoRedisKey.VIDEO_INTERACTION_SYNC_TOPIC,
                            message
                    );
                    log.info("点赞Kafka消息发送成功，videoId:{}, userId:{}, 耗时:{}ms", videoId, userId, System.currentTimeMillis() - startTime);
                } catch (Exception e) {
                    log.error("Kafka消息发送失败，videoId:{}, userId:{}, 耗时:{}ms", videoId, userId, System.currentTimeMillis() - startTime, e);
                    // 继续执行，不影响主流程
                }
            }).start();
        } catch (Exception e) {
            log.error("点赞消息序列化失败，videoId:{}, userId:{}", videoId, userId, e);
        }

        log.debug("视频点赞成功，videoId:{}, userId:{}", videoId, userId);
    }

    @Override
    @SentinelResource(value = "unlikeVideo", blockHandler = "handleBlockException")
    public void unlikeVideo(Long videoId, Long userId) {
        // 1. 布隆过滤器：过滤不存在的视频ID
        if (!videoIdBloomFilter.mightContain(videoId)) {
            throw new RuntimeException("视频不存在");
        }

        // 2. 检查用户是否已点赞
        String userLikeKey = VideoRedisKey.VIDEO_USER_LIKE_PREFIX + userId + ":" + videoId;
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(userLikeKey))) {
            throw new RuntimeException("还没有点过赞");
        }

        // 3. 原子操作：减少点赞数
        String likeCountKey = VideoRedisKey.VIDEO_LIKE_COUNT_PREFIX + videoId;
        stringRedisTemplate.opsForValue().decrement(likeCountKey);

        // 4. 删除用户点赞状态
        stringRedisTemplate.delete(userLikeKey);

        // 5. 发送Kafka消息，异步同步到MySQL
        try {
            VideoInteractionDTO interactionDTO = new VideoInteractionDTO(videoId, userId, "unlike", System.currentTimeMillis());
            String message = objectMapper.writeValueAsString(interactionDTO);
            // 异步发送Kafka消息，不阻塞主请求
            new Thread(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    kafkaTemplate.send(
                            VideoRedisKey.VIDEO_INTERACTION_SYNC_TOPIC,
                            message
                    );
                    log.info("取消点赞Kafka消息发送成功，videoId:{}, userId:{}, 耗时:{}ms", videoId, userId, System.currentTimeMillis() - startTime);
                } catch (Exception e) {
                    log.error("Kafka消息发送失败，videoId:{}, userId:{}, 耗时:{}ms", videoId, userId, System.currentTimeMillis() - startTime, e);
                    // 继续执行，不影响主流程
                }
            }).start();
        } catch (Exception e) {
            log.error("取消点赞消息序列化失败，videoId:{}, userId:{}", videoId, userId, e);
        }

        log.debug("取消视频点赞成功，videoId:{}, userId:{}", videoId, userId);
    }

    @Override
    @SentinelResource(value = "coinVideo", blockHandler = "handleBlockException")
    public void coinVideo(Long videoId, Long userId, Integer coinCount) {
        // 1. 布隆过滤器：过滤不存在的视频ID
        if (!videoIdBloomFilter.mightContain(videoId)) {
            throw new RuntimeException("视频不存在");
        }

        // 2. 验证投币数量
        if (coinCount <= 0 || coinCount > 10) {
            throw new RuntimeException("投币数量必须在1-10之间");
        }

        // 3. 原子操作：增加投币数
        String coinCountKey = VideoRedisKey.VIDEO_COIN_COUNT_PREFIX + videoId;
        Long increment = stringRedisTemplate.opsForValue().increment(coinCountKey, coinCount);
        if (increment != null && increment.equals(Long.valueOf(coinCount))) {
            // 首次投币，初始化过期时间
            stringRedisTemplate.expire(coinCountKey, VideoRedisKey.INTERACTION_EXPIRE_DAY, TimeUnit.DAYS);
        }

        // 4. 发送Kafka消息，异步同步到MySQL
        try {
            VideoInteractionDTO interactionDTO = new VideoInteractionDTO(videoId, userId, "coin", coinCount, System.currentTimeMillis());
            String message = objectMapper.writeValueAsString(interactionDTO);
            // 异步发送Kafka消息，不阻塞主请求
            new Thread(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    kafkaTemplate.send(
                            VideoRedisKey.VIDEO_INTERACTION_SYNC_TOPIC,
                            message
                    );
                    log.info("投币Kafka消息发送成功，videoId:{}, userId:{}, coinCount:{}, 耗时:{}ms", videoId, userId, coinCount, System.currentTimeMillis() - startTime);
                } catch (Exception e) {
                    log.error("Kafka消息发送失败，videoId:{}, userId:{}, coinCount:{}, 耗时:{}ms", videoId, userId, coinCount, System.currentTimeMillis() - startTime, e);
                    // 继续执行，不影响主流程
                }
            }).start();
        } catch (Exception e) {
            log.error("投币消息序列化失败，videoId:{}, userId:{}", videoId, userId, e);
        }

        log.debug("视频投币成功，videoId:{}, userId:{}, coinCount:{}", videoId, userId, coinCount);
    }

    @Override
    @SentinelResource(value = "collectVideo", blockHandler = "handleBlockException")
    public void collectVideo(Long videoId, Long userId) {
        // 1. 布隆过滤器：过滤不存在的视频ID
        if (!videoIdBloomFilter.mightContain(videoId)) {
            throw new RuntimeException("视频不存在");
        }

        // 2. 检查用户是否已收藏
        String userCollectKey = VideoRedisKey.VIDEO_USER_COLLECT_PREFIX + userId + ":" + videoId;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(userCollectKey))) {
            throw new RuntimeException("已经收藏过了");
        }

        // 3. 原子操作：增加收藏数
        String collectCountKey = VideoRedisKey.VIDEO_COLLECT_COUNT_PREFIX + videoId;
        Long increment = stringRedisTemplate.opsForValue().increment(collectCountKey);
        if (increment != null && increment == 1) {
            // 首次收藏，初始化过期时间
            stringRedisTemplate.expire(collectCountKey, VideoRedisKey.INTERACTION_EXPIRE_DAY, TimeUnit.DAYS);
        }

        // 4. 记录用户收藏状态
        stringRedisTemplate.opsForValue().set(userCollectKey, "1", VideoRedisKey.INTERACTION_EXPIRE_DAY, TimeUnit.DAYS);

        // 5. 发送Kafka消息，异步同步到MySQL
        try {
            VideoInteractionDTO interactionDTO = new VideoInteractionDTO(videoId, userId, "collect", System.currentTimeMillis());
            String message = objectMapper.writeValueAsString(interactionDTO);
            // 异步发送Kafka消息，不阻塞主请求
            new Thread(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    kafkaTemplate.send(
                            VideoRedisKey.VIDEO_INTERACTION_SYNC_TOPIC,
                            message
                    );
                    log.info("收藏Kafka消息发送成功，videoId:{}, userId:{}, 耗时:{}ms", videoId, userId, System.currentTimeMillis() - startTime);
                } catch (Exception e) {
                    log.error("Kafka消息发送失败，videoId:{}, userId:{}, 耗时:{}ms", videoId, userId, System.currentTimeMillis() - startTime, e);
                    // 继续执行，不影响主流程
                }
            }).start();
        } catch (Exception e) {
            log.error("收藏消息序列化失败，videoId:{}, userId:{}", videoId, userId, e);
        }

        log.debug("视频收藏成功，videoId:{}, userId:{}", videoId, userId);
    }

    @Override
    @SentinelResource(value = "uncollectVideo", blockHandler = "handleBlockException")
    public void uncollectVideo(Long videoId, Long userId) {
        // 1. 布隆过滤器：过滤不存在的视频ID
        if (!videoIdBloomFilter.mightContain(videoId)) {
            throw new RuntimeException("视频不存在");
        }

        // 2. 检查用户是否已收藏
        String userCollectKey = VideoRedisKey.VIDEO_USER_COLLECT_PREFIX + userId + ":" + videoId;
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(userCollectKey))) {
            throw new RuntimeException("还没有收藏过");
        }

        // 3. 原子操作：减少收藏数
        String collectCountKey = VideoRedisKey.VIDEO_COLLECT_COUNT_PREFIX + videoId;
        stringRedisTemplate.opsForValue().decrement(collectCountKey);

        // 4. 删除用户收藏状态
        stringRedisTemplate.delete(userCollectKey);

        // 5. 发送Kafka消息，异步同步到MySQL
        try {
            VideoInteractionDTO interactionDTO = new VideoInteractionDTO(videoId, userId, "uncollect", System.currentTimeMillis());
            String message = objectMapper.writeValueAsString(interactionDTO);
            // 异步发送Kafka消息，不阻塞主请求
            new Thread(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    kafkaTemplate.send(
                            VideoRedisKey.VIDEO_INTERACTION_SYNC_TOPIC,
                            message
                    );
                    log.info("取消收藏Kafka消息发送成功，videoId:{}, userId:{}, 耗时:{}ms", videoId, userId, System.currentTimeMillis() - startTime);
                } catch (Exception e) {
                    log.error("Kafka消息发送失败，videoId:{}, userId:{}, 耗时:{}ms", videoId, userId, System.currentTimeMillis() - startTime, e);
                    // 继续执行，不影响主流程
                }
            }).start();
        } catch (Exception e) {
            log.error("取消收藏消息序列化失败，videoId:{}, userId:{}", videoId, userId, e);
        }

        log.debug("取消视频收藏成功，videoId:{}, userId:{}", videoId, userId);
    }

    @Override
    @SentinelResource(value = "shareVideo", blockHandler = "handleBlockException")
    public void shareVideo(Long videoId, Long userId) {
        // 1. 布隆过滤器：过滤不存在的视频ID
        if (!videoIdBloomFilter.mightContain(videoId)) {
            throw new RuntimeException("视频不存在");
        }

        // 2. 原子操作：增加分享数
        String shareCountKey = VideoRedisKey.VIDEO_SHARE_COUNT_PREFIX + videoId;
        Long increment = stringRedisTemplate.opsForValue().increment(shareCountKey);
        if (increment != null && increment == 1) {
            // 首次分享，初始化过期时间
            stringRedisTemplate.expire(shareCountKey, VideoRedisKey.INTERACTION_EXPIRE_DAY, TimeUnit.DAYS);
        }

        // 3. 发送Kafka消息，异步同步到MySQL
        try {
            VideoInteractionDTO interactionDTO = new VideoInteractionDTO(videoId, userId, "share", System.currentTimeMillis());
            String message = objectMapper.writeValueAsString(interactionDTO);
            // 异步发送Kafka消息，不阻塞主请求
            new Thread(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    kafkaTemplate.send(
                            VideoRedisKey.VIDEO_INTERACTION_SYNC_TOPIC,
                            message
                    );
                    log.info("分享Kafka消息发送成功，videoId:{}, userId:{}, 耗时:{}ms", videoId, userId, System.currentTimeMillis() - startTime);
                } catch (Exception e) {
                    log.error("Kafka消息发送失败，videoId:{}, userId:{}, 耗时:{}ms", videoId, userId, System.currentTimeMillis() - startTime, e);
                    // 继续执行，不影响主流程
                }
            }).start();
        } catch (Exception e) {
            log.error("分享消息序列化失败，videoId:{}, userId:{}", videoId, userId, e);
        }

        log.debug("视频分享成功，videoId:{}, userId:{}", videoId, userId);
    }

    /**
     * Sentinel熔断降级处理方法
     */
    public void handleBlockException(Long videoId, BlockException ex) {
        log.warn("Sentinel熔断触发，videoId:{}, 原因:{}", videoId, ex.getMessage());
        throw new RuntimeException("系统繁忙，请稍后再试");
    }

    public void handleBlockException(Long videoId, Long userId, BlockException ex) {
        log.warn("Sentinel熔断触发，videoId:{}, userId:{}, 原因:{}", videoId, userId, ex.getMessage());
        throw new RuntimeException("系统繁忙，请稍后再试");
    }

    public void handleBlockException(Long videoId, Long userId, Integer coinCount, BlockException ex) {
        log.warn("Sentinel熔断触发，videoId:{}, userId:{}, coinCount:{}, 原因:{}", videoId, userId, coinCount, ex.getMessage());
        throw new RuntimeException("系统繁忙，请稍后再试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSyncViewCountToMysql() {
        // 1. 使用scan命令替代keys，避免阻塞Redis
        Set<String> keys = new HashSet<>();
        try {
            stringRedisTemplate.execute((RedisCallback<Void>) redisConnection -> {
                Cursor<byte[]> cursor = redisConnection.scan(
                    ScanOptions.scanOptions().match((VideoRedisKey.VIDEO_VIEW_COUNT_PREFIX + "*").getBytes()).count(100).build()
                );
                try {
                    while (cursor.hasNext()) {
                        byte[] keyBytes = cursor.next();
                        String key = new String(keyBytes);
                        keys.add(key);
                    }
                } finally {
                    if (cursor != null) {
                        try {
                            cursor.close();
                        } catch (Exception e) {
                            log.error("关闭Redis扫描游标失败", e);
                        }
                    }
                }
                return null;
            });
        } catch (Exception e) {
            log.error("执行Redis scan操作失败", e);
        }
        
        if (keys == null || keys.isEmpty()) {
            log.info("暂无需要同步的播放量数据");
            return;
        }

        // 2. 批量获取播放量值（multiGet返回的List与入参keys顺序严格一一对应）
        List<String> keyList = new ArrayList<>(keys);
        List<String> viewCountValueList = stringRedisTemplate.opsForValue().multiGet(keyList);
        if (viewCountValueList == null || viewCountValueList.isEmpty()) {
            log.warn("批量获取播放量值为空，key数量:{}", keyList.size());
            return;
        }

        // 3. 组装key-播放量映射，过滤无效值，避免空指针
        Map<Long, Long> videoViewCountMap = new HashMap<>(keyList.size());
        for (int i = 0; i < keyList.size(); i++) {
            String key = keyList.get(i);
            String valueStr = viewCountValueList.get(i);

            // 过滤空值、无效值
            if (valueStr == null || valueStr.isBlank()) {
                continue;
            }

            // 提取视频ID，转换数值，捕获格式异常
            try {
                Long videoId = Long.parseLong(key.replace(VideoRedisKey.VIDEO_VIEW_COUNT_PREFIX, ""));
                Long viewCount = Long.parseLong(valueStr);
                videoViewCountMap.put(videoId, viewCount);
            } catch (NumberFormatException e) {
                log.error("播放量数据格式异常，key:{}, value:{}", key, valueStr, e);
            }
        }

        if (videoViewCountMap.isEmpty()) {
            log.info("无有效播放量数据需要同步");
            return;
        }

        // 4. 批量更新到MySQL + 同步更新缓存元信息
        for (Map.Entry<Long, Long> entry : videoViewCountMap.entrySet()) {
            Long videoId = entry.getKey();
            Long viewCount = entry.getValue();

            // 更新MySQL播放量
            Video updateVideo = new Video();
            updateVideo.setVideoId(videoId);
            updateVideo.setViewCount(viewCount);
            videoMapper.updateById(updateVideo);

            // 同步更新视频元信息缓存，保证缓存与最终数据一致
            String videoInfoKey = VideoRedisKey.VIDEO_INFO_PREFIX + videoId;
            String videoInfoJson = stringRedisTemplate.opsForValue().get(videoInfoKey);
            if (videoInfoJson != null && !videoInfoJson.isBlank()) {
                try {
                    Video cacheVideo = objectMapper.readValue(videoInfoJson, Video.class);
                    cacheVideo.setViewCount(viewCount);
                    stringRedisTemplate.opsForValue().set(
                            videoInfoKey,
                            objectMapper.writeValueAsString(cacheVideo),
                            VideoRedisKey.VIDEO_INFO_EXPIRE_HOUR,
                            TimeUnit.HOURS
                    );
                } catch (JsonProcessingException e) {
                    log.error("更新缓存视频播放量失败，videoId:{}", videoId, e);
                }
            }

            log.info("视频播放量同步完成，videoId:{}, 同步播放量:{}", videoId, viewCount);
        }
    }

    /**
     * 缓存预热：提前加载热点视频数据
     */
    public void warmUpCache() {
        log.info("开始缓存预热...");
        
        // 查询播放量前100的热点视频
        LambdaQueryWrapper<Video> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Video::getAuditStatus, 1)
                .eq(Video::getStatus, 1)
                .orderByDesc(Video::getViewCount)
                .last("LIMIT 100");
        
        List<Video> hotVideos = videoMapper.selectList(queryWrapper);
        log.info("预热视频数量: {}", hotVideos.size());
        
        // 预热视频信息缓存
        for (Video video : hotVideos) {
            try {
                String videoInfoKey = VideoRedisKey.VIDEO_INFO_PREFIX + video.getVideoId();
                stringRedisTemplate.opsForValue().set(
                        videoInfoKey,
                        objectMapper.writeValueAsString(video),
                        VideoRedisKey.VIDEO_INFO_EXPIRE_HOUR,
                        TimeUnit.HOURS
                );
                
                // 预热视频选集缓存
                String episodesCacheKey = "video:episodes:" + video.getVideoId();
                List<VideoEpisode> episodes = videoEpisodeMapper.selectByVideoIdAndStatus(video.getVideoId(), 1);
                if (!episodes.isEmpty()) {
                    List<VideoEpisodeVO> episodeVOs = new ArrayList<>();
                    for (VideoEpisode episode : episodes) {
                        VideoEpisodeVO vo = new VideoEpisodeVO();
                        BeanUtils.copyProperties(episode, vo);
                        vo.setDurationFormat(formatDuration(episode.getDuration()));
                        episodeVOs.add(vo);
                    }
                    stringRedisTemplate.opsForValue().set(
                            episodesCacheKey,
                            objectMapper.writeValueAsString(episodeVOs),
                            24,
                            TimeUnit.HOURS
                    );
                }
                
                // 预热相关视频推荐缓存
                String relatedCacheKey = "video:related:" + video.getVideoId() + ":10";
                List<VideoPlayDetailVO> relatedVideos = getRelatedVideos(video.getVideoId(), 10);
                if (!relatedVideos.isEmpty()) {
                    stringRedisTemplate.opsForValue().set(
                            relatedCacheKey,
                            objectMapper.writeValueAsString(relatedVideos),
                            1,
                            TimeUnit.HOURS
                    );
                }
                
            } catch (Exception e) {
                log.error("预热视频缓存失败，videoId:{}", video.getVideoId(), e);
            }
        }
        
        log.info("缓存预热完成");
    }

    /**
     * 从MySQL查询视频信息
     */
    private Video getVideoFromMysql(Long videoId) {
        long startTime = System.currentTimeMillis();
        log.info("开始getVideoFromMysql，videoId:{}, 开始时间:{}", videoId, startTime);
        
        Video video = videoMapper.selectOne(
                new LambdaQueryWrapper<Video>()
                        .eq(Video::getVideoId, videoId)
        );
        
        log.info("getVideoFromMysql查询完成，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - startTime);
        
        if (video == null) {
            log.info("getVideoFromMysql查询结果为空，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - startTime);
            throw new RuntimeException("视频不存在");
        }
        
        log.info("getVideoFromMysql处理完成，videoId:{}, 总耗时:{}ms", videoId, System.currentTimeMillis() - startTime);
        return video;
    }

    /**
     * 加分布式锁查询视频（防缓存击穿）
     */
    private Video getVideoWithLock(Long videoId) {
        long startTime = System.currentTimeMillis();
        log.info("开始getVideoWithLock，videoId:{}, 开始时间:{}", videoId, startTime);
        
        String lockKey = LOCK_KEY_PREFIX + videoId;
        String lockValue = UUID.randomUUID().toString();
        Video video = null;
        int retryCount = 0; // 重试次数限制
        final int MAX_RETRY = 2; // 减少重试次数，避免长时间等待
        final long LOCK_EXPIRE_TIME = 15; // 锁过期时间15秒，减少锁持有时间
        final long RETRY_WAIT_TIME = 30; // 重试等待时间30ms，减少等待时间

        while (retryCount < MAX_RETRY) {
            try {
                // 1. 获取分布式锁
                long lockStart = System.currentTimeMillis();
                boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(
                        lockKey, lockValue, LOCK_EXPIRE_TIME, TimeUnit.SECONDS
                );
                log.info("尝试获取分布式锁，videoId:{}, 耗时:{}ms, 结果:{}", videoId, System.currentTimeMillis() - lockStart, locked);

                if (locked) {
                    try {
                        // 2. 锁内查库
                        long dbStart = System.currentTimeMillis();
                        video = getVideoFromMysql(videoId);
                        log.info("锁内查库完成，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - dbStart);

                        // 3. 写入缓存
                        long cacheStart = System.currentTimeMillis();
                        String videoInfoKey = VideoRedisKey.VIDEO_INFO_PREFIX + videoId;
                        if (video != null) {
                            // 热点视频（播放量>10000）永不过期
                            if (video.getViewCount() > 10000) {
                                stringRedisTemplate.opsForValue().set(videoInfoKey, objectMapper.writeValueAsString(video));
                                log.info("热点视频缓存写入完成，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - cacheStart);
                            } else {
                                // 非热点：过期时间随机化（24±2小时），进一步避免缓存雪崩
                                long expireHour = VideoRedisKey.VIDEO_INFO_EXPIRE_HOUR + (long) (Math.random() * 4 - 2);
                                stringRedisTemplate.opsForValue().set(
                                        videoInfoKey,
                                        objectMapper.writeValueAsString(video),
                                        expireHour,
                                        TimeUnit.HOURS
                                );
                                log.info("非热点视频缓存写入完成，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - cacheStart);
                            }
                        } else {
                            // 缓存空值（防穿透）
                            stringRedisTemplate.opsForValue().set(
                                    videoInfoKey, "null", NULL_VALUE_EXPIRE_MINUTE, TimeUnit.MINUTES
                            );
                            log.info("空值缓存写入完成，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - cacheStart);
                            throw new RuntimeException("视频不存在");
                        }
                        break; // 获取锁并查库成功，退出循环
                    } finally {
                        // 释放锁（避免误删其他线程的锁）
                        long releaseStart = System.currentTimeMillis();
                        releaseLock(lockKey, lockValue);
                        log.info("释放分布式锁完成，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - releaseStart);
                    }
                } else {
                    // 未获取到锁：等待后重试
                    retryCount++;
                    if (retryCount < MAX_RETRY) {
                        long waitStart = System.currentTimeMillis();
                        TimeUnit.MILLISECONDS.sleep(RETRY_WAIT_TIME);
                        log.info("未获取到锁，等待后重试，videoId:{}, 重试次数:{}, 耗时:{}ms", videoId, retryCount, System.currentTimeMillis() - waitStart);
                    }
                }
            } catch (InterruptedException e) {
                log.error("获取锁时线程中断，videoId:{}", videoId, e);
                Thread.currentThread().interrupt();
                throw new RuntimeException("获取视频信息失败");
            } catch (Exception e) {
                log.error("加锁查库失败，videoId:{}", videoId, e);
                // 释放锁
                releaseLock(lockKey, lockValue);
                throw new RuntimeException("获取视频信息失败");
            }
        }

        if (video == null) {
            // 最后尝试一次无锁查询，避免因锁竞争导致失败
            log.info("锁竞争失败，开始无锁查询，videoId:{}", videoId);
            long finalQueryStart = System.currentTimeMillis();
            video = getVideoFromMysql(videoId);
            log.info("无锁查询完成，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - finalQueryStart);
            if (video != null) {
                // 写入缓存
                long finalCacheStart = System.currentTimeMillis();
                String videoInfoKey = VideoRedisKey.VIDEO_INFO_PREFIX + videoId;
                try {
                    stringRedisTemplate.opsForValue().set(
                            videoInfoKey,
                            objectMapper.writeValueAsString(video),
                            VideoRedisKey.VIDEO_INFO_EXPIRE_HOUR,
                            TimeUnit.HOURS
                    );
                    log.info("无锁查询缓存写入完成，videoId:{}, 耗时:{}ms", videoId, System.currentTimeMillis() - finalCacheStart);
                } catch (Exception e) {
                    log.error("缓存视频信息失败，videoId:{}", videoId, e);
                }
            } else {
                throw new RuntimeException("获取视频信息失败");
            }
        }
        
        log.info("getVideoWithLock处理完成，videoId:{}, 总耗时:{}ms", videoId, System.currentTimeMillis() - startTime);
        return video;
    }

    /**
     * 释放分布式锁
     */
    private void releaseLock(String lockKey, String lockValue) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        List<String> keys = Collections.singletonList(lockKey);
        stringRedisTemplate.execute(redisScript, keys, lockValue);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSyncInteractionToMysql() {
        // 1. 定义所有互动类型的前缀和字段名映射
        Map<String, String> interactionMap = new HashMap<>();
        interactionMap.put(VideoRedisKey.VIDEO_LIKE_COUNT_PREFIX, "likeCount");
        interactionMap.put(VideoRedisKey.VIDEO_COIN_COUNT_PREFIX, "coinCount");
        interactionMap.put(VideoRedisKey.VIDEO_COLLECT_COUNT_PREFIX, "collectCount");
        interactionMap.put(VideoRedisKey.VIDEO_SHARE_COUNT_PREFIX, "shareCount");

        // 2. 批量处理每种互动类型
        for (Map.Entry<String, String> entry : interactionMap.entrySet()) {
            String prefix = entry.getKey();
            String fieldName = entry.getValue();
            
            // 2.1 使用scan命令替代keys，避免阻塞Redis
            Set<String> keys = new HashSet<>();
            try {
                stringRedisTemplate.execute((RedisCallback<Void>) redisConnection -> {
                    Cursor<byte[]> cursor = redisConnection.scan(
                        ScanOptions.scanOptions().match((prefix + "*").getBytes()).count(100).build()
                    );
                    try {
                        while (cursor.hasNext()) {
                            byte[] keyBytes = cursor.next();
                            String key = new String(keyBytes);
                            keys.add(key);
                        }
                    } finally {
                        if (cursor != null) {
                            try {
                                cursor.close();
                            } catch (Exception e) {
                                log.error("关闭Redis扫描游标失败", e);
                            }
                        }
                    }
                    return null;
                });
            } catch (Exception e) {
                log.error("执行Redis scan操作失败", e);
            }
            
            // 2.2 批量获取值并同步到MySQL
            if (!keys.isEmpty()) {
                syncInteractionCountBatch(keys, prefix, fieldName);
            }
        }
    }

    /**
     * 批量同步互动数据到MySQL
     */
    private void syncInteractionCountBatch(Set<String> keys, String prefix, String fieldName) {
        // 批量获取Redis中的计数
        List<String> keyList = new ArrayList<>(keys);
        List<String> countValues = stringRedisTemplate.opsForValue().multiGet(keyList);
        
        if (countValues == null || countValues.isEmpty()) {
            return;
        }
        
        // 批量处理更新
        List<Video> updateVideos = new ArrayList<>();
        for (int i = 0; i < keyList.size(); i++) {
            try {
                String key = keyList.get(i);
                String countStr = countValues.get(i);
                
                if (countStr == null) {
                    continue;
                }
                
                // 提取视频ID
                String videoIdStr = key.replace(prefix, "");
                Long videoId = Long.parseLong(videoIdStr);
                Long count = Long.parseLong(countStr);
                
                // 创建更新对象
                Video updateVideo = new Video();
                updateVideo.setVideoId(videoId);
                
                // 根据字段名设置对应的值
                switch (fieldName) {
                    case "likeCount":
                        updateVideo.setLikeCount(count);
                        break;
                    case "coinCount":
                        updateVideo.setCoinCount(count);
                        break;
                    case "collectCount":
                        updateVideo.setCollectCount(count);
                        break;
                    case "shareCount":
                        updateVideo.setShareCount(count);
                        break;
                }
                
                updateVideos.add(updateVideo);
            } catch (Exception e) {
                log.error("处理互动数据失败，key:{}", keyList.get(i), e);
            }
        }
        
        // 批量更新到MySQL
        if (!updateVideos.isEmpty()) {
            for (Video video : updateVideos) {
                videoMapper.updateById(video);
                log.info("同步{}成功，videoId:{}", fieldName, video.getVideoId());
            }
        }
    }

    @Override
    public List<VideoEpisodeVO> getVideoEpisodes(Long videoId) {
        // 1. 布隆过滤器：过滤不存在的视频ID
        if (!videoIdBloomFilter.mightContain(videoId)) {
            throw new RuntimeException("视频不存在");
        }

        // 2. 缓存键
        String cacheKey = "video:episodes:" + videoId;
        
        // 3. 尝试从缓存获取
        try {
            String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cacheValue != null) {
                try {
                    return objectMapper.readValue(cacheValue, new com.fasterxml.jackson.core.type.TypeReference<List<VideoEpisodeVO>>() {});
                } catch (Exception e) {
                    log.error("反序列化选集缓存失败，videoId:{}", videoId, e);
                    // 缓存反序列化失败，继续查询
                }
            }
        } catch (Exception e) {
            log.error("Redis操作失败，videoId:{}", videoId, e);
            // Redis失败时，继续从数据库查询
        }

        // 4. 查询公开的选集列表
        List<VideoEpisode> episodes = videoEpisodeMapper.selectByVideoIdAndStatus(videoId, 1);
        
        // 5. 检查是否存在选集
        if (episodes == null || episodes.isEmpty()) {
            throw new RuntimeException("视频不存在或已下架");
        }

        // 6. 转换为VO并格式化时长
        List<VideoEpisodeVO> episodeVOs = new ArrayList<>();
        for (VideoEpisode episode : episodes) {
            VideoEpisodeVO vo = new VideoEpisodeVO();
            BeanUtils.copyProperties(episode, vo);
            // 格式化时长
            vo.setDurationFormat(formatDuration(episode.getDuration()));
            episodeVOs.add(vo);
        }

        // 7. 缓存选集列表，过期时间24小时
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(episodeVOs), 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("序列化选集缓存失败，videoId:{}", videoId, e);
            // 缓存失败不影响返回结果
        }

        return episodeVOs;
    }

    @Override
    public VideoPlayDetailVO getVideoEpisodeDetail(Long episodeId) {
        // 1. 缓存键
        String cacheKey = "video:episode:detail:" + episodeId;
        
        // 2. 尝试从缓存获取
        try {
            String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cacheValue != null) {
                try {
                    return objectMapper.readValue(cacheValue, VideoPlayDetailVO.class);
                } catch (Exception e) {
                    log.error("反序列化选集详情缓存失败，episodeId:{}", episodeId, e);
                    // 缓存反序列化失败，继续查询
                }
            }
        } catch (Exception e) {
            log.error("Redis操作失败，episodeId:{}", episodeId, e);
            // Redis失败时，继续从数据库查询
        }

        // 3. 查询选集信息
        VideoEpisode episode = videoEpisodeMapper.selectById(episodeId);
        if (episode == null || episode.getAuditStatus() != 1 || episode.getStatus() != 1) {
            throw new RuntimeException("选集不存在或已下架");
        }

        // 4. 获取视频主信息（复用已有的getVideoPlayDetail方法，确保一致性）
        Long videoId = episode.getVideoId();
        VideoPlayDetailVO playDetailVO = getVideoPlayDetail(videoId);

        // 5. 生成预签名播放地址（缓存）
        String playUrlKey = "video:play:url:" + episode.getVideoUrl();
        String playUrl = null;
        try {
            playUrl = stringRedisTemplate.opsForValue().get(playUrlKey);
            if (playUrl == null) {
                // 生成新的预签名URL
                try {
                    playUrl = storageService.generatePresignedPlayUrl(episode.getVideoUrl(), PLAY_URL_EXPIRE_SECONDS);
                    // 缓存预签名URL，过期时间比预签名URL短1小时，确保URL始终有效
                    stringRedisTemplate.opsForValue().set(playUrlKey, playUrl, PLAY_URL_EXPIRE_SECONDS - 3600, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.error("生成预签名URL失败，episodeId:{}, videoUrl:{}", episodeId, episode.getVideoUrl(), e);
                    // MinIO失败时，使用占位符URL
                    playUrl = "#";
                }
            }
        } catch (Exception e) {
            log.error("Redis操作失败，episodeId:{}", episodeId, e);
            // Redis失败时，尝试生成预签名URL
            try {
                playUrl = storageService.generatePresignedPlayUrl(episode.getVideoUrl(), PLAY_URL_EXPIRE_SECONDS);
            } catch (Exception ex) {
                log.error("生成预签名URL失败，episodeId:{}, videoUrl:{}", episodeId, episode.getVideoUrl(), ex);
                // MinIO也失败时，使用占位符URL
                playUrl = "#";
            }
        }

        // 6. 异步检查文件存在（不阻塞主请求）
        final Long finalEpisodeId = episodeId;
        final String finalEpisodeVideoUrl = episode.getVideoUrl();
        new Thread(() -> {
            try {
                boolean fileExists = storageService.checkFileExists(finalEpisodeVideoUrl);
                if (!fileExists) {
                    log.warn("视频源文件不存在，episodeId:{}, videoUrl:{}", finalEpisodeId, finalEpisodeVideoUrl);
                }
            } catch (Exception e) {
                log.error("检查文件存在失败，episodeId:{}, videoUrl:{}", finalEpisodeId, finalEpisodeVideoUrl, e);
            }
        }).start();

        // 7. 设置选集相关信息
        playDetailVO.setPlayUrl(playUrl);
        playDetailVO.setCurrentEpisodeId(episodeId);
        playDetailVO.setCurrentEpisodeTitle(episode.getEpisodeTitle());
        playDetailVO.setCurrentEpisodeOrder(episode.getEpisodeOrder());

        // 8. 缓存选集详情，过期时间12小时
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(playDetailVO), 12, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("序列化选集详情缓存失败，episodeId:{}", episodeId, e);
            // 缓存失败不影响返回结果
        }

        return playDetailVO;
    }

    @Override
    public List<VideoPlayDetailVO> getRelatedVideos(Long videoId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10; // 默认推荐10个视频
        }

        // 1. 布隆过滤器：过滤不存在的视频ID
        if (!videoIdBloomFilter.mightContain(videoId)) {
            return Collections.emptyList();
        }

        // 2. 缓存键
        String cacheKey = "video:related:" + videoId + ":" + limit;
        
        // 3. 尝试从缓存获取
        try {
            String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cacheValue != null) {
                try {
                    return objectMapper.readValue(cacheValue, new com.fasterxml.jackson.core.type.TypeReference<List<VideoPlayDetailVO>>() {});
                } catch (Exception e) {
                    log.error("反序列化相关视频缓存失败，videoId:{}", videoId, e);
                    // 缓存反序列化失败，继续查询
                }
            }
        } catch (Exception e) {
            log.error("Redis操作失败，videoId:{}", videoId, e);
            // Redis失败时，继续从数据库查询
        }

        // 4. 查询视频信息，获取分类ID
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            return Collections.emptyList();
        }

        // 5. 查询同分类的其他视频，按播放量排序
        LambdaQueryWrapper<Video> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Video::getCategoryId, video.getCategoryId())
                .ne(Video::getVideoId, videoId)
                .eq(Video::getAuditStatus, 1)
                .eq(Video::getStatus, 1)
                .orderByDesc(Video::getViewCount)
                .last("LIMIT " + limit);

        List<Video> relatedVideos = videoMapper.selectList(queryWrapper);

        // 6. 转换为VO并批量获取实时播放量
        List<VideoPlayDetailVO> relatedVideoVOs = new ArrayList<>();
        List<String> viewCountKeys = new ArrayList<>();
        Map<Long, Video> videoMap = new HashMap<>();
        
        for (Video relatedVideo : relatedVideos) {
            VideoPlayDetailVO vo = new VideoPlayDetailVO();
            BeanUtils.copyProperties(relatedVideo, vo);
            relatedVideoVOs.add(vo);
            viewCountKeys.add(VideoRedisKey.VIDEO_VIEW_COUNT_PREFIX + relatedVideo.getVideoId());
            videoMap.put(relatedVideo.getVideoId(), relatedVideo);
        }
        
        // 批量获取实时播放量
        if (!viewCountKeys.isEmpty()) {
            try {
                List<String> viewCountValues = stringRedisTemplate.opsForValue().multiGet(viewCountKeys);
                for (int i = 0; i < relatedVideoVOs.size(); i++) {
                    VideoPlayDetailVO vo = relatedVideoVOs.get(i);
                    String viewCountStr = viewCountValues != null ? viewCountValues.get(i) : null;
                    if (viewCountStr != null) {
                        vo.setRealViewCount(Long.parseLong(viewCountStr));
                    } else {
                        vo.setRealViewCount(videoMap.get(vo.getVideoId()).getViewCount());
                    }
                }
            } catch (Exception e) {
                log.error("获取实时播放量失败，videoId:{}", videoId, e);
                // Redis失败时，使用数据库中的数据
            }
        }

        // 7. 缓存推荐结果，过期时间随机化（1-2小时）避免缓存雪崩
        try {
            long expireHour = 1 + (long) (Math.random() * 1);
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(relatedVideoVOs), expireHour, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("序列化相关视频缓存失败，videoId:{}", videoId, e);
            // 缓存失败不影响返回结果
        }

        return relatedVideoVOs;
    }



    /**
     * 格式化时长
     */
    private String formatDuration(Integer seconds) {
        if (seconds == null || seconds < 0) {
            return "0:00";
        }
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%d:%02d", minutes, remainingSeconds);
    }
}
