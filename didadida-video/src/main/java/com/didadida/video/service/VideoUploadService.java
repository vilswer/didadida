package com.didadida.video.service;

import com.didadida.video.dto.VideoChunkMergeDTO;
import com.didadida.video.dto.VideoChunkUploadDTO;

/**
 * 视频上传服务接口
 */
public interface VideoUploadService {
    /**
     * 上传分片
     * @param dto 分片上传参数
     * @return 是否上传成功
     */
    boolean uploadChunk(VideoChunkUploadDTO dto);

    /**
     * 合并分片并发布视频
     * @param dto 合并参数
     * @return 视频ID
     */
    Long mergeChunksAndPublish(VideoChunkMergeDTO dto);

    /**
     * 检查分片是否已上传（断点续传）
     * @param fileMd5 文件MD5
     * @param chunkIndex 分片索引
     * @return 是否已上传
     */
    boolean checkChunkExists(String fileMd5, Integer chunkIndex);
}
