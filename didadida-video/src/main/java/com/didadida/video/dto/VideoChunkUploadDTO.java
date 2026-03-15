package com.didadida.video.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 视频分片上传请求DTO
 */
@Data
public class VideoChunkUploadDTO {
    /**
     * 文件唯一标识（前端生成MD5，用于断点续传和合并分片）
     */
    @NotNull(message = "文件MD5不能为空")
    private String fileMd5;

    /**
     * 分片索引（从0开始）
     */
    @NotNull(message = "分片索引不能为空")
    private Integer chunkIndex;

    /**
     * 总分片数
     */
    @NotNull(message = "总分片数不能为空")
    private Integer totalChunks;

    /**
     * 分片大小（字节）
     */
    @NotNull(message = "分片大小不能为空")
    private Long chunkSize;

    /**
     * 分片文件
     */
    @NotNull(message = "分片文件不能为空")
    private MultipartFile chunkFile;

    /**
     * 原始文件名
     */
    @NotNull(message = "原始文件名不能为空")
    private String originalFileName;
    
    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
