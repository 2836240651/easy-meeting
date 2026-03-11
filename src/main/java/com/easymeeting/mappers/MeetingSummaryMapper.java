package com.easymeeting.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easymeeting.entity.po.MeetingSummary;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会议摘要Mapper
 */
@Mapper
public interface MeetingSummaryMapper extends BaseMapper<MeetingSummary> {
}
