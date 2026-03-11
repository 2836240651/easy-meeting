package com.easymeeting.mappers;

import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 *  数据库操作接口
 */
public interface MeetingReserveMapper<T,P> extends BaseMapper<T,P> {

	/**
	 * 根据MeetingId更新
	 */
	 Integer updateByMeetingId(@Param("bean") T t,@Param("meetingId") String meetingId);


	/**
	 * 根据MeetingId删除
	 */
	 Integer deleteByMeetingId(@Param("meetingId") String meetingId);


	/**
	 * 根据MeetingId获取对象
	 */
	 T selectByMeetingId(@Param("meetingId") String meetingId);

	/**
	 * 查询即将开始的会议（用于发送提醒）
	 * @param startTimeFrom 开始时间范围起点
	 * @param startTimeTo 开始时间范围终点
	 * @return 会议列表
	 */
	List<T> selectUpcomingMeetings(@Param("startTimeFrom") Date startTimeFrom, 
	                                @Param("startTimeTo") Date startTimeTo);

}

