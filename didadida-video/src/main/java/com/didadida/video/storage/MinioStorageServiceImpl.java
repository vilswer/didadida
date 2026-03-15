package com.didadida.video.storage;

import com.didadida.video.config.MinioConfig;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageServiceImpl implements StorageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Override
    public String generatePresignedPlayUrl(String fileName, int expireSeconds) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .method(Method.GET)
                            .expiry(expireSeconds, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            log.error("生成MinIO预签名URL失败，fileName:{}", fileName, e);
            throw new RuntimeException("生成视频播放地址失败");
        }
    }

    @Override
    public boolean checkFileExists(String fileName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            log.warn("文件不存在，fileName:{}", fileName);
            return false;
        }
    }

    @Override
    public void uploadChunk(String fileName, InputStream inputStream, long size) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .stream(inputStream, size, -1)
                            .build()
            );
        } catch (Exception e) {
            log.error("上传分片失败，fileName:{}", fileName, e);
            throw new RuntimeException("分片上传失败");
        }
    }

    @Override
    public String mergeChunks(String fileMd5, String originalFileName) {
        // 1. 改为根目录存储：直接用 原始文件名（去掉特殊字符，避免MinIO报错）
        String finalFileName = originalFileName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "");
        List<ComposeSource> sources = new ArrayList<>();

        try {
            // 2. 优化分片遍历 + 异常处理（抽离方法，提升可读性）
            listChunkItems(fileMd5, sources);

            // 3. 合并分片到根目录
            minioClient.composeObject(
                    ComposeObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(finalFileName) // 根目录存储
                            .sources(sources)
                            .build()
            );

            // 4. 清理分片文件
            deleteChunkFiles(sources);

            log.info("分片合并成功，最终文件路径：{}", finalFileName);
            return finalFileName;
        } catch (Exception e) {
            log.error("合并分片失败，fileMd5:{}, originalFileName:{}", fileMd5, originalFileName, e);
            throw new RuntimeException("分片合并失败");
        }
    }

    @Override
    public void uploadFile(String fileName, InputStream inputStream, long size) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .stream(inputStream, size, -1)
                            .build()
            );
        } catch (Exception e) {
            log.error("上传文件失败，fileName:{}", fileName, e);
            throw new RuntimeException("文件上传失败");
        }
    }

    /**
     * 遍历分片文件并添加到合并源列表（统一异常处理）
     */
    private void listChunkItems(String fileMd5, List<ComposeSource> sources) {
        Iterable<Result<Item>> chunkItems = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .prefix("chunk/" + fileMd5 + "/")
                        .recursive(true)
                        .build()
        );

        for (Result<Item> itemResult : chunkItems) {
            try {
                Item item = itemResult.get(); // 捕获MinIO异常
                if (!item.isDir()) {
                    sources.add(
                            ComposeSource.builder()
                                    .bucket(minioConfig.getBucketName())
                                    .object(item.objectName())
                                    .build()
                    );
                }
            } catch (Exception e) {
                log.warn("读取分片文件失败，fileMd5:{}, 异常：{}", fileMd5, e.getMessage());
                // 单个分片读取失败不中断整体流程，仅日志记录
            }
        }

        if (sources.isEmpty()) {
            throw new RuntimeException("未找到可合并的分片文件");
        }
    }


    /**
     * 删除分片文件（统一异常处理）
     */
    private void deleteChunkFiles(List<ComposeSource> sources) {
        for (ComposeSource source : sources) {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(minioConfig.getBucketName())
                                .object(source.object())
                                .build()
                );
            } catch (Exception e) {
                log.warn("删除分片文件失败，fileName:{}, 异常：{}", source.object(), e.getMessage());
                // 单个分片删除失败不中断
            }
        }
    }
}