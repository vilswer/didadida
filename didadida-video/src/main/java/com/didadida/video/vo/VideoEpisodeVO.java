package com.didadida.video.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 视频选集VO
 *
 * @author 奶油盒桃
 * @date 2026-03-10
 */
@Data
public class VideoEpisodeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 选集ID
     */
    private Long episodeId;

    /**
     * 视频ID
     */
    private Long videoId;

    /**
     * 选集标题
     */
    private String episodeTitle;

    /**
     * 选集序号
     */
    private Integer episodeOrder;

    /**
     * 选集时长（秒）
     */
    private Integer duration;

    /**
     * 选集时长格式化显示
     */
    private String durationFormat;
}
