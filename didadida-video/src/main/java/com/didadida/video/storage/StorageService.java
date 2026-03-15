package com.didadida.video.storage;

import java.io.InputStream;

/**
 * 通用存储服务接口
 * 后续切换OSS仅需新增该接口的实现类，业务代码无需修改
 */
public interface StorageService {

    /**
     * 生成文件的预签名播放URL（核心：前端直接通过该URL播放视频，不经过后端服务，扛住1w+并发）
     * @param fileName 文件名（视频在存储中的完整路径）
     * @param expireSeconds 链接过期时间（秒）
     * @return 预签名的可播放URL
     */
    String generatePresignedPlayUrl(String fileName, int expireSeconds);

    /**
     * 检查文件是否存在
     * @param fileName 文件名
     * @return 是否存在
     */
    boolean checkFileExists(String fileName);

    /**
     * 上传分片文件
     * @param fileName 分片存储路径（如：chunk/{fileMd5}/{chunkIndex}）
     * @param inputStream 文件流
     * @param size 文件大小（字节）
     */
    void uploadChunk(String fileName, InputStream inputStream, long size);

    /**
     * 合并分片文件（MinIO专用，OSS需单独实现）
     * @param fileMd5 文件唯一标识
     * @param originalFileName 原始文件名
     * @return 合并后的文件存储路径
     */
    String mergeChunks(String fileMd5, String originalFileName);

    /**
     * 上传单个小文件（封面/小视频）
     * @param fileName 存储路径
     * @param inputStream 文件流
     * @param size 文件大小
     */
    void uploadFile(String fileName, InputStream inputStream, long size);
}
