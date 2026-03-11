package com.easymeeting.entity.enums;


public enum UserStatusEnum {
disable(0,"禁用"),
enable(1,"启用");
private Integer status;
private String desc;
UserStatusEnum(Integer status, String desc) {
    this.status = status;
    this.desc = desc;
}
public static UserStatusEnum getUserStatusEnum(Integer status) {
    for(UserStatusEnum item : UserStatusEnum.values()) {
        if(item.getStatus().equals(status) ) {
            return item;
        }
    }
    return null;
}



    public String getDesc() {
        return desc;
    }

    public Integer getStatus() {
        return status;
    }
}
