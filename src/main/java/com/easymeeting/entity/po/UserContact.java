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
public class UserContact implements Serializable {


	/**
	 * 
	 */
	private String userId;

	/**
	 * 联系人id
	 */
	private String contactId;

	/**
	 * 
	 */
	private Integer status;

	/**
	 * 
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date lastUpdateTime;
	private String nickName;

	private Long lastLoginTime;
	private Long lastOffTime;
	private Integer onlineStatus;
	private String avatar;

	public Integer getOnlineStatus() {
		if (onlineStatus != null) {
			return onlineStatus;
		}
		if (lastLoginTime == null) {
			return Constants.ZERO;
		}
		if (lastOffTime == null) {
			return Constants.ONE;
		}
		return lastLoginTime > lastOffTime ? Constants.ONE : Constants.ZERO;
	}

	public void setOnlineStatus(Integer onlineStatus) {
		this.onlineStatus = onlineStatus;
	}

	public Long getLastLoginTime() {
		return lastLoginTime;
	}

	public void setLastLoginTime(Long lastLoginTime) {
		this.lastLoginTime = lastLoginTime;
	}

	public Long getLastOffTime() {
		return lastOffTime;
	}

	public void setLastOffTime(Long lastOffTime) {
		this.lastOffTime = lastOffTime;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public String getAvatar() {
		return avatar;
	}

	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}

	public void setUserId(String userId){
		this.userId = userId;
	}

	public String getUserId(){
		return this.userId;
	}

	public void setContactId(String contactId){
		this.contactId = contactId;
	}

	public String getContactId(){
		return this.contactId;
	}

	public void setStatus(Integer status){
		this.status = status;
	}

	public Integer getStatus(){
		return this.status;
	}

	public void setLastUpdateTime(Date lastUpdateTime){
		this.lastUpdateTime = lastUpdateTime;
	}

	public Date getLastUpdateTime(){
		return this.lastUpdateTime;
	}

	@Override
	public String toString (){
		return "userId:"+(userId == null ? "空" : userId)+"，联系人id:"+(contactId == null ? "空" : contactId)+"，status:"+(status == null ? "空" : status)+"，lastUpdateTime:"+(lastUpdateTime == null ? "空" : DateUtil.format(lastUpdateTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()));
	}
}
