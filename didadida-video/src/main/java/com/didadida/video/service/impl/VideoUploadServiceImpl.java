package com.didadida.video.service.impl;

import com.didadida.video.dto.VideoChunkMergeDTO;
import com.didadida.video.dto.VideoChunkUploadDTO;
import com.didadida.video.entity.Video;
import com.didadida.video.mapper.VideoMapper;
import com.didadida.video.service.VideoUploadService;
import com.didadida.video.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoUploadServiceImpl implements VideoUploadService {
    private final StorageService storageService;
    private final StringRedisTemplate stringRedisTemplate;
    private final VideoMapper videoMapper;

    // Redis Key前缀：分片上传状态
    private static final String CHUNK_UPLOAD_STATUS_KEY = "video:chunk:status:";

    @Override
    public boolean uploadChunk(VideoChunkUploadDTO dto) {
        // 1. 构造分片存储路径
        String chunkFileName = "chunk/" + dto.getFileMd5() + "/" + dto.getChunkIndex();

        // 2. 上传分片到MinIO
        try (InputStream inputStream = dto.getChunkFile().getInputStream()) {
            storageService.uploadChunk(chunkFileName, inputStream, dto.getChunkFile().getSize());
        } catch (IOException e) {
            log.error("获取分片文件流失败", e);
            return false;
        }

        // 3. 记录分片上传状态到Redis（断点续传）
        String statusKey = CHUNK_UPLOAD_STATUS_KEY + dto.getFileMd5() + ":" + dto.getChunkIndex();
        stringRedisTemplate.opsForValue().set(statusKey, "1", 24, TimeUnit.HOURS);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long mergeChunksAndPublish(VideoChunkMergeDTO dto) {
        // 1. 合并分片
        String videoUrl = storageService.mergeChunks(dto.getFileMd5(), dto.getOriginalFileName());

        // 2. 插入视频数据到MySQL
        Video video = new Video();
        video.setUserId(dto.getUserId());
        video.setCategoryId(dto.getCategoryId());
        video.setTitle(dto.getTitle());
        video.setDescription(dto.getDescription());
        video.setVideoUrl(videoUrl);
        // 初始状态：待审核、公开
        video.setAuditStatus(0);
        video.setStatus(1);
        video.setViewCount(0L);
        video.setDanmakuCount(0L);
        video.setLikeCount(0L);
        video.setCoinCount(0L);
        video.setCollectCount(0L);
        video.setShareCount(0L);
        video.setPublishTime(LocalDateTime.now());
        video.setCreateTime(LocalDateTime.now());
        video.setUpdateTime(LocalDateTime.now());

        // 3. 保存视频信息
        videoMapper.insert(video);

        // 4. 删除Redis分片状态
        stringRedisTemplate.delete(CHUNK_UPLOAD_STATUS_KEY + dto.getFileMd5() + ":*");

        return video.getVideoId();
    }

    @Override
    public boolean checkChunkExists(String fileMd5, Integer chunkIndex) {
        String statusKey = CHUNK_UPLOAD_STATUS_KEY + fileMd5 + ":" + chunkIndex;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(statusKey));
    }
}
