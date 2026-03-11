package com.easymeeting.redis;
import com.easymeeting.entity.constants.Constants;
import com.easymeeting.entity.dto.MeetingMemberDto;
import com.easymeeting.entity.dto.SystemSettingDto;
import com.easymeeting.entity.dto.TokenUserInfoDto;
import com.easymeeting.entity.enums.MeetingMemberStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
@Slf4j
@Component
public class RedisComponent {
@Resource
private RedisUtils redisUtils;
public  String saveCheckCode(String code){
String checkCodeKey= UUID.randomUUID().toString();
redisUtils.setEx(Constants.REDIS_KEY_CHECK_CODE+checkCodeKey,code,60L * 10);
return checkCodeKey;
}

public String getcheckCode(String checkCodeKey){
return (String) redisUtils.get(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey);
}
public void cleanCheckCode(String checkCodeKey){
    redisUtils.delete(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey);
}

    public void saveTokenUserInfoDto(TokenUserInfoDto tokenUserInfoDto) {
    redisUtils.setEx(Constants.REDIS_KEY_WS_TOKEN+tokenUserInfoDto.getToken(),
    tokenUserInfoDto,Constants.REDIS_KEY_EXPIRES_DAY);
    redisUtils.setEx(Constants.REDIS_KEY_WS_TOKEN_USERID+tokenUserInfoDto.getUserId(),
    tokenUserInfoDto.getToken(),Constants.REDIS_KEY_EXPIRES_DAY);
}

    public TokenUserInfoDto getTokenUserByToken(String token) {
        return (TokenUserInfoDto)redisUtils.get(Constants.REDIS_KEY_WS_TOKEN + token);
    }
    public TokenUserInfoDto getTokenByUserId(String userId) {
        String token=(String)redisUtils.get(Constants.REDIS_KEY_WS_TOKEN_USERID + userId);
        return getTokenUserByToken(token);

    }
    public void add2meeting(String meetingId, MeetingMemberDto meetingMemberDto){
    redisUtils.hset(Constants.REDIS_KEY_MEETING_ROOM+meetingId,meetingMemberDto.getUserId(),meetingMemberDto);
    }

    /**
     * 获取会议中所有成员（包括所有状态：正常、退出、踢出、拉黑）
     * 用于需要获取完整成员信息的场景，如清理资源、统计等
     * 
     * @param meetingId 会议ID
     * @return 所有成员列表（按加入时间排序）
     */
    /**
     * 获取会议中所有成员（包括所有状态：正常、退出、踢出、拉黑）
     * 用于需要获取完整成员信息的场景，如清理资源、统计等
     * 
     * @param meetingId 会议ID
     * @return 所有成员列表（按加入时间排序）
     */
    public List<MeetingMemberDto> getAllMeetingMembers(String meetingId){
        List<MeetingMemberDto> meetingMemberDtoList =
         redisUtils.hvals(Constants.REDIS_KEY_MEETING_ROOM + meetingId);

        log.debug("获取会议所有成员: meetingId={}, 总数={}", meetingId, meetingMemberDtoList.size());

        // 使用nullsLast处理joinTime为null的情况，避免NullPointerException
        meetingMemberDtoList = meetingMemberDtoList.stream()
         .sorted(Comparator.comparing(MeetingMemberDto::getJoinTime, Comparator.nullsLast(Comparator.naturalOrder())))
         .collect(Collectors.toList());

        return meetingMemberDtoList;
    }


    /**
     * 获取会议中正常状态的成员列表（过滤掉被踢出和被拉黑的成员）
     * 这是默认的获取成员列表方法，只返回status=1（NORMAL）的成员
     * 
     * @param meetingId 会议ID
     * @return 正常状态的成员列表（按加入时间排序）
     */
    public List<MeetingMemberDto> getMeetingMemberList(String meetingId){
        List<MeetingMemberDto> allMembers = getAllMeetingMembers(meetingId);
        
        // 只返回状态为NORMAL的成员，过滤掉被踢出(3)和被拉黑(4)的成员
        List<MeetingMemberDto> normalMembers = allMembers.stream()
         .filter(member -> MeetingMemberStatusEnum.NORMAL.getStatus().equals(member.getStatus()))
         .collect(Collectors.toList());
         
        log.info("获取会议正常成员: meetingId={}, 总成员数={}, 正常成员数={}", 
                 meetingId, allMembers.size(), normalMembers.size());
        
        // 记录被过滤掉的成员（调试用）
        if (allMembers.size() > normalMembers.size()) {
            allMembers.stream()
                .filter(member -> !MeetingMemberStatusEnum.NORMAL.getStatus().equals(member.getStatus()))
                .forEach(member -> log.debug("过滤掉非正常成员: userId={}, nickName={}, status={}", 
                         member.getUserId(), member.getNickName(), member.getStatus()));
        }
        
        return normalMembers;
    }
    public MeetingMemberDto getMeetingMember(String meetingId,String userId){
       return  (MeetingMemberDto)redisUtils.hget(Constants.REDIS_KEY_MEETING_ROOM + meetingId, userId);
    }
    public Boolean exitMeeting(String meetingId, String userId, MeetingMemberStatusEnum statusEnum){
        MeetingMemberDto memberDto = getMeetingMember(meetingId, userId);
        if (memberDto==null){
            log.warn("退出会议失败，成员不存在: meetingId={}, userId={}", meetingId, userId);
            return false;
        }
        
        Integer oldStatus = memberDto.getStatus();
        memberDto.setStatus(statusEnum.getStatus());
        add2meeting(meetingId, memberDto);
        
        log.info("更新成员状态: meetingId={}, userId={}, nickName={}, oldStatus={}, newStatus={}", 
                 meetingId, userId, memberDto.getNickName(), oldStatus, statusEnum.getStatus());
        
        return true;
    }

    public void removeAllMeetingMember(String meetingId) {
        // 这里需要获取所有成员（包括非正常状态），因为要清理所有数据
        List<MeetingMemberDto> meetingMemberList = getAllMeetingMembers(meetingId);
        List<String> userIdList  = meetingMemberList.stream().map(MeetingMemberDto::getUserId).
        collect(Collectors.toList());
        if (userIdList.isEmpty()){
            log.info("会议无成员，无需清理: meetingId={}", meetingId);
            return;
        }
        log.info("清理会议所有成员: meetingId={}, 成员数={}", meetingId, userIdList.size());
        redisUtils.hdel(Constants.REDIS_KEY_MEETING_ROOM+meetingId,userIdList.toArray(new String[userIdList.size()]));
    }

    public void inviteInfo(String currentMeetingId, String userId) {
    redisUtils.setEx(Constants.REDIS_KEY_INVITE_MEMBER+userId+currentMeetingId,
    currentMeetingId,Constants.REDIS_KEY_EXPIRES_ONE_MIN*5);

}
    public String getInviteInfo(String userId,String meetingId){
    return (String) redisUtils.get(Constants.REDIS_KEY_INVITE_MEMBER+userId+meetingId);
    }
    public void saveSystemSetting(SystemSettingDto systemSettingDto){
    redisUtils.set(Constants.REDIS_KEY_SYS_SETTING, systemSettingDto);
    }
    public SystemSettingDto getSystemSetting(){
        SystemSettingDto systemSettingDto=(SystemSettingDto)redisUtils.get(Constants.REDIS_KEY_SYS_SETTING);
        systemSettingDto= systemSettingDto==null?new SystemSettingDto():systemSettingDto;

        return systemSettingDto;
}

    public void cleanTokenByUserId(String userId) {
        TokenUserInfoDto tokenByUserId = getTokenByUserId(userId);
        String token = tokenByUserId.getToken();
        if (!token.isEmpty()){
            redisUtils.delete(Constants.REDIS_KEY_WS_TOKEN + token);
        }
        redisUtils.delete(Constants.REDIS_KEY_WS_TOKEN_USERID+userId);
}
}
