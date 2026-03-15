package com.didadida.user.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 视频分片上传入参
 */
@Data
public class VideoChunkUploadDTO {

    /**
     * 文件唯一标识
     */
    private String fileMd5;

    /**
     * 分片索引（从0开始）
     */
    private Integer chunkIndex;

    /**
     * 总分片数
     */
    private Integer totalChunks;

    /**
     * 分片文件
     */
    private MultipartFile chunkFile;

    /**
     * 视频名称
     */
    private String videoName;

    /**
     * 用户ID
     */
    private Long userId;
}
