package com.easymeeting.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * MinIO服务接口
 */
public interface MinioService {
    
    /**
     * 上传头像文件
     * @param file 文件
     * @param userId 用户ID
     * @return 文件访问URL
     */
    String uploadAvatar(MultipartFile file, String userId) throws Exception;
    
    /**
     * 从字节数组上传头像
     * @param imageData 图片字节数据
     * @param fileName 文件名
     * @param userId 用户ID
     * @return 文件访问URL
     */
    String uploadAvatarFromBytes(byte[] imageData, String fileName, String userId) throws Exception;
    
    /**
     * 删除头像文件
     * @param fileName 文件名
     */
    void deleteAvatar(String fileName) throws Exception;
    
    /**
     * 获取文件访问URL
     * @param fileName 文件名
     * @return 访问URL
     */
    String getFileUrl(String fileName);
    
    /**
     * 检查并创建桶
     */
    void ensureBucketExists() throws Exception;
}