package com.didadida.user.feign;

import com.didadida.common.result.Result;
import com.didadida.user.dto.VideoChunkUploadDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 视频服务Feign Client
 */
@FeignClient(name = "didadida-video")
public interface VideoFeignClient {

    /**
     * 上传视频分片
     */
    @PostMapping(value = "/video/upload/chunk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Result<String> uploadChunk(
            @RequestPart("file") MultipartFile file,
            @RequestParam("chunkIndex") Integer chunkIndex,
            @RequestParam("totalChunks") Integer totalChunks,
            @RequestParam("fileName") String fileName,
            @RequestParam("userId") Long userId
    );


    /**
     * 合并视频分片
     */
    @PostMapping("/video/upload/merge")
    Result<String> mergeChunks(
            @RequestParam("fileName") String fileName,
            @RequestParam("totalChunks") Integer totalChunks,
            @RequestParam("userId") Long userId
    );
}