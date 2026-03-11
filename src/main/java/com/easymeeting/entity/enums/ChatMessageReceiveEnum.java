package com.easymeeting.entity.enums;

public enum ChatMessageReceiveEnum {
    ALL(0,"全员"),
    USER(1,"个人");
    private Integer type;
    private String desc;
    public  static ChatMessageReceiveEnum getByType(Integer type) {
        for (ChatMessageReceiveEnum item : ChatMessageReceiveEnum.values()) {
            if (item.getType().equals(type)) {
                return item;
            }
        }
    return null;
    }


    ChatMessageReceiveEnum(Integer type, String desc) {
        this.type=type;
        this.desc=desc;
    }
    public Integer getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }
}
