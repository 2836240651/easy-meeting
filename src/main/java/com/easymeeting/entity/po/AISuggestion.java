package com.easymeeting.entity.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * AI建议记录实体
 */
@Data
@TableName("ai_suggestion")
public class AISuggestion implements Serializable {
    
    /**
     * 建议ID
     */
    @TableId("suggestion_id")
    private String suggestionId;
    
    /**
     * 会议ID
     */
    private String meetingId;
    
    /**
     * 建议类型
     */
    private String suggestionType;
    
    /**
     * 建议内容(JSON格式)
     */
    private String suggestions;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
