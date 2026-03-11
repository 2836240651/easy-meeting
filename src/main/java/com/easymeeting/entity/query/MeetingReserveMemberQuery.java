package com.easymeeting.entity.query;



/**
 * 参数
 */
public class MeetingReserveMemberQuery extends BaseParam {


	/**
	 * 
	 */
	private String meetingId;

	private String meetingIdFuzzy;

	/**
	 * 
	 */
	private String inviteUserId;

	private String inviteUserIdFuzzy;
	
	/**
	 * 排除特定用户ID（用于删除时保留创建者）
	 */
	private String inviteUserIdNotEqual;

	public String getInviteUserIdNotEqual() {
		return inviteUserIdNotEqual;
	}

	public void setInviteUserIdNotEqual(String inviteUserIdNotEqual) {
		this.inviteUserIdNotEqual = inviteUserIdNotEqual;
	}


	public void setMeetingId(String meetingId){
		this.meetingId = meetingId;
	}

	public String getMeetingId(){
		return this.meetingId;
	}

	public void setMeetingIdFuzzy(String meetingIdFuzzy){
		this.meetingIdFuzzy = meetingIdFuzzy;
	}

	public String getMeetingIdFuzzy(){
		return this.meetingIdFuzzy;
	}

	public void setInviteUserId(String inviteUserId){
		this.inviteUserId = inviteUserId;
	}

	public String getInviteUserId(){
		return this.inviteUserId;
	}

	public void setInviteUserIdFuzzy(String inviteUserIdFuzzy){
		this.inviteUserIdFuzzy = inviteUserIdFuzzy;
	}

	public String getInviteUserIdFuzzy(){
		return this.inviteUserIdFuzzy;
	}

}
