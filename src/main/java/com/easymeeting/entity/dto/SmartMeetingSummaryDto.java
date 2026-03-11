package com.easymeeting.entity.dto;

import lombok.Data;
import java.util.List;

/**
 * 智能会议纪要 DTO
 */
@Data
public class SmartMeetingSummaryDto {
    
    // 会议基本信息
    private String meetingId;
    private String meetingName;
    private String meetingTime;
    private Integer duration;
    private Integer participantCount;
    private List<String> participants;
    
    // 会议概要
    private String overview;
    
    // 讨论要点
    private List<String> discussionPoints;
    
    // 关键决策
    private List<String> decisions;
    
    // 待办事项
    private List<ActionItem> actionItems;
    
    // 重要时刻
    private List<String> highlights;
    
    // 参会者反馈/情绪
    private String sentiment;
    
    // AI 建议
    private List<String> suggestions;
    
    // 完整记录
    private String fullTranscript;
    
    @Data
    public static class ActionItem {
        private String task;
        private String assignee;
        private String deadline;
        private String priority; // HIGH, MEDIUM, LOW
    }
}
