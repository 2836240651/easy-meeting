package com.easymeeting.entity.enums;

public enum MemberTypeEnum {
    NORAML(0,"普通用户"),
    COMPERE(1,"主持人");
    private Integer type;
    private String desc;
    private MemberTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }
    public MemberTypeEnum getByType(Integer type) {
        for (MemberTypeEnum item : MemberTypeEnum.values()) {
            if (item.getType().equals(type)) {
                return item;
            }

        }
        return null;
        
    }
    public Integer getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }
}
