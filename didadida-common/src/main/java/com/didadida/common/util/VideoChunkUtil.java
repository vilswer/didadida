package com.didadida.common.util;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.crypto.digest.DigestUtil;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;

public class VideoChunkUtil {
    // 分片大小：10MB（10*1024*1024 字节），可自定义
    private static final long CHUNK_SIZE = 20 * 1024 * 1024;

    public static void main(String[] args) {
        // ========== 请修改为你的本地视频路径和分片输出目录 ==========
        String videoPath = "D:/Eve 自宅雑談_2_prob4.mp4";
        String chunkOutDir = "D:/video-chunk/";

        // 清空并重建分片目录（避免旧分片干扰）
        FileUtil.del(chunkOutDir);
        FileUtil.mkdir(chunkOutDir);

        File videoFile = new File(videoPath);
        if (!videoFile.exists()) {
            System.err.println("错误：视频文件不存在！路径：" + videoPath);
            return;
        }

        try {
            // 1. 生成视频文件MD5（核心，与上传接口的fileMd5一致）
            String fileMd5 = DigestUtil.md5Hex(new FileReader(videoFile).readBytes());
            System.out.println("✅ 视频文件MD5：" + fileMd5);

            // 2. 计算文件总大小和总分片数
            long fileTotalSize = videoFile.length();
            int totalChunks = (int) Math.ceil((double) fileTotalSize / CHUNK_SIZE);
            System.out.println("✅ 视频总大小：" + fileTotalSize + " 字节，总分片数：" + totalChunks);

            // 3. 逐片读取并写入（核心优化）
            try (RandomAccessFile raf = new RandomAccessFile(videoFile, "r")) {
                byte[] buffer = new byte[(int) CHUNK_SIZE]; // 固定大小的缓冲区
                for (int i = 0; i < totalChunks; i++) {
                    // 计算当前分片的起始偏移量
                    long start = i * CHUNK_SIZE;
                    raf.seek(start); // 移动到分片起始位置

                    // 读取当前分片的字节数（最多CHUNK_SIZE）
                    int readBytes = raf.read(buffer);

                    // 处理读取结果：readBytes=-1 表示无数据，跳过
                    if (readBytes == -1) {
                        System.out.println("ℹ️ 分片" + i + "无数据，跳过");
                        continue;
                    }

                    // 关键优化：截取有效字节数组，避免空字节写入
                    byte[] validData = new byte[readBytes];
                    System.arraycopy(buffer, 0, validData, 0, readBytes);

                    // 生成当前分片文件路径
                    String chunkFileName = chunkOutDir + "chunk-" + i + ".mp4";
                    // 写入有效字节数组（无多余空字节）
                    Files.write(Paths.get(chunkFileName), validData);

                    System.out.println("✅ 生成分片" + i + "：" + chunkFileName + "，有效大小：" + readBytes + " 字节");
                }
            }

            System.out.println("\n🎉 分片生成完成！输出目录：" + chunkOutDir);
        } catch (Exception e) {
            System.err.println("❌ 分片生成失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
}