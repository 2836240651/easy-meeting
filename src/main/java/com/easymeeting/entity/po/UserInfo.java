package com.easymeeting.entity.po;

import com.easymeeting.entity.constants.Constants;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import com.easymeeting.entity.enums.DateTimePatternEnum;
import com.easymeeting.utils.DateUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;


/**
 * 
 */
public class UserInfo implements Serializable {


	/**
	 * uid
	 */
	private String userId;

	/**
	 * 性别 0:女 1:男 2:保密
	 */
	private Integer sex;

	/**
	 * 邮箱
	 */
	private String email;

	/**
	 * md5密码
	 */
	private String password;

	/**
	 * 昵称
	 */
	private String nickName;

	/**
	 * 0：禁用 1：启用
	 */
	private Integer status;

	/**
	 * 创建时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime;

	/**
	 * 最近登录时间
	 */
	private Long lastLoginTime;

	/**
	 * 最近离线时间
	 */
	private Long lasgOffTime;

	/**
	 * 个人会议号
	 */
	private String meetingNo;

	/**
	 * 头像URL
	 */
	private String avatar;

	private Integer onlineType;

	public Integer getOnlineType() {
		if (lastLoginTime != null && lasgOffTime != null && lastLoginTime > lasgOffTime) {
			return Constants.ONE;
		} else {
			return Constants.ZERO;
		}
	}

	public void setOnlineType(Integer onlineType) {
		this.onlineType = onlineType;
	}

	public void setUserId(String userId){
		this.userId = userId;
	}

	public String getUserId(){
		return this.userId;
	}

	public void setSex(Integer sex){
		this.sex = sex;
	}

	public Integer getSex(){
		return this.sex;
	}

	public void setEmail(String email){
		this.email = email;
	}

	public String getEmail(){
		return this.email;
	}

	public void setPassword(String password){
		this.password = password;
	}

	public String getPassword(){
		return this.password;
	}

	public void setNickName(String nickName){
		this.nickName = nickName;
	}

	public String getNickName(){
		return this.nickName;
	}

	public void setStatus(Integer status){
		this.status = status;
	}

	public Integer getStatus(){
		return this.status;
	}

	public void setCreateTime(Date createTime){
		this.createTime = createTime;
	}

	public Date getCreateTime(){
		return this.createTime;
	}

	public void setLastLoginTime(Long lastLoginTime){
		this.lastLoginTime = lastLoginTime;
	}

	public Long getLastLoginTime(){
		return this.lastLoginTime;
	}

	public void setLasgOffTime(Long lasgOffTime){
		this.lasgOffTime = lasgOffTime;
	}

	public Long getLasgOffTime(){
		return this.lasgOffTime;
	}

	public void setMeetingNo(String meetingNo){
		this.meetingNo = meetingNo;
	}

	public String getMeetingNo(){
		return this.meetingNo;
	}

	public void setAvatar(String avatar){
		this.avatar = avatar;
	}

	public String getAvatar(){
		return this.avatar;
	}

	@Override
	public String toString (){
		return "uid:"+(userId == null ? "空" : userId)+"，性别 0:女 1:男 2:保密:"+(sex == null ? "空" : sex)+"，邮箱:"+(email == null ? "空" : email)+"，md5密码:"+(password == null ? "空" : password)+"，昵称:"+(nickName == null ? "空" : nickName)+"，0：禁用 1：启用:"+(status == null ? "空" : status)+"，创建时间:"+(createTime == null ? "空" : DateUtil.format(createTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()))+"，最近登录时间:"+(lastLoginTime == null ? "空" : lastLoginTime)+"，最近离线时间:"+(lasgOffTime == null ? "空" : lasgOffTime)+"，个人会议号:"+(meetingNo == null ? "空" : meetingNo)+"，头像URL:"+(avatar == null ? "空" : avatar);
	}
}
