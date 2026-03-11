package com.easymeeting.entity.enums;

public enum AppUpdateStatusEnum {
    INIT(0,"未发布"),
    GRAYSCALE(1,"灰度发布"),
    ALL(2,"全网发布");


    private Integer status;
    private String desc;
    public static AppUpdateStatusEnum getAppUpdateStatusEnum(Integer status) {
        for (AppUpdateStatusEnum item : AppUpdateStatusEnum.values()) {
            if (item.status.equals(status)) {
                return item;
            }
        }
        return null;
    }
    AppUpdateStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }
    public String getDesc() {
        return desc;
    }

    public Integer getStatus() {
        return status;
    }
}
