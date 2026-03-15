package com.didadida.user.controller;

import com.didadida.common.result.Result;
import com.didadida.user.feign.VideoFeignClient;
import com.didadida.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 视频上传控制器
 */
@RestController
@RequestMapping("/user/video")
@Tag(name = "视频上传", description = "用户视频上传相关接口")
public class VideoUploadController {

    @Autowired
    private VideoFeignClient videoFeignClient;
    
    @Autowired
    private UserService userService;

    /**
     * 上传视频分片
     */
    @PostMapping("/upload/chunk")
    @Operation(summary = "上传视频分片")
    public Result<String> uploadChunk(
            @RequestHeader("Authorization") String token,
            @RequestPart("file") MultipartFile file,
            @RequestParam("chunkIndex") Integer chunkIndex,
            @RequestParam("totalChunks") Integer totalChunks,
            @RequestParam("fileName") String fileName
    ) {
        // 验证令牌，获取用户ID
        Long userId = userService.verifyToken(token);
        
        // 调用video模块的分片上传接口
        Result<String> result = videoFeignClient.uploadChunk(file, chunkIndex, totalChunks, fileName, userId);
        return result;
    }

    /**
     * 合并视频分片
     */
    @PostMapping("/upload/merge")
    @Operation(summary = "合并视频分片")
    public Result<String> mergeChunks(
            @RequestHeader("Authorization") String token,
            @RequestParam("fileName") String fileName,
            @RequestParam("totalChunks") Integer totalChunks
    ) {
        // 验证令牌，获取用户ID
        Long userId = userService.verifyToken(token);
        
        // 调用video模块的分片合并接口
        Result<String> result = videoFeignClient.mergeChunks(fileName, totalChunks, userId);
        return result;
    }
}