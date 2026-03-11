package com.easymeeting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.easymeeting.entity.po.ChatMessage;
import com.easymeeting.mappers.ChatMessageMapper;
import com.easymeeting.service.ChatMessageService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 聊天消息服务实现
 */
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> 
    implements ChatMessageService {
    
    @Override
    public List<ChatMessage> getMessagesByMeetingId(String meetingId) {
        QueryWrapper<ChatMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("meeting_id", meetingId);
        wrapper.orderByAsc("create_time");
        return this.list(wrapper);
    }
    
    @Override
    public List<ChatMessage> getRecentMessages(String meetingId, int limit) {
        QueryWrapper<ChatMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("meeting_id", meetingId);
        wrapper.orderByDesc("create_time");
        wrapper.last("LIMIT " + limit);
        
        List<ChatMessage> messages = this.list(wrapper);
        // 反转列表,使其按时间正序排列
        java.util.Collections.reverse(messages);
        return messages;
    }
    
    @Override
    public boolean saveMessage(ChatMessage message) {
        return this.save(message);
    }
}
