package com.easymeeting.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easymeeting.entity.po.AIConversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI对话记录Mapper
 */
@Mapper
public interface AIConversationMapper extends BaseMapper<AIConversation> {
}
