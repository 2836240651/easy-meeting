package com.easymeeting.entity.query;

import java.util.Date;


/**
 * 参数
 */
public class UserContactApplyQuery extends BaseParam {


	/**
	 * 
	 */
	private Integer applyId;

	/**
	 * 
	 */
	private String applyUserId;

	private String applyUserIdFuzzy;

	/**
	 * 
	 */
	private String receiveUserId;

	private String receiveUserIdFuzzy;

	/**
	 * 
	 */
	private Integer status;

	/**
	 * 
	 */
	private String lastApplyTime;

	private String lastApplyTimeStart;

	private String lastApplyTimeEnd;
	private Boolean queryUserInfo;
	private Boolean queryReceiveUserInfo;  // 查询接收用户信息

	public Boolean getQueryReceiveUserInfo() {
		return queryReceiveUserInfo;
	}

	public void setQueryReceiveUserInfo(Boolean queryReceiveUserInfo) {
		this.queryReceiveUserInfo = queryReceiveUserInfo;
	}

	public Boolean getQueryUserInfo() {
		return queryUserInfo;
	}

	public void setQueryUserInfo(Boolean queryUserInfo) {
		this.queryUserInfo = queryUserInfo;
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

	public void setApplyUserIdFuzzy(String applyUserIdFuzzy){
		this.applyUserIdFuzzy = applyUserIdFuzzy;
	}

	public String getApplyUserIdFuzzy(){
		return this.applyUserIdFuzzy;
	}

	public void setReceiveUserId(String receiveUserId){
		this.receiveUserId = receiveUserId;
	}

	public String getReceiveUserId(){
		return this.receiveUserId;
	}

	public void setReceiveUserIdFuzzy(String receiveUserIdFuzzy){
		this.receiveUserIdFuzzy = receiveUserIdFuzzy;
	}

	public String getReceiveUserIdFuzzy(){
		return this.receiveUserIdFuzzy;
	}

	public void setStatus(Integer status){
		this.status = status;
	}

	public Integer getStatus(){
		return this.status;
	}

	public void setLastApplyTime(String lastApplyTime){
		this.lastApplyTime = lastApplyTime;
	}

	public String getLastApplyTime(){
		return this.lastApplyTime;
	}

	public void setLastApplyTimeStart(String lastApplyTimeStart){
		this.lastApplyTimeStart = lastApplyTimeStart;
	}

	public String getLastApplyTimeStart(){
		return this.lastApplyTimeStart;
	}
	public void setLastApplyTimeEnd(String lastApplyTimeEnd){
		this.lastApplyTimeEnd = lastApplyTimeEnd;
	}

	public String getLastApplyTimeEnd(){
		return this.lastApplyTimeEnd;
	}

}
