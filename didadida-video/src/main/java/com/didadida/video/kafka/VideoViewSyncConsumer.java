package com.didadida.video.kafka;

import com.didadida.video.constant.VideoRedisKey;
import com.didadida.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 视频播放量同步消费者
 * 高并发优化：定时批量同步，避免单条频繁写库
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoViewSyncConsumer {

    private final VideoService videoService;

    /**
     * 消费播放量上报消息（仅做日志记录，核心同步用定时任务批量处理）
     */
    @KafkaListener(topics = VideoRedisKey.VIDEO_VIEW_SYNC_TOPIC, groupId = "didadida-video-group")
    public void onMessage(String message) {
        log.debug("收到播放量上报消息：{}", message);
    }

    /**
     * 定时批量同步播放量到MySQL，每5分钟执行一次
     * 可根据业务量调整频率，降低数据库压力
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void batchSyncViewCount() {
        log.info("开始批量同步视频播放量到MySQL");
        try {
            videoService.batchSyncViewCountToMysql();
        } catch (Exception e) {
            log.error("批量同步播放量失败", e);
        }
    }

    /**
     * 定时批量同步互动数据到MySQL，每5分钟执行一次
     * 可根据业务量调整频率，降低数据库压力
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void batchSyncInteraction() {
        log.info("开始批量同步视频互动数据到MySQL");
        try {
            videoService.batchSyncInteractionToMysql();
        } catch (Exception e) {
            log.error("批量同步互动数据失败", e);
        }
    }
}
