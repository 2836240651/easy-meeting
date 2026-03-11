package com.easymeeting.entity.po;

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
public class UserContactApply implements Serializable {


	/**
	 * 
	 */
	private Integer applyId;

	/**
	 * 
	 */
	private String applyUserId;

	/**
	 * 
	 */
	private String receiveUserId;

	/**
	 * 
	 */
	private Integer status;

	/**
	 * 
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date lastApplyTime;
	private String nickName;
	private String avatar;  // 用户头像
	private String email;   // 用户邮箱
	
	public String getAvatar() {
		return avatar;
	}

	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}
	
	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}


	public void setApplyId(Integer applyId){
		this.applyId = applyId;
	}

	public Integer getApplyId(){
		return this.applyId;
	}

	public void setApplyUserId(String applyUserId){
		this.applyUserId = applyUserId;
	}

	public String getApplyUserId(){
		return this.applyUserId;
	}

	public void setReceiveUserId(String receiveUserId){
		this.receiveUserId = receiveUserId;
	}

	public String getReceiveUserId(){
		return this.receiveUserId;
	}

	public void setStatus(Integer status){
		this.status = status;
	}

	public Integer getStatus(){
		return this.status;
	}

	public void setLastApplyTime(Date lastApplyTime){
		this.lastApplyTime = lastApplyTime;
	}

	public Date getLastApplyTime(){
		return this.lastApplyTime;
	}

	@Override
	public String toString (){
		return "applyId:"+(applyId == null ? "空" : applyId)+"，applyUserId:"+(applyUserId == null ? "空" : applyUserId)+"，receiveUserId:"+(receiveUserId == null ? "空" : receiveUserId)+"，status:"+(status == null ? "空" : status)+"，lastApplyTime:"+(lastApplyTime == null ? "空" : DateUtil.format(lastApplyTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()));
	}
}
