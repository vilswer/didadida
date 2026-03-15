package com.didadida.video.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 合并分片请求DTO
 */
@Data
public class VideoChunkMergeDTO {
    /**
     * 文件唯一标识（与分片上传一致）
     */
    @NotNull(message = "文件MD5不能为空")
    private String fileMd5;

    /**
     * 原始文件名
     */
    @NotNull(message = "原始文件名不能为空")
    private String originalFileName;

    /**
     * UP主用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 视频分区ID
     */
    @NotNull(message = "分区ID不能为空")
    private Long categoryId;

    /**
     * 视频标题
     */
    @NotNull(message = "视频标题不能为空")
    private String title;

    /**
     * 视频简介
     */
    private String description;
}
