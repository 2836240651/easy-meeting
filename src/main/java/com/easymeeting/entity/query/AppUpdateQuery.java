package com.easymeeting.entity.query;

import java.util.Date;


/**
 * 参数
 */
public class AppUpdateQuery extends BaseParam {


	/**
	 * 
	 */
	private Integer id;

	/**
	 * 
	 */
	private String version;

	private String versionFuzzy;

	/**
	 * 
	 */
	private String updateDesc;

	private String updateDescFuzzy;

	/**
	 * 
	 */
	private String createTime;

	private String createTimeStart;

	private String createTimeEnd;

	/**
	 * 
	 */
	private Integer status;

	/**
	 * 
	 */
	private String grayscaleId;

	private String grayscaleIdFuzzy;

	/**
	 * 
	 */
	private Integer fileType;

	/**
	 * 
	 */
	private String outerLink;

	private String outerLinkFuzzy;


	public void setId(Integer id){
		this.id = id;
	}

	public Integer getId(){
		return this.id;
	}

	public void setVersion(String version){
		this.version = version;
	}

	public String getVersion(){
		return this.version;
	}

	public void setVersionFuzzy(String versionFuzzy){
		this.versionFuzzy = versionFuzzy;
	}

	public String getVersionFuzzy(){
		return this.versionFuzzy;
	}

	public void setUpdateDesc(String updateDesc){
		this.updateDesc = updateDesc;
	}

	public String getUpdateDesc(){
		return this.updateDesc;
	}

	public void setUpdateDescFuzzy(String updateDescFuzzy){
		this.updateDescFuzzy = updateDescFuzzy;
	}

	public String getUpdateDescFuzzy(){
		return this.updateDescFuzzy;
	}

	public void setCreateTime(String createTime){
		this.createTime = createTime;
	}

	public String getCreateTime(){
		return this.createTime;
	}

	public void setCreateTimeStart(String createTimeStart){
		this.createTimeStart = createTimeStart;
	}

	public String getCreateTimeStart(){
		return this.createTimeStart;
	}
	public void setCreateTimeEnd(String createTimeEnd){
		this.createTimeEnd = createTimeEnd;
	}

	public String getCreateTimeEnd(){
		return this.createTimeEnd;
	}

	public void setStatus(Integer status){
		this.status = status;
	}

	public Integer getStatus(){
		return this.status;
	}

	public void setGrayscaleId(String grayscaleId){
		this.grayscaleId = grayscaleId;
	}

	public String getGrayscaleId(){
		return this.grayscaleId;
	}

	public void setGrayscaleIdFuzzy(String grayscaleIdFuzzy){
		this.grayscaleIdFuzzy = grayscaleIdFuzzy;
	}

	public String getGrayscaleIdFuzzy(){
		return this.grayscaleIdFuzzy;
	}

	public void setFileType(Integer fileType){
		this.fileType = fileType;
	}

	public Integer getFileType(){
		return this.fileType;
	}

	public void setOuterLink(String outerLink){
		this.outerLink = outerLink;
	}

	public String getOuterLink(){
		return this.outerLink;
	}

	public void setOuterLinkFuzzy(String outerLinkFuzzy){
		this.outerLinkFuzzy = outerLinkFuzzy;
	}

	public String getOuterLinkFuzzy(){
		return this.outerLinkFuzzy;
	}

}
