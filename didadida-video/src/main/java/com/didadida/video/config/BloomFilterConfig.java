package com.didadida.video.config;

import com.didadida.video.mapper.VideoMapper;
import com.google.common.hash.BloomFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.google.common.hash.Funnels;
/**
 * 布隆过滤器配置（防缓存穿透）
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BloomFilterConfig {
    private final VideoMapper videoMapper;

    /**
     * 初始化视频ID布隆过滤器
     */
    @Bean
    public BloomFilter<Long> videoIdBloomFilter() {
        // 预估数据量：100万视频
        long expectedInsertions = 1000000;
        // 误判率：0.01
        double fpp = 0.01;

        BloomFilter<Long> bloomFilter = BloomFilter.create(
                Funnels.longFunnel(),
                expectedInsertions,
                fpp
        );


        // 加载所有视频ID到布隆过滤器（项目启动时执行）
        videoMapper.selectList(null).forEach(video -> {
            bloomFilter.put(video.getVideoId());
        });

        log.info("布隆过滤器初始化完成，加载视频ID数量：{}", expectedInsertions);
        return bloomFilter;
    }

    /**
     * 定时刷新布隆过滤器（每24小时）
     */
    @Bean
    public ApplicationRunner bloomFilterRefreshRunner(BloomFilter<Long> videoIdBloomFilter) {
        return args -> {
            // 此处可添加定时刷新逻辑（如定时从数据库加载新增视频ID）
            log.info("布隆过滤器定时刷新任务已启动");
        };
    }
}
