package com.easymeeting.service;

import com.easymeeting.entity.dto.AIMessageDto;
import com.easymeeting.entity.dto.AISummaryDto;
import com.easymeeting.entity.dto.AISuggestionDto;
import com.easymeeting.entity.dto.SmartMeetingSummaryDto;

/**
 * AI助手服务接口
 */
public interface AIAssistantService {
    
    /**
     * 发送消息给AI助手
     * @param meetingId 会议ID
     * @param userId 用户ID
     * @param message 用户消息
     * @return AI响应
     */
    AIMessageDto chat(String meetingId, String userId, String message);
    
    /**
     * 生成会议摘要
     * @param meetingId 会议ID
     * @return 会议摘要
     */
    AISummaryDto generateSummary(String meetingId);
    
    /**
     * 获取会议建议
     * @param meetingId 会议ID
     * @return 会议建议
     */
    AISuggestionDto getSuggestions(String meetingId);
    
    /**
     * 执行AI命令
     * @param meetingId 会议ID
     * @param userId 用户ID
     * @param command 命令
     * @return 执行结果
     */
    AIMessageDto executeCommand(String meetingId, String userId, String command);
    
    /**
     * 生成智能会议纪要（结构化）
     * @param meetingId 会议ID
     * @return 智能会议纪要
     */
    SmartMeetingSummaryDto generateSmartSummary(String meetingId);

    /**
     * 保存会议中的成员发言文本，用于联合生成摘要
     */
    void saveSpeechSegment(String meetingId, String userId, String speakerName, String content);
}
