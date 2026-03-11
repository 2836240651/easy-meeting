package com.easymeeting.entity.enums;


public enum ResponseCodeEnum {
    CODE_200(200, "请求成功"),
    CODE_404(404, "请求地址不存在"),
    CODE_600(600, "请求参数错误"),
    CODE_601(601, "信息已经存在"),
    CODE_603(603,"数据转换失败"),
    CODE_500(500, "服务器返回错误，请联系管理员"),
    CODE_901(901,"登录超时,请重新登录"),
    CODE_902(902,"登录超时,无权限操作"),
    CODE_701(701,"会议已结束"),
    CODE_702(702,"您已加入其他会议"),
    CODE_703(703,"入会密码不正确");
    private Integer code;

    private String msg;

    ResponseCodeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
