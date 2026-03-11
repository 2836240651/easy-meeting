package com.easymeeting.entity.enums;

public enum MessageTypeEnum {
    INIT(0,"连接ws获取信息"),
    ADD_MEETING_ROOM(1,"加入房间"),
    PEER(2,"发送peer"),
    EXIT_MEETING_ROOM(3,"退出会议"),
    FINIS_MEETING(4,"结束会议"),
    CHAT_TEXT_MESSAGE(5,"文本消息"),
    CHAT_MEDIA_MESSAGE(6,"媒体消息"),
    CHAT_MEDIA_MESSAGE_UPDATE(7,"媒体消息更新"),
    USER_CONTACT_APPLY(8,"好友申请消息"),
    INVITE_MEMBER_MEETING(9,"邀请入会"),
    FORCE_OFF_LINE(10,"强制下线"),
    MEETING_USER_VIDEO_CHANGE(11,"用户视频改变"),
    USER_CONTACT_DEAL_WITH(12,"好友申请处理"),
    WEBRTC_OFFER(13,"WebRTC Offer"),
    WEBRTC_ANSWER(14,"WebRTC Answer"),
    WEBRTC_ICE_CANDIDATE(15,"WebRTC ICE候选"),
    SCREEN_SHARE_START(16,"开始屏幕共享"),
    SCREEN_SHARE_STOP(17,"停止屏幕共享"),
    SCREEN_SHARE_OFFER(18,"屏幕共享 Offer"),
    SCREEN_SHARE_ANSWER(19,"屏幕共享 Answer"),
    SCREEN_SHARE_ICE_CANDIDATE(20,"屏幕共享 ICE候选"),
    USER_ONLINE_STATUS_CHANGE(21,"用户在线状态变更"),
    USER_CONTACT_DELETE(22,"好友删除通知"),
    SYSTEM_NOTIFICATION(23,"系统通知");
    private Integer type;
    private String desc;
    private MessageTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }
    public static MessageTypeEnum getByType(Integer type) {
        for (MessageTypeEnum item : MessageTypeEnum.values()) {
            if (item.type.equals(type)) {
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
