package com.easymeeting.entity.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 会议摘要实体
 */
@Data
@TableName("meeting_summary")
public class MeetingSummary implements Serializable {
    
    /**
     * 摘要ID
     */
    private String summaryId;
    
    /**
     * 会议ID
     */
    private String meetingId;
    
    /**
     * 会议名称
     */
    private String meetingName;
    
    /**
     * 摘要内容
     */
    private String summaryContent;
    
    /**
     * 关键要点(JSON格式)
     */
    private String keyPoints;
    
    /**
     * 参与者列表(JSON格式)
     */
    private String participants;
    
    /**
     * 会议时长(分钟)
     */
    private Integer duration;
    
    /**
     * 消息数量
     */
    private Integer messageCount;
    
    /**
     * 生成者(AI/USER)
     */
    private String generatedBy;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
