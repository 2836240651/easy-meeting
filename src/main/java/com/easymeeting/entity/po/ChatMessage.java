package com.easymeeting.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 聊天消息实体类
 */
@Data
@TableName("meeting_chat_message")
public class ChatMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 消息ID
     */
    @TableId(type = IdType.AUTO)
    private Long messageId;
    
    /**
     * 会议ID
     */
    private String meetingId;
    
    /**
     * 发送者用户ID
     */
    private String senderId;
    
    /**
     * 发送者昵称
     */
    private String senderName;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息类型 (text/image/file等)
     */
    private String messageType;
    
    /**
     * 创建时间
     */
    private Date createTime;
}
