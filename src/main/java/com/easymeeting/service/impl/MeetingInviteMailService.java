package com.easymeeting.service.impl;

import com.easymeeting.entity.dto.TokenUserInfoDto;
import com.easymeeting.entity.po.MeetingInfo;
import com.easymeeting.entity.po.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class MeetingInviteMailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.mail.invite.enabled:false}")
    private boolean inviteMailEnabled;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${app.meeting.join-base-url:http://localhost:3001}")
    private String meetingJoinBaseUrl;

    public boolean sendInviteMail(UserInfo inviteUser, MeetingInfo meetingInfo, TokenUserInfoDto inviter) {
        if (!inviteMailEnabled) {
            log.info("会议邀请邮件未启用，跳过发送。inviteUserId={}", inviteUser == null ? null : inviteUser.getUserId());
            return false;
        }
        if (mailSender == null || !StringUtils.hasText(fromAddress)) {
            log.warn("会议邀请邮件发送器未配置完成，跳过发送。inviteUserId={}", inviteUser == null ? null : inviteUser.getUserId());
            return false;
        }
        if (inviteUser == null || !StringUtils.hasText(inviteUser.getEmail())) {
            log.warn("被邀请用户邮箱为空，跳过发送。inviteUserId={}", inviteUser == null ? null : inviteUser.getUserId());
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(inviteUser.getEmail());
            message.setSubject("EasyMeeting 会议邀请：" + defaultString(meetingInfo.getMeetingName(), "即时会议"));
            message.setText(buildMailContent(inviteUser, meetingInfo, inviter));
            mailSender.send(message);
            log.info("会议邀请邮件发送成功。inviteUserId={}, email={}", inviteUser.getUserId(), inviteUser.getEmail());
            return true;
        } catch (Exception e) {
            log.error("会议邀请邮件发送失败。inviteUserId={}, email={}", inviteUser.getUserId(), inviteUser.getEmail(), e);
            return false;
        }
    }

    private String buildMailContent(UserInfo inviteUser, MeetingInfo meetingInfo, TokenUserInfoDto inviter) {
        String meetingName = defaultString(meetingInfo.getMeetingName(), "即时会议");
        String inviterName = inviter == null ? "会议主持人" : defaultString(inviter.getNickName(), inviter.getUserId());
        String meetingNo = defaultString(meetingInfo.getMeetingNo(), "未生成");
        String joinPassword = StringUtils.hasText(meetingInfo.getJoinPassword()) ? meetingInfo.getJoinPassword() : "无";
        String joinUrl = meetingJoinBaseUrl + "/meeting/" + meetingInfo.getMeetingId();

        return "您好，" + defaultString(inviteUser.getNickName(), inviteUser.getUserId()) + "：\n\n"
                + inviterName + " 邀请您加入 EasyMeeting 会议。\n\n"
                + "会议主题：" + meetingName + "\n"
                + "会议号：" + meetingNo + "\n"
                + "会议ID：" + meetingInfo.getMeetingId() + "\n"
                + "入会密码：" + joinPassword + "\n"
                + "入会地址：" + joinUrl + "\n\n"
                + "您也可以在 EasyMeeting 客户端的通知中心中接受本次邀请。";
    }

    private String defaultString(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
