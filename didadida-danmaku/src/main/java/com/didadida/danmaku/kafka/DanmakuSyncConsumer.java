package com.didadida.danmaku.kafka;

import com.alibaba.fastjson.JSON;
import com.didadida.danmaku.dto.DanmakuMessageDTO;
import com.didadida.danmaku.service.DanmakuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 弹幕同步消费者
 */
@Component
@Slf4j
public class DanmakuSyncConsumer {

    @Autowired
    private DanmakuService danmakuService;

    /**
     * 消费弹幕消息并同步到数据库
     * @param message 消息内容
     */
    @KafkaListener(topics = "danmaku_sync_topic", groupId = "didadida-danmaku-group")
    public void consumeDanmakuMessage(String message) {
        try {
            log.info("收到弹幕同步消息: {}", message);
            DanmakuMessageDTO danmakuMessageDTO = JSON.parseObject(message, DanmakuMessageDTO.class);
            danmakuService.syncDanmakuToMysql(danmakuMessageDTO);
        } catch (Exception e) {
            log.error("处理弹幕同步消息失败", e);
        }
    }
}
