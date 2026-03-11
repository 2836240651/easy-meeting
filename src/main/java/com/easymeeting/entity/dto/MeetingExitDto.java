package com.easymeeting.entity.dto;

import java.io.Serializable;
import java.util.List;

public class MeetingExitDto implements Serializable {
public String exitUserId;
private List<MeetingMemberDto> meetingMemberDtoList;
private Integer exitStatus;

    public String getExitUserId() {
        return exitUserId;
    }

    public void setExitUserId(String exitUserId) {
        this.exitUserId = exitUserId;
    }

    public List<MeetingMemberDto> getMeetingMemberDtoList() {
        return meetingMemberDtoList;
    }

    public void setMeetingMemberDtoList(List<MeetingMemberDto> meetingMemberDtoList) {
        this.meetingMemberDtoList = meetingMemberDtoList;
    }

    public Integer getExitStatus() {
        return exitStatus;
    }

    public void setExitStatus(Integer exitStatus) {
        this.exitStatus = exitStatus;
    }
}
