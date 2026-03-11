package com.easymeeting.mappers;

import org.apache.ibatis.annotations.Param;

/**
 *  数据库操作接口
 */
public interface MeetingChatMessageMapper<T,P> extends BaseMapperTableSplit<T,P> {

	/**
	 * 根据MessageId更新
	 */
	 Integer updateByMessageId(@Param("tableName") String tableName,@Param("bean") T t,@Param("messageId") Long messageId);


	/**
	 * 根据MessageId删除
	 */
	 Integer deleteByMessageId(@Param("tableName") String tableName,@Param("messageId") Long messageId);


	/**
	 * 根据MessageId获取对象
	 */
	 T selectByMessageId(@Param("tableName") String tableName,@Param("messageId") Long messageId);

	/**
	 * 检查表是否存在
	 */
	Integer checkTableExists(@Param("tableName") String tableName);

	/**
	 * 创建分表
	 */
	void createTable(@Param("tableName") String tableName, @Param("templateTableName") String templateTableName);

	/**
	 * 转换表字符集为 utf8mb4
	 */
	void convertTableCharset(@Param("tableName") String tableName);

}
