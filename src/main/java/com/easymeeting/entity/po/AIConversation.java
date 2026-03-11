package com.easymeeting.entity.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * AI对话记录实体
 */
@Data
@TableName("ai_conversation")
public class AIConversation implements Serializable {
    
    /**
     * 对话ID
     */
    @TableId("conversation_id")
    private String conversationId;
    
    /**
     * 会议ID
     */
    private String meetingId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 用户消息
     */
    private String userMessage;
    
    /**
     * AI回复
     */
    private String aiResponse;
    
    /**
     * 消息类型
     */
    private String messageType;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
