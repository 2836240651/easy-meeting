package com.easymeeting.entity.enums;

public enum UserContactApplyStatusEnum {
    INIT(0,"待处理"),
    PASS(1,"已通过"),
    REJECT(2,"拒绝"),
    BLACKLIST(3,"黑名单");
    private Integer status;
    private String desc;
    public static UserContactApplyStatusEnum getByStatus(Integer status) {
        for (UserContactApplyStatusEnum item : UserContactApplyStatusEnum.values()) {
            if (item.status.equals(status)) {
                return item;
            }
        }
        return null;
    }


    UserContactApplyStatusEnum( Integer status,String desc) {
        this.status=status;
        this.desc = desc;
    }
    public String getDesc() {
        return desc;
    }

    public Integer getStatus() {
        return status;
    }
}
