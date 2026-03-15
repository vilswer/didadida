package com.didadida.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.didadida.interaction.dto.CommentDTO;
import com.didadida.interaction.entity.Comment;
import com.didadida.interaction.entity.LikeRecord;
import com.didadida.interaction.mapper.CommentMapper;
import com.didadida.interaction.mapper.LikeRecordMapper;
import com.didadida.interaction.service.CommentService;
import com.didadida.interaction.vo.CommentVO;
import com.didadida.interaction.constant.CommentRedisKey;
import com.didadida.interaction.dto.CommentMessageDTO;
import com.didadida.interaction.feign.UserFeignClient;
import com.didadida.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评论服务实现类
 */
@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private LikeRecordMapper likeRecordMapper;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    @Qualifier("interactionRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Long addComment(CommentDTO commentDTO, Long userId) {
        try {
            // 生成评论ID
            Long commentId = redisTemplate.opsForValue().increment("comment:id:seq");

            // 创建评论实体
            Comment comment = new Comment();
            comment.setCommentId(commentId);
            comment.setVideoId(commentDTO.getVideoId());
            comment.setUserId(userId);
            comment.setParentId(commentDTO.getParentId());
            comment.setReplyUserId(commentDTO.getReplyUserId());
            comment.setContent(commentDTO.getContent());
            comment.setLikeCount(0L);
            comment.setReplyCount(0);
            comment.setStatus(1);

            // 将评论信息存储到Redis
            String commentInfoKey = String.format(CommentRedisKey.COMMENT_INFO, commentId);
            redisTemplate.opsForValue().set(commentInfoKey, comment);

            // 如果是回复评论，更新父评论的回复数
            if (commentDTO.getParentId() != null && commentDTO.getParentId() > 0) {
                String replyCountKey = String.format(CommentRedisKey.COMMENT_REPLY_COUNT, commentDTO.getParentId());
                redisTemplate.opsForValue().increment(replyCountKey);
            }

            // 清除相关缓存
            String videoCommentListKey = String.format(CommentRedisKey.VIDEO_COMMENT_LIST, commentDTO.getVideoId());
            redisTemplate.delete(videoCommentListKey);

            // 发送Kafka消息，异步处理持久化到MySQL
            CommentMessageDTO messageDTO = new CommentMessageDTO();
            messageDTO.setOperationType("add");
            messageDTO.setCommentId(commentId);
            messageDTO.setVideoId(commentDTO.getVideoId());
            messageDTO.setUserId(userId);
            messageDTO.setParentId(commentDTO.getParentId());
            messageDTO.setReplyUserId(commentDTO.getReplyUserId());
            messageDTO.setContent(commentDTO.getContent());
            messageDTO.setTimestamp(System.currentTimeMillis());

            // 异步发送Kafka消息
            new Thread(() -> {
                try {
                    String message = objectMapper.writeValueAsString(messageDTO);
                    kafkaTemplate.send("comment_sync_topic", message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            return commentId;
        } catch (Exception e) {
            e.printStackTrace();
            // 降级处理：直接写入MySQL
            Comment comment = new Comment();
            comment.setVideoId(commentDTO.getVideoId());
            comment.setUserId(userId);
            comment.setParentId(commentDTO.getParentId());
            comment.setReplyUserId(commentDTO.getReplyUserId());
            comment.setContent(commentDTO.getContent());
            comment.setLikeCount(0L);
            comment.setReplyCount(0);
            comment.setStatus(1);
            commentMapper.insert(comment);
            if (commentDTO.getParentId() != null && commentDTO.getParentId() > 0) {
                commentMapper.incrementReplyCount(commentDTO.getParentId());
            }
            return comment.getCommentId();
        }
    }

    @Override
    public List<CommentVO> getVideoComments(Long videoId, Integer page, Integer size, Long currentUserId) {
        try {
            // 尝试从Redis缓存中获取评论列表
            String videoCommentListKey = String.format(CommentRedisKey.VIDEO_COMMENT_LIST, videoId);
            List<Comment> comments = (List<Comment>) redisTemplate.opsForValue().get(videoCommentListKey);

            if (comments == null) {
                // 缓存不存在，从数据库读取
                long offset = (page - 1L) * size;
                comments = commentMapper.selectFirstLevelComments(videoId, offset, size);

                // 将评论列表缓存到Redis，设置过期时间为1小时
                redisTemplate.opsForValue().set(videoCommentListKey, comments, 3600, java.util.concurrent.TimeUnit.SECONDS);
            }

            // 转换为VO
            return convertToCommentVOList(comments, currentUserId);
        } catch (Exception e) {
            e.printStackTrace();
            // 降级处理：直接从数据库读取
            long offset = (page - 1L) * size;
            List<Comment> comments = commentMapper.selectFirstLevelComments(videoId, offset, size);
            return convertToCommentVOList(comments, currentUserId);
        }
    }

    @Override
    public List<CommentVO> getCommentReplies(Long commentId, Integer page, Integer size, Long currentUserId) {
        try {
            // 尝试从Redis缓存中获取回复列表
            String commentRepliesListKey = String.format(CommentRedisKey.COMMENT_REPLIES_LIST, commentId);
            List<Comment> comments = (List<Comment>) redisTemplate.opsForValue().get(commentRepliesListKey);

            if (comments == null) {
                // 缓存不存在，从数据库读取
                long offset = (page - 1L) * size;
                comments = commentMapper.selectRepliesByParentId(commentId, offset, size);

                // 将回复列表缓存到Redis，设置过期时间为1小时
                redisTemplate.opsForValue().set(commentRepliesListKey, comments, 3600, java.util.concurrent.TimeUnit.SECONDS);
            }

            // 转换为VO
            return convertToCommentVOList(comments, currentUserId);
        } catch (Exception e) {
            e.printStackTrace();
            // 降级处理：直接从数据库读取
            long offset = (page - 1L) * size;
            List<Comment> comments = commentMapper.selectRepliesByParentId(commentId, offset, size);
            return convertToCommentVOList(comments, currentUserId);
        }
    }

    @Override
    public boolean likeComment(Long commentId, Long userId) {
        try {
            // 检查是否已经点赞
            String likeStatusKey = String.format(CommentRedisKey.COMMENT_LIKE_STATUS, userId, commentId);
            Boolean isLiked = redisTemplate.hasKey(likeStatusKey);

            if (Boolean.TRUE.equals(isLiked)) {
                return false; // 已经点赞过
            }

            // 设置点赞状态
            redisTemplate.opsForValue().set(likeStatusKey, true, 7, java.util.concurrent.TimeUnit.DAYS);

            // 增加评论点赞数
            String likeCountKey = String.format(CommentRedisKey.COMMENT_LIKE_COUNT, commentId);
            redisTemplate.opsForValue().increment(likeCountKey);

            // 发送Kafka消息，异步处理持久化到MySQL
            CommentMessageDTO messageDTO = new CommentMessageDTO();
            messageDTO.setOperationType("like");
            messageDTO.setCommentId(commentId);
            messageDTO.setUserId(userId);
            messageDTO.setTimestamp(System.currentTimeMillis());

            // 异步发送Kafka消息
            new Thread(() -> {
                try {
                    String message = objectMapper.writeValueAsString(messageDTO);
                    kafkaTemplate.send("comment_sync_topic", message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            // 降级处理：直接写入MySQL
            LambdaQueryWrapper<LikeRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(LikeRecord::getUserId, userId)
                    .eq(LikeRecord::getTargetType, 2) // 2表示评论
                    .eq(LikeRecord::getTargetId, commentId);
            LikeRecord existingLike = likeRecordMapper.selectOne(wrapper);

            if (existingLike != null) {
                return false; // 已经点赞过
            }

            // 添加点赞记录
            LikeRecord likeRecord = new LikeRecord();
            likeRecord.setUserId(userId);
            likeRecord.setTargetType(2);
            likeRecord.setTargetId(commentId);
            likeRecordMapper.insert(likeRecord);

            // 更新评论的点赞数
            commentMapper.incrementLikeCount(commentId);

            return true;
        }
    }

    @Override
    public boolean unlikeComment(Long commentId, Long userId) {
        try {
            // 检查是否已经点赞
            String likeStatusKey = String.format(CommentRedisKey.COMMENT_LIKE_STATUS, userId, commentId);
            Boolean isLiked = redisTemplate.hasKey(likeStatusKey);

            if (Boolean.FALSE.equals(isLiked)) {
                return false; // 没有点赞过
            }

            // 删除点赞状态
            redisTemplate.delete(likeStatusKey);

            // 减少评论点赞数
            String likeCountKey = String.format(CommentRedisKey.COMMENT_LIKE_COUNT, commentId);
            redisTemplate.opsForValue().decrement(likeCountKey);

            // 发送Kafka消息，异步处理持久化到MySQL
            CommentMessageDTO messageDTO = new CommentMessageDTO();
            messageDTO.setOperationType("unlike");
            messageDTO.setCommentId(commentId);
            messageDTO.setUserId(userId);
            messageDTO.setTimestamp(System.currentTimeMillis());

            // 异步发送Kafka消息
            new Thread(() -> {
                try {
                    String message = objectMapper.writeValueAsString(messageDTO);
                    kafkaTemplate.send("comment_sync_topic", message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            // 降级处理：直接写入MySQL
            LambdaQueryWrapper<LikeRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(LikeRecord::getUserId, userId)
                    .eq(LikeRecord::getTargetType, 2) // 2表示评论
                    .eq(LikeRecord::getTargetId, commentId);
            LikeRecord existingLike = likeRecordMapper.selectOne(wrapper);

            if (existingLike == null) {
                return false; // 没有点赞过
            }

            // 删除点赞记录
            likeRecordMapper.deleteById(existingLike.getId());

            // 更新评论的点赞数
            commentMapper.decrementLikeCount(commentId);

            return true;
        }
    }

    @Override
    public boolean deleteComment(Long commentId, Long userId) {
        try {
            // 从Redis中获取评论信息
            String commentInfoKey = String.format(CommentRedisKey.COMMENT_INFO, commentId);
            Comment comment = (Comment) redisTemplate.opsForValue().get(commentInfoKey);

            // 如果Redis中没有，从数据库中获取
            if (comment == null) {
                comment = commentMapper.selectById(commentId);
            }

            // 检查是否是评论的作者
            if (comment == null || !comment.getUserId().equals(userId)) {
                return false;
            }

            // 更新评论状态为删除
            comment.setStatus(0);
            redisTemplate.opsForValue().set(commentInfoKey, comment);

            // 清除相关缓存
            if (comment.getParentId() != null && comment.getParentId() > 0) {
                String replyCountKey = String.format(CommentRedisKey.COMMENT_REPLY_COUNT, comment.getParentId());
                redisTemplate.opsForValue().decrement(replyCountKey);
            }
            String videoCommentListKey = String.format(CommentRedisKey.VIDEO_COMMENT_LIST, comment.getVideoId());
            redisTemplate.delete(videoCommentListKey);
            if (comment.getParentId() != null && comment.getParentId() > 0) {
                String commentRepliesListKey = String.format(CommentRedisKey.COMMENT_REPLIES_LIST, comment.getParentId());
                redisTemplate.delete(commentRepliesListKey);
            }

            // 发送Kafka消息，异步处理持久化到MySQL
            CommentMessageDTO messageDTO = new CommentMessageDTO();
            messageDTO.setOperationType("delete");
            messageDTO.setCommentId(commentId);
            messageDTO.setVideoId(comment.getVideoId());
            messageDTO.setUserId(userId);
            messageDTO.setTimestamp(System.currentTimeMillis());

            // 异步发送Kafka消息
            new Thread(() -> {
                try {
                    String message = objectMapper.writeValueAsString(messageDTO);
                    kafkaTemplate.send("comment_sync_topic", message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            // 降级处理：直接写入MySQL
            Comment comment = commentMapper.selectById(commentId);
            if (comment == null || !comment.getUserId().equals(userId)) {
                return false;
            }

            // 更新评论状态为删除
            comment.setStatus(0);
            commentMapper.updateById(comment);

            return true;
        }
    }

    /**
     * 将评论列表转换为VO列表
     */
    private List<CommentVO> convertToCommentVOList(List<Comment> comments, Long currentUserId) {
        if (comments.isEmpty()) {
            return new ArrayList<>();
        }

        // 收集所有用户ID
        List<Long> userIds = comments.stream()
                .map(Comment::getUserId)
                .collect(Collectors.toList());

        // 收集回复的用户ID
        List<Long> replyUserIds = comments.stream()
                .filter(comment -> comment.getReplyUserId() != null && comment.getReplyUserId() > 0)
                .map(Comment::getReplyUserId)
                .collect(Collectors.toList());
        userIds.addAll(replyUserIds);

        // 批量获取用户信息
        Map<Long, User> userMap = new java.util.HashMap<>();
        for (Long userId : userIds.stream().distinct().collect(Collectors.toList())) {
            try {
                User user = userFeignClient.getUserById(userId);
                if (user != null) {
                    userMap.put(userId, user);
                }
            } catch (Exception e) {
                // 降级处理：当用户服务不可用时，使用默认用户信息
                User defaultUser = new User();
                defaultUser.setUserId(userId);
                defaultUser.setNickname("用户" + userId);
                defaultUser.setAvatar("");
                userMap.put(userId, defaultUser);
            }
        }

        // 构建点赞状态映射
        Map<Long, Boolean> likeStatusMap = new java.util.HashMap<>();
        // 构建点赞数映射
        Map<Long, Long> likeCountMap = new java.util.HashMap<>();

        if (currentUserId != null) {
            // 从Redis中获取用户的点赞状态
            for (Comment comment : comments) {
                String likeStatusKey = String.format(CommentRedisKey.COMMENT_LIKE_STATUS, currentUserId, comment.getCommentId());
                Boolean isLiked = redisTemplate.hasKey(likeStatusKey);
                likeStatusMap.put(comment.getCommentId(), Boolean.TRUE.equals(isLiked));

                // 从Redis中获取评论的点赞数
                String likeCountKey = String.format(CommentRedisKey.COMMENT_LIKE_COUNT, comment.getCommentId());
                Object likeCountObj = redisTemplate.opsForValue().get(likeCountKey);
                if (likeCountObj != null) {
                    likeCountMap.put(comment.getCommentId(), (Long) likeCountObj);
                } else {
                    likeCountMap.put(comment.getCommentId(), comment.getLikeCount());
                }
            }
        } else {
            // 只获取点赞数
            for (Comment comment : comments) {
                String likeCountKey = String.format(CommentRedisKey.COMMENT_LIKE_COUNT, comment.getCommentId());
                Object likeCountObj = redisTemplate.opsForValue().get(likeCountKey);
                if (likeCountObj != null) {
                    likeCountMap.put(comment.getCommentId(), (Long) likeCountObj);
                } else {
                    likeCountMap.put(comment.getCommentId(), comment.getLikeCount());
                }
            }
        }

        // 转换为VO
        return comments.stream().map(comment -> {
            CommentVO vo = new CommentVO();
            BeanUtils.copyProperties(comment, vo);

            // 设置评论者信息
            User user = userMap.get(comment.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            }

            // 设置回复的用户信息
            if (comment.getReplyUserId() != null && comment.getReplyUserId() > 0) {
                User replyUser = userMap.get(comment.getReplyUserId());
                if (replyUser != null) {
                    vo.setReplyUserNickname(replyUser.getNickname());
                }
            }

            // 设置点赞状态
            vo.setIsLiked(likeStatusMap.getOrDefault(comment.getCommentId(), false));

            // 设置点赞数
            vo.setLikeCount(likeCountMap.getOrDefault(comment.getCommentId(), comment.getLikeCount()));

            return vo;
        }).collect(Collectors.toList());
    }
}
