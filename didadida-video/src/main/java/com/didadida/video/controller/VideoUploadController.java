package com.didadida.video.controller;

import com.didadida.common.result.Result;
import com.didadida.video.dto.VideoChunkMergeDTO;
import com.didadida.video.dto.VideoChunkUploadDTO;
import com.didadida.video.service.VideoUploadService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/video/upload")
@RequiredArgsConstructor
@Validated
public class VideoUploadController {
    private final VideoUploadService videoUploadService;

    /**
     * 上传分片
     */
    @PostMapping("/chunk")
    public Result<String> uploadChunk(
            @RequestPart("file") MultipartFile file,
            @RequestParam("chunkIndex") Integer chunkIndex,
            @RequestParam("totalChunks") Integer totalChunks,
            @RequestParam("fileName") String fileName,
            @RequestParam("userId") Long userId
    ) {
        VideoChunkUploadDTO dto = new VideoChunkUploadDTO();
        dto.setChunkFile(file);
        dto.setChunkIndex(chunkIndex);
        dto.setTotalChunks(totalChunks);
        dto.setFileMd5(fileName); // 使用fileName作为fileMd5，实际项目中应该使用文件的MD5值
        dto.setUserId(userId);
        
        boolean success = videoUploadService.uploadChunk(dto);
        return Result.success(success ? "上传成功" : "上传失败");
    }

    /**
     * 检查分片是否已上传（断点续传）
     */
    @GetMapping("/chunk/check")
    public Result<Boolean> checkChunk(
            @RequestParam @NotNull(message = "文件MD5不能为空") String fileMd5,
            @RequestParam @NotNull(message = "分片索引不能为空") Integer chunkIndex
    ) {
        boolean exists = videoUploadService.checkChunkExists(fileMd5, chunkIndex);
        return Result.success(exists);
    }

    /**
     * 合并分片并发布视频
     */
    @PostMapping("/merge")
    public Result<String> mergeChunks(
            @RequestParam("fileName") String fileName,
            @RequestParam("totalChunks") Integer totalChunks,
            @RequestParam("userId") Long userId
    ) {
        VideoChunkMergeDTO dto = new VideoChunkMergeDTO();
        dto.setFileMd5(fileName); // 使用fileName作为fileMd5，实际项目中应该使用文件的MD5值
        dto.setOriginalFileName(fileName);
        dto.setUserId(userId);
        dto.setTitle(fileName); // 实际项目中应该从请求参数中获取
        dto.setDescription(""); // 实际项目中应该从请求参数中获取
        dto.setCategoryId(1L); // 实际项目中应该从请求参数中获取
        
        Long videoId = videoUploadService.mergeChunksAndPublish(dto);
        return Result.success("视频上传成功，视频ID：" + videoId);
    }
}
