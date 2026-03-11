package com.easymeeting.entity.dto;

import lombok.Data;
import java.util.List;

/**
 * AI消息DTO
 */
@Data
public class AIMessageDto {
    
    /**
     * AI响应内容
     */
    private String response;
    
    /**
     * 消息类型
     */
    private String type;
    
    /**
     * 可执行的操作列表
     */
    private List<String> actions;
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 错误信息
     */
    private String error;
}
