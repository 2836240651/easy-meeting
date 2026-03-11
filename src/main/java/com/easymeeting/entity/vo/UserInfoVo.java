package com.easymeeting.entity.vo;

import org.omg.CORBA.PRIVATE_MEMBER;

import java.io.Serializable;

public class UserInfoVo implements Serializable {
    /**
     * uid
     */
    private String userId;

    /**
     * 性别 0:女 1:男 2:保密
     */
    private Integer sex;

    /**
     * 昵称
     */
    private String nickName;
    /*
    * token
    * */
    private String token;
    /*
    * meetingNo
    * */
    private String meetingNo;
    /* admin
    * */
    private Boolean admin;


    private Boolean success;

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Boolean getAdmin() {
        return admin;
    }

    public void setAdmin(Boolean admin) {
        this.admin = admin;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getSex() {
        return sex;
    }

    public void setSex(Integer sex) {
        this.sex = sex;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMeetingNo() {
        return meetingNo;
    }

    public void setMeetingNo(String meetingNo) {
        this.meetingNo = meetingNo;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }
}
