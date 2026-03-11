package com.easymeeting.entity.dto;

import lombok.Data;
import java.util.List;

/**
 * AI会议摘要DTO
 */
@Data
public class AISummaryDto {
    
    /**
     * 会议摘要
     */
    private String summary;
    
    /**
     * 关键要点
     */
    private List<String> keyPoints;
    
    /**
     * 参与者统计
     */
    private List<String> participants;
    
    /**
     * 会议时长(分钟)
     */
    private Integer duration;
    
    /**
     * 消息总数
     */
    private Integer messageCount;
    
    /**
     * 会议名称
     */
    private String meetingName;

    /**
     * 发言片段数量
     */
    private Integer speechSegmentCount;

    /**
     * 摘要来源说明
     */
    private String contextSource;
}
