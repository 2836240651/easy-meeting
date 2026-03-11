package com.easymeeting.service.impl;

import com.easymeeting.service.MinioService;
import io.minio.*;
import io.minio.errors.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * MinIO服务实现类
 */
@Service
public class MinioServiceImpl implements MinioService {

    private static final Logger logger = LoggerFactory.getLogger(MinioServiceImpl.class);

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    /**
     * 初始化时检查并创建桶
     */
    @PostConstruct
    public void init() {
        try {
            ensureBucketExists();
            logger.info("MinIO服务初始化成功，桶名: {}", bucketName);
        } catch (Exception e) {
            logger.error("MinIO服务初始化失败", e);
        }
    }

    @Override
    public String uploadAvatar(MultipartFile file, String userId) throws Exception {
        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String fileName = "avatar_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;

        InputStream inputStream = null;
        try {
            // 获取输入流
            inputStream = file.getInputStream();
            
            // 上传文件到MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            logger.info("用户 {} 头像上传到MinIO成功: {}", userId, fileName);
            return getFileUrl(fileName);

        } catch (Exception e) {
            logger.error("上传文件到MinIO失败", e);
            throw new Exception("上传文件失败: " + e.getMessage());
        } finally {
            // 确保输入流被关闭
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.warn("关闭输入流失败", e);
                }
            }
        }
    }

    @Override
    public String uploadAvatarFromBytes(byte[] imageData, String fileName, String userId) throws Exception {
        ByteArrayInputStream inputStream = null;
        try {
            // 根据文件扩展名确定Content-Type
            String contentType = getContentTypeFromExtension(getFileExtension(fileName));
            
            // 使用ByteArrayInputStream上传
            inputStream = new ByteArrayInputStream(imageData);
            
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(inputStream, imageData.length, -1)
                            .contentType(contentType)
                            .build()
            );

            logger.info("用户 {} 从字节数组上传头像到MinIO成功: {}", userId, fileName);
            return getFileUrl(fileName);

        } catch (Exception e) {
            logger.error("从字节数组上传文件到MinIO失败", e);
            throw new Exception("上传文件失败: " + e.getMessage());
        } finally {
            // 确保输入流被关闭
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.warn("关闭ByteArrayInputStream失败", e);
                }
            }
        }
    }

    @Override
    public void deleteAvatar(String fileName) throws Exception {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
            logger.info("从MinIO删除文件成功: {}", fileName);
        } catch (Exception e) {
            logger.error("从MinIO删除文件失败: {}", fileName, e);
            throw new Exception("删除文件失败: " + e.getMessage());
        }
    }

    @Override
    public String getFileUrl(String fileName) {
        // 返回MinIO的公共访问URL
        return endpoint + "/" + bucketName + "/" + fileName;
    }

    @Override
    public void ensureBucketExists() throws Exception {
        try {
            // 检查桶是否存在
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            if (!exists) {
                // 创建桶
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );

                // 设置桶策略为公共读取
                String policy = "{\n" +
                        "  \"Version\": \"2012-10-17\",\n" +
                        "  \"Statement\": [\n" +
                        "    {\n" +
                        "      \"Effect\": \"Allow\",\n" +
                        "      \"Principal\": \"*\",\n" +
                        "      \"Action\": \"s3:GetObject\",\n" +
                        "      \"Resource\": \"arn:aws:s3:::" + bucketName + "/*\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";

                minioClient.setBucketPolicy(
                        SetBucketPolicyArgs.builder()
                                .bucket(bucketName)
                                .config(policy)
                                .build()
                );

                logger.info("创建MinIO桶成功: {}", bucketName);
            } else {
                logger.info("MinIO桶已存在: {}", bucketName);
            }
        } catch (Exception e) {
            logger.error("检查或创建MinIO桶失败", e);
            throw new Exception("MinIO桶操作失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * 根据文件扩展名获取Content-Type
     */
    private String getContentTypeFromExtension(String extension) {
        if (extension == null) {
            return "application/octet-stream";
        }
        
        switch (extension.toLowerCase()) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            default:
                return "application/octet-stream";
        }
    }
}