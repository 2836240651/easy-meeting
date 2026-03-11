package com.easymeeting.entity.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import com.easymeeting.entity.enums.DateTimePatternEnum;
import com.easymeeting.utils.DateUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.StringUtils;
import org.omg.CORBA.PRIVATE_MEMBER;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;


/**
 * 
 */
public class AppUpdate implements Serializable {


	/**
	 * 
	 */
	private Integer id;

	/**
	 * 
	 */
	private String version;

	/**
	 * 
	 */
	private String updateDesc;

	/**
	 * 
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime;

	/**
	 * 
	 */
	private Integer status;

	/**
	 * 
	 */
	private String grayscaleId;

	/**
	 * 
	 */
	private Integer fileType;

	/**
	 * 
	 */
	private String outerLink;
	private String[] descSplit;
	public String[] getDescSplit() {
	if (!StringUtils.isEmpty(updateDesc)){
		return updateDesc.split("\\|");
	}
	return descSplit;
	}

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

	public void setUpdateDesc(String updateDesc){
		this.updateDesc = updateDesc;
	}

	public String getUpdateDesc(){
		return this.updateDesc;
	}

	public void setCreateTime(Date createTime){
		this.createTime = createTime;
	}

	public Date getCreateTime(){
		return this.createTime;
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

	@Override
	public String toString (){
		return "id:"+(id == null ? "空" : id)+"，version:"+(version == null ? "空" : version)+"，updateDesc:"+(updateDesc == null ? "空" : updateDesc)+"，createTime:"+(createTime == null ? "空" : DateUtil.format(createTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()))+"，status:"+(status == null ? "空" : status)+"，grayscaleId:"+(grayscaleId == null ? "空" : grayscaleId)+"，fileType:"+(fileType == null ? "空" : fileType)+"，outerLink:"+(outerLink == null ? "空" : outerLink);
	}
}
