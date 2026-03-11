package com.easymeeting.service;

import com.easymeeting.entity.po.ChatMessage;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * 聊天消息服务接口
 */
public interface ChatMessageService extends IService<ChatMessage> {
    
    /**
     * 获取会议的所有聊天消息
     * @param meetingId 会议ID
     * @return 聊天消息列表
     */
    List<ChatMessage> getMessagesByMeetingId(String meetingId);
    
    /**
     * 获取会议的最近N条消息
     * @param meetingId 会议ID
     * @param limit 消息数量
     * @return 聊天消息列表
     */
    List<ChatMessage> getRecentMessages(String meetingId, int limit);
    
    /**
     * 保存聊天消息
     * @param message 聊天消息
     * @return 是否成功
     */
    boolean saveMessage(ChatMessage message);
}
