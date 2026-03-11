package com.easymeeting.entity.dto;

import com.easymeeting.entity.po.MeetingReserve;
import com.easymeeting.entity.po.UserInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MeetingReserveDetailDto implements Serializable {
    
    private MeetingReserve meetingReserve;
    private List<UserInfo> inviteMembers;
    private Boolean isCreator;
    private String meetingStatus;
    
    public MeetingReserve getMeetingReserve() {
        return meetingReserve;
    }
    
    public void setMeetingReserve(MeetingReserve meetingReserve) {
        this.meetingReserve = meetingReserve;
    }
    
    public List<UserInfo> getInviteMembers() {
        return inviteMembers;
    }
    
    public void setInviteMembers(List<UserInfo> inviteMembers) {
        this.inviteMembers = inviteMembers;
    }
    
    public Boolean getIsCreator() {
        return isCreator;
    }
    
    public void setIsCreator(Boolean isCreator) {
        this.isCreator = isCreator;
    }
    
    public String getMeetingStatus() {
        return meetingStatus;
    }
    
    public void setMeetingStatus(String meetingStatus) {
        this.meetingStatus = meetingStatus;
    }
}
