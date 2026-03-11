package com.easymeeting.controller;

import com.easymeeting.annotation.globalInterceptor;
import com.easymeeting.entity.dto.TokenUserInfoDto;
import com.easymeeting.entity.vo.ResponseVO;
import com.easymeeting.service.MinioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * 文件上传控制器
 */
@RestController("fileUploadController")
@RequestMapping("/upload")
public class FileUploadController extends ABaseController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);
    
    // 支持的图片格式
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    
    // 最大文件大小 (2MB)
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;

    @Autowired
    private MinioService minioService;

    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    @globalInterceptor
    public ResponseVO uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo();
            String userId = tokenUserInfoDto.getUserId();

            // 验证文件
            String validationError = validateFile(file);
            if (validationError != null) {
                return getFailResponseVO(validationError);
            }

            // 上传到MinIO
            String avatarUrl = minioService.uploadAvatar(file, userId);
            
            logger.info("用户 {} 上传头像成功，URL: {}", userId, avatarUrl);
            return getSuccessResponseVO(avatarUrl);

        } catch (Exception e) {
            logger.error("头像上传失败", e);
            return getFailResponseVO("头像上传失败: " + e.getMessage());
        }
    }

    /**
     * 通过URL上传头像
     */
    @PostMapping("/avatarByUrl")
    @globalInterceptor
    public ResponseVO uploadAvatarByUrl(@RequestParam("url") String imageUrl, HttpServletRequest request) {
        try {
            TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo();
            String userId = tokenUserInfoDto.getUserId();

            logger.info("用户 {} 开始通过URL上传头像: {}", userId, imageUrl);

            // 验证URL格式
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                return getFailResponseVO("图片URL不能为空");
            }

            if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                return getFailResponseVO("请提供有效的HTTP或HTTPS图片链接");
            }

            // 下载图片
            byte[] imageData = downloadImageFromUrl(imageUrl);
            if (imageData == null || imageData.length == 0) {
                return getFailResponseVO("无法下载图片，请检查URL是否正确");
            }

            // 检查文件大小
            if (imageData.length > MAX_FILE_SIZE) {
                return getFailResponseVO("图片文件大小不能超过2MB");
            }

            // 获取文件扩展名
            String extension = getExtensionFromUrl(imageUrl);
            if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
                // 尝试从Content-Type判断
                extension = detectImageFormat(imageData);
                if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
                    return getFailResponseVO("不支持的图片格式，只支持 " + String.join(", ", ALLOWED_EXTENSIONS) + " 格式");
                }
            }

            // 创建临时文件名
            String fileName = "avatar_" + userId + "_" + System.currentTimeMillis() + "." + extension;

            // 上传到MinIO
            String avatarUrl = minioService.uploadAvatarFromBytes(imageData, fileName, userId);
            
            logger.info("用户 {} 通过URL上传头像成功，新URL: {}", userId, avatarUrl);
            return getSuccessResponseVO(avatarUrl);

        } catch (Exception e) {
            logger.error("通过URL上传头像失败", e);
            return getFailResponseVO("上传头像失败: " + e.getMessage());
        }
    }

    /**
     * 验证上传文件
     */
    private String validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "请选择要上传的文件";
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return "文件大小不能超过2MB";
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return "文件名不能为空";
        }

        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            return "只支持 " + String.join(", ", ALLOWED_EXTENSIONS) + " 格式的图片";
        }

        return null;
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
     * 从URL获取文件扩展名
     */
    private String getExtensionFromUrl(String url) {
        try {
            // 移除查询参数
            if (url.contains("?")) {
                url = url.substring(0, url.indexOf("?"));
            }
            
            if (url.contains(".")) {
                String extension = url.substring(url.lastIndexOf(".") + 1).toLowerCase();
                return ALLOWED_EXTENSIONS.contains(extension) ? extension : null;
            }
        } catch (Exception e) {
            logger.warn("无法从URL获取扩展名: {}", url);
        }
        return null;
    }

    /**
     * 从URL下载图片
     */
    private byte[] downloadImageFromUrl(String imageUrl) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        
        try {
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();
            
            // 设置请求属性 - 模拟浏览器请求
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000); // 15秒连接超时
            connection.setReadTimeout(30000);    // 30秒读取超时
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            connection.setRequestProperty("Accept", "image/webp,image/apng,image/*,*/*;q=0.8");
            connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setRequestProperty("Pragma", "no-cache");
            
            // 允许重定向
            connection.setInstanceFollowRedirects(true);
            
            // 检查响应码
            int responseCode = connection.getResponseCode();
            logger.info("下载图片响应码: {}, URL: {}", responseCode, imageUrl);
            
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || 
                responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                // 处理重定向
                String newUrl = connection.getHeaderField("Location");
                logger.info("图片URL重定向到: {}", newUrl);
                connection.disconnect();
                return downloadImageFromUrl(newUrl);
            }
            
            if (responseCode != HttpURLConnection.HTTP_OK) {
                logger.warn("下载图片失败，HTTP响应码: {}, URL: {}", responseCode, imageUrl);
                return null;
            }
            
            // 检查Content-Type（放宽限制）
            String contentType = connection.getContentType();
            logger.info("图片Content-Type: {}, URL: {}", contentType, imageUrl);
            
            // 检查Content-Length
            long contentLength = connection.getContentLengthLong();
            if (contentLength > 0 && contentLength > MAX_FILE_SIZE) {
                logger.warn("图片文件过大: {} bytes, URL: {}", contentLength, imageUrl);
                return null;
            }
            
            // 读取数据
            inputStream = connection.getInputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                
                // 检查已读取的数据大小
                if (outputStream.size() > MAX_FILE_SIZE) {
                    logger.warn("图片文件过大，已读取: {} bytes, URL: {}", outputStream.size(), imageUrl);
                    return null;
                }
            }
            
            byte[] imageData = outputStream.toByteArray();
            logger.info("成功下载图片，大小: {} bytes, URL: {}", imageData.length, imageUrl);
            return imageData;
            
        } catch (Exception e) {
            logger.error("下载图片失败，URL: {}", imageUrl, e);
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.warn("关闭输入流失败", e);
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 检测图片格式
     */
    private String detectImageFormat(byte[] imageData) {
        if (imageData == null || imageData.length < 4) {
            return null;
        }
        
        // 检查文件头来判断图片格式
        if (imageData.length >= 2) {
            // JPEG
            if ((imageData[0] & 0xFF) == 0xFF && (imageData[1] & 0xFF) == 0xD8) {
                return "jpg";
            }
        }
        
        if (imageData.length >= 8) {
            // PNG
            if ((imageData[0] & 0xFF) == 0x89 && imageData[1] == 0x50 && 
                imageData[2] == 0x4E && imageData[3] == 0x47) {
                return "png";
            }
        }
        
        if (imageData.length >= 6) {
            // GIF
            if (imageData[0] == 0x47 && imageData[1] == 0x49 && imageData[2] == 0x46) {
                return "gif";
            }
        }
        
        if (imageData.length >= 12) {
            // WebP
            if (imageData[0] == 0x52 && imageData[1] == 0x49 && imageData[2] == 0x46 && imageData[3] == 0x46 &&
                imageData[8] == 0x57 && imageData[9] == 0x45 && imageData[10] == 0x42 && imageData[11] == 0x50) {
                return "webp";
            }
        }
        
        return null;
    }
}