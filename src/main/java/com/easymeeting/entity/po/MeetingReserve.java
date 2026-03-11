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
public class MeetingReserve implements Serializable {


	/**
	 * 
	 */
	private String meetingId;

	/**
	 * 
	 */
	private String meetingName;

	/**
	 * 
	 */
	private Integer joinType;

	/**
	 * 
	 */
	private String joinPassword;

	/**
	 * 
	 */
	private Integer duration;

	/**
	 * 
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
	private Date startTime;

	/**
	 * 
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
	private Date createTime;

	/**
	 * 
	 */
	private String createUserId;

	/**
	 * 
	 */
	private Integer status;
	
	/**
	 * 创建者昵称（非数据库字段，用于前端显示）
	 */
	private String nickName;
	
	/**
	 * 被邀请的用户ID列表（非数据库字段，用于前端显示）
	 */
	private String inviteUserIds;

	public String getInviteUserIds() {
		return inviteUserIds;
	}

	public void setInviteUserIds(String inviteUserIds) {
		this.inviteUserIds = inviteUserIds;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public void setMeetingId(String meetingId){
		this.meetingId = meetingId;
	}

	public String getMeetingId(){
		return this.meetingId;
	}

	public void setMeetingName(String meetingName){
		this.meetingName = meetingName;
	}

	public String getMeetingName(){
		return this.meetingName;
	}

	public void setJoinType(Integer joinType){
		this.joinType = joinType;
	}

	public Integer getJoinType(){
		return this.joinType;
	}

	public void setJoinPassword(String joinPassword){
		this.joinPassword = joinPassword;
	}

	public String getJoinPassword(){
		return this.joinPassword;
	}

	public void setDuration(Integer duration){
		this.duration = duration;
	}

	public Integer getDuration(){
		return this.duration;
	}

	public void setStartTime(Date startTime){
		this.startTime = startTime;
	}

	public Date getStartTime(){
		return this.startTime;
	}

	public void setCreateTime(Date createTime){
		this.createTime = createTime;
	}

	public Date getCreateTime(){
		return this.createTime;
	}

	public void setCreateUserId(String createUserId){
		this.createUserId = createUserId;
	}

	public String getCreateUserId(){
		return this.createUserId;
	}

	public void setStatus(Integer status){
		this.status = status;
	}

	public Integer getStatus(){
		return this.status;
	}

	@Override
	public String toString (){
		return "meetingId:"+(meetingId == null ? "空" : meetingId)+"，meetingName:"+(meetingName == null ? "空" : meetingName)+"，joinType:"+(joinType == null ? "空" : joinType)+"，joinPassword:"+(joinPassword == null ? "空" : joinPassword)+"，duration:"+(duration == null ? "空" : duration)+"，startTime:"+(startTime == null ? "空" : DateUtil.format(startTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()))+"，createTime:"+(createTime == null ? "空" : DateUtil.format(createTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()))+"，createUserId:"+(createUserId == null ? "空" : createUserId)+"，status:"+(status == null ? "空" : status);
	}
}
