package com.easymeeting.task;

import com.easymeeting.entity.dto.MeetingMemberDto;
import com.easymeeting.entity.enums.MeetingStatusEnum;
import com.easymeeting.entity.po.MeetingInfo;
import com.easymeeting.entity.po.MeetingReserve;
import com.easymeeting.entity.po.MeetingReserveMember;
import com.easymeeting.entity.query.MeetingInfoQuery;
import com.easymeeting.entity.query.MeetingReserveMemberQuery;
import com.easymeeting.mappers.MeetingReserveMapper;
import com.easymeeting.mappers.MeetingReserveMemberMapper;
import com.easymeeting.redis.RedisComponent;
import com.easymeeting.redis.RedisUtils;
import com.easymeeting.service.MeetingInfoService;
import com.easymeeting.service.UserNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 会议超时自动关闭定时任务
 * 每分钟检查一次所有进行中的会议，如果超过预定时长则自动关闭
 * 同时检查即将开始的会议，在开始前 5 分钟发送提醒通知
 */
@Slf4j
@Component
public class MeetingTimeoutTask {

    @Resource
    private MeetingInfoService meetingInfoService;
    
    @Resource
    private MeetingReserveMapper meetingReserveMapper;
    
    @Resource
    private MeetingReserveMemberMapper<MeetingReserveMember, MeetingReserveMemberQuery> meetingReserveMemberMapper;
    
    @Resource
    private RedisComponent redisComponent;
    
    @Resource
    private RedisUtils redisUtils;
    
    @Resource
    private UserNotificationService userNotificationService;

    // Redis key 前缀，用于标记已发送提醒的会议
    private static final String REMINDER_SENT_KEY_PREFIX = "meeting:reminder:sent:";
    
    // 提醒时间：会议开始前 5 分钟
    private static final int REMINDER_MINUTES_BEFORE = 5;

    /**
     * 每分钟执行一次，检查会议是否超时
     * cron表达式: 秒 分 时 日 月 周
     * 0 * * * * ? 表示每分钟的第0秒执行
     */
    @Scheduled(cron = "0 * * * * ?")
    public void checkMeetingTimeout() {
        try {
            log.debug("开始检查会议超时...");
            
            // 查询所有进行中的会议
            MeetingInfoQuery query = new MeetingInfoQuery();
            query.setStatus(MeetingStatusEnum.RUNING.getStatus());
            List<MeetingInfo> ongoingMeetings = meetingInfoService.findListByParam(query);
            
            if (ongoingMeetings == null || ongoingMeetings.isEmpty()) {
                log.debug("当前没有进行中的会议");
                return;
            }
            
            long currentTime = System.currentTimeMillis();
            int closedCount = 0;
            
            // 默认会议时长：1小时（60分钟）
            final int DEFAULT_DURATION_MINUTES = 60;
            
            for (MeetingInfo meeting : ongoingMeetings) {
                String meetingId = meeting.getMeetingId();
                
                // 获取会议预约信息（包含时长）
                MeetingReserve meetingReserve = (MeetingReserve) meetingReserveMapper.selectByMeetingId(meetingId);
                
                long meetingEndTime;
                int durationMinutes;
                
                if (meetingReserve == null) {
                    // 没有预约信息，使用会议创建时间 + 默认1小时
                    if (meeting.getCreateTime() == null) {
                        log.warn("会议 {} 没有创建时间，跳过超时检查", meetingId);
                        continue;
                    }
                    durationMinutes = DEFAULT_DURATION_MINUTES;
                    meetingEndTime = meeting.getCreateTime().getTime() + durationMinutes * 60 * 1000;
                    log.debug("会议 {} 没有预约信息，使用默认时长 {} 分钟", meetingId, durationMinutes);
                } else {
                    // 有预约信息，使用预约的时长
                    durationMinutes = meetingReserve.getDuration();
                    meetingEndTime = meetingReserve.getStartTime().getTime() + durationMinutes * 60 * 1000;
                }
                
                // 如果当前时间超过会议结束时间，则关闭会议
                if (currentTime > meetingEndTime) {
                    log.info("会议 {} 已超时，自动关闭。时长: {}分钟, 当前时间: {}", 
                             meetingId, 
                             durationMinutes,
                             new java.util.Date(currentTime));
                    
                    // 检查是否还有成员在会议中
                    List<MeetingMemberDto> activeMembers = redisComponent.getMeetingMemberList(meetingId);
                    if (!activeMembers.isEmpty()) {
                        log.info("会议 {} 还有 {} 个成员在线，将强制关闭", meetingId, activeMembers.size());
                    }
                    
                    // 调用结束会议方法
                    meetingInfoService.finishMeeting(meetingId, null);
                    closedCount++;
                }
            }
            
            if (closedCount > 0) {
                log.info("本次检查共关闭 {} 个超时会议", closedCount);
            } else {
                log.debug("本次检查没有超时的会议");
            }
            
        } catch (Exception e) {
            log.error("检查会议超时时发生错误", e);
        }
    }
    
    /**
     * 每分钟执行一次，检查即将开始的会议并发送提醒
     * cron表达式: 秒 分 时 日 月 周
     * 0 * * * * ? 表示每分钟的第0秒执行
     */
    @Scheduled(cron = "0 * * * * ?")
    public void checkMeetingReminder() {
        try {
            log.debug("开始检查会议提醒...");
            
            long currentTime = System.currentTimeMillis();
            long reminderTime = currentTime + REMINDER_MINUTES_BEFORE * 60 * 1000; // 5分钟后
            
            // 查询所有未开始的预约会议（status = 0）
            List<MeetingReserve> upcomingMeetings = meetingReserveMapper.selectUpcomingMeetings(
                new Date(currentTime), 
                new Date(reminderTime)
            );
            
            if (upcomingMeetings == null || upcomingMeetings.isEmpty()) {
                log.debug("当前没有即将开始的会议需要提醒");
                return;
            }
            
            int reminderCount = 0;
            
            for (MeetingReserve meeting : upcomingMeetings) {
                String meetingId = meeting.getMeetingId();
                
                // 检查是否已发送过提醒（使用 Redis 标记）
                String reminderKey = REMINDER_SENT_KEY_PREFIX + meetingId;
                String alreadySent = (String) redisUtils.get(reminderKey);
                
                if (alreadySent != null) {
                    log.debug("会议 {} 已发送过提醒，跳过", meetingId);
                    continue;
                }
                
                // 查询会议的所有参与者
                MeetingReserveMemberQuery memberQuery = new MeetingReserveMemberQuery();
                memberQuery.setMeetingId(meetingId);
                List<MeetingReserveMember> members = meetingReserveMemberMapper.selectList(memberQuery);
                
                if (members == null || members.isEmpty()) {
                    log.debug("会议 {} 没有参与者，跳过提醒", meetingId);
                    continue;
                }
                
                // 为每个参与者创建提醒通知
                for (MeetingReserveMember member : members) {
                    try {
                        userNotificationService.createMeetingReminderNotification(
                            meetingId,
                            member.getInviteUserId(),
                            meeting.getMeetingName(),
                            meeting.getStartTime()
                        );
                        reminderCount++;
                    } catch (Exception e) {
                        log.error("为用户 {} 创建会议提醒通知失败", member.getInviteUserId(), e);
                    }
                }
                
                // 标记该会议已发送提醒（设置过期时间为 24 小时）
                redisUtils.setEx(reminderKey, "1", 24 * 60 * 60L);
                
                log.info("会议 {} 提醒通知已发送，参与者数量: {}", meetingId, members.size());
            }
            
            if (reminderCount > 0) {
                log.info("本次检查共发送 {} 条会议提醒通知", reminderCount);
            } else {
                log.debug("本次检查没有需要发送的会议提醒");
            }
            
        } catch (Exception e) {
            log.error("检查会议提醒时发生错误", e);
        }
    }
}
