package com.easymeeting.entity.enums;

public enum MeetingStatusEnum {
    RUNING(0,"会议进行中"),
    FINISHED(1,"会议结束");
    private Integer status;
    private String desc;
    private MeetingStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }
    public static MeetingStatusEnum getEnum(Integer status) {
        for (MeetingStatusEnum item : MeetingStatusEnum.values()) {
            if (item.status.equals(status)) {
                return item;
            }
                 }
        return null;
    }
    public int getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }

}
