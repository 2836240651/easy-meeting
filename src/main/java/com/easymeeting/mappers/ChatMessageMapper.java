package com.easymeeting.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easymeeting.entity.po.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天消息Mapper
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
