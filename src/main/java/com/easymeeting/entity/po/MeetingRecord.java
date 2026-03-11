package com.easymeeting.entity.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 会议记录实体
 */
@Data
public class MeetingRecord implements Serializable {
    
    /**
     * 记录ID
     */
    private String recordId;
    
    /**
     * 会议ID
     */
    private String meetingId;
    
    /**
     * 会议名称
     */
    private String meetingName;
    
    /**
     * 主持人ID
     */
    private String hostUserId;
    
    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    
    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    
    /**
     * 时长(分钟)
     */
    private Integer duration;
    
    /**
     * 参与人数
     */
    private Integer participantCount;
    
    /**
     * 关联的摘要ID
     */
    private String summaryId;
    
    /**
     * 聊天记录(JSON格式)
     */
    private String chatMessages;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
