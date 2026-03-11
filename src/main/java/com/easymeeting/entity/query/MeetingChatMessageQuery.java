package com.easymeeting.entity.query;



/**
 * 参数
 */
public class MeetingChatMessageQuery extends BaseParam {


	/**
	 * 雪花算法meesageId
	 *
	 */
	private String userId;
	private Long messageId;

	/**
	 * 
	 */
	private String meetingId;

	private String meetingIdFuzzy;

	/**
	 * 
	 */
	private Integer messageType;

	/**
	 * 
	 */
	private String messageContent;

	private String messageContentFuzzy;

	/**
	 * 
	 */
	private String sendUserId;

	private String sendUserIdFuzzy;

	/**
	 * 
	 */
	private String sendUserNickName;

	private String sendUserNickNameFuzzy;

	/**
	 * 
	 */
	private Long sendTime;

	/**
	 * 
	 */
	private Integer receiveType;

	/**
	 * 
	 */
	private String receiveUserId;

	private String receiveUserIdFuzzy;

	/**
	 * 
	 */
	private Long fileSize;

	/**
	 * 
	 */
	private String fileName;

	private String fileNameFuzzy;

	/**
	 * 
	 */
	private Integer fileType;

	/**
	 * 
	 */
	private String fileSuffix;

	private String fileSuffixFuzzy;

	/**
	 * 
	 */
	private Integer status;

	private Long maxMessage;
	private Long minMessage;

	/**
	 * 是否查询用户信息（头像和昵称）
	 */
	private Boolean queryUserInfo;

	public Long getMaxMessage() {
		return maxMessage;
	}

	public void setMaxMessage(Long maxMessage) {
		this.maxMessage = maxMessage;
	}

	public Long getMinMessage() {
		return minMessage;
	}

	public void setMinMessage(Long minMessage) {
		this.minMessage = minMessage;
	}

	public void setMessageId(Long messageId){
		this.messageId = messageId;
	}

	public Long getMessageId(){
		return this.messageId;
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

	public void setMessageType(Integer messageType){
		this.messageType = messageType;
	}

	public Integer getMessageType(){
		return this.messageType;
	}

	public void setMessageContent(String messageContent){
		this.messageContent = messageContent;
	}

	public String getMessageContent(){
		return this.messageContent;
	}

	public void setMessageContentFuzzy(String messageContentFuzzy){
		this.messageContentFuzzy = messageContentFuzzy;
	}

	public String getMessageContentFuzzy(){
		return this.messageContentFuzzy;
	}

	public void setSendUserId(String sendUserId){
		this.sendUserId = sendUserId;
	}

	public String getSendUserId(){
		return this.sendUserId;
	}

	public void setSendUserIdFuzzy(String sendUserIdFuzzy){
		this.sendUserIdFuzzy = sendUserIdFuzzy;
	}

	public String getSendUserIdFuzzy(){
		return this.sendUserIdFuzzy;
	}

	public void setSendUserNickName(String sendUserNickName){
		this.sendUserNickName = sendUserNickName;
	}

	public String getSendUserNickName(){
		return this.sendUserNickName;
	}

	public void setSendUserNickNameFuzzy(String sendUserNickNameFuzzy){
		this.sendUserNickNameFuzzy = sendUserNickNameFuzzy;
	}

	public String getSendUserNickNameFuzzy(){
		return this.sendUserNickNameFuzzy;
	}

	public void setSendTime(Long sendTime){
		this.sendTime = sendTime;
	}

	public Long getSendTime(){
		return this.sendTime;
	}

	public void setReceiveType(Integer receiveType){
		this.receiveType = receiveType;
	}

	public Integer getReceiveType(){
		return this.receiveType;
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

	public void setFileSize(Long fileSize){
		this.fileSize = fileSize;
	}

	public Long getFileSize(){
		return this.fileSize;
	}

	public void setFileName(String fileName){
		this.fileName = fileName;
	}

	public String getFileName(){
		return this.fileName;
	}

	public void setFileNameFuzzy(String fileNameFuzzy){
		this.fileNameFuzzy = fileNameFuzzy;
	}

	public String getFileNameFuzzy(){
		return this.fileNameFuzzy;
	}

	public void setFileType(Integer fileType){
		this.fileType = fileType;
	}

	public Integer getFileType(){
		return this.fileType;
	}

	public void setFileSuffix(String fileSuffix){
		this.fileSuffix = fileSuffix;
	}

	public String getFileSuffix(){
		return this.fileSuffix;
	}

	public void setFileSuffixFuzzy(String fileSuffixFuzzy){
		this.fileSuffixFuzzy = fileSuffixFuzzy;
	}

	public String getFileSuffixFuzzy(){
		return this.fileSuffixFuzzy;
	}

	public void setStatus(Integer status){
		this.status = status;
	}

	public Integer getStatus(){
		return this.status;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUserId() {
		return userId;
	}

	public void setQueryUserInfo(Boolean queryUserInfo) {
		this.queryUserInfo = queryUserInfo;
	}

	public Boolean getQueryUserInfo() {
		return queryUserInfo;
	}
}
