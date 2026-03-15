package com.didadida.interaction.kafka;

import com.didadida.interaction.dto.CommentMessageDTO;
import com.didadida.interaction.entity.Comment;
import com.didadida.interaction.entity.LikeRecord;
import com.didadida.interaction.mapper.CommentMapper;
import com.didadida.interaction.mapper.LikeRecordMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 评论同步消费者
 */
@Component
public class CommentSyncConsumer {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private LikeRecordMapper likeRecordMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 消费评论同步消息
     * @param message 消息内容
     */
    @KafkaListener(topics = "comment_sync_topic", groupId = "didadida-interaction-group")
    public void consumeCommentSyncMessage(String message) {
        try {
            // 解析消息
            CommentMessageDTO messageDTO = objectMapper.readValue(message, CommentMessageDTO.class);

            // 根据操作类型处理
            switch (messageDTO.getOperationType()) {
                case "add":
                    handleAddComment(messageDTO);
                    break;
                case "like":
                    handleLikeComment(messageDTO);
                    break;
                case "unlike":
                    handleUnlikeComment(messageDTO);
                    break;
                case "delete":
                    handleDeleteComment(messageDTO);
                    break;
                default:
                    System.out.println("未知操作类型: " + messageDTO.getOperationType());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 处理添加评论操作
     */
    private void handleAddComment(CommentMessageDTO messageDTO) {
        // 创建评论实体
        Comment comment = new Comment();
        comment.setCommentId(messageDTO.getCommentId());
        comment.setVideoId(messageDTO.getVideoId());
        comment.setUserId(messageDTO.getUserId());
        comment.setParentId(messageDTO.getParentId());
        comment.setReplyUserId(messageDTO.getReplyUserId());
        comment.setContent(messageDTO.getContent());
        comment.setLikeCount(0L);
        comment.setReplyCount(0);
        comment.setStatus(1);

        // 保存评论
        commentMapper.insert(comment);

        // 如果是回复评论，更新父评论的回复数
        if (messageDTO.getParentId() != null && messageDTO.getParentId() > 0) {
            commentMapper.incrementReplyCount(messageDTO.getParentId());
        }
    }

    /**
     * 处理点赞评论操作
     */
    private void handleLikeComment(CommentMessageDTO messageDTO) {
        // 检查是否已经点赞
        LikeRecord existingLike = likeRecordMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LikeRecord>()
                        .eq(LikeRecord::getUserId, messageDTO.getUserId())
                        .eq(LikeRecord::getTargetType, 2)
                        .eq(LikeRecord::getTargetId, messageDTO.getCommentId())
        );

        if (existingLike == null) {
            // 添加点赞记录
            LikeRecord likeRecord = new LikeRecord();
            likeRecord.setUserId(messageDTO.getUserId());
            likeRecord.setTargetType(2);
            likeRecord.setTargetId(messageDTO.getCommentId());
            likeRecordMapper.insert(likeRecord);

            // 更新评论的点赞数
            commentMapper.incrementLikeCount(messageDTO.getCommentId());
        }
    }

    /**
     * 处理取消点赞评论操作
     */
    private void handleUnlikeComment(CommentMessageDTO messageDTO) {
        // 查找点赞记录
        LikeRecord existingLike = likeRecordMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LikeRecord>()
                        .eq(LikeRecord::getUserId, messageDTO.getUserId())
                        .eq(LikeRecord::getTargetType, 2)
                        .eq(LikeRecord::getTargetId, messageDTO.getCommentId())
        );

        if (existingLike != null) {
            // 删除点赞记录
            likeRecordMapper.deleteById(existingLike.getId());

            // 更新评论的点赞数
            commentMapper.decrementLikeCount(messageDTO.getCommentId());
        }
    }

    /**
     * 处理删除评论操作
     */
    private void handleDeleteComment(CommentMessageDTO messageDTO) {
        // 查找评论
        Comment comment = commentMapper.selectById(messageDTO.getCommentId());

        if (comment != null && comment.getUserId().equals(messageDTO.getUserId())) {
            // 更新评论状态为删除
            comment.setStatus(0);
            commentMapper.updateById(comment);
        }
    }

}
