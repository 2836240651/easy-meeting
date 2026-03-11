package com.easymeeting.entity.enums;

public enum MeetingJoinTypeEnum {
NO_PASSWORD(0,"无需密码"),
PASSWORD(1,"需要密码");
 Integer Status;
 String desc;
 MeetingJoinTypeEnum(Integer Status, String desc) {
     this.Status = Status;
     this.desc = desc;
 }
    MeetingStatusEnum getStatusEnum(Integer status) {
        for (MeetingStatusEnum item : MeetingStatusEnum.values()) {
            if (item.equals(status));
            return item;
        }
        return null;
    }
    public Integer getStatus() {
        return Status;
    }

    public String getDesc() {
        return desc;
    }
}
