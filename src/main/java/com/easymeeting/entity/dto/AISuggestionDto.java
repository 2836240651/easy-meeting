package com.easymeeting.entity.dto;

import lombok.Data;
import java.util.List;

/**
 * AI会议建议DTO
 */
@Data
public class AISuggestionDto {
    
    /**
     * 建议列表
     */
    private List<String> suggestions;
    
    /**
     * 建议类型
     */
    private String type;
}
