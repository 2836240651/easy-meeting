package com.easymeeting.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Base64 工具类
 * 用于处理 base64 编码的图片数据
 */
public class Base64Utils {
    private static final Logger logger = LoggerFactory.getLogger(Base64Utils.class);

    /**
     * 从完整的 data URI 中提取 base64 数据部分
     * 例如: "data:image/png;base64,iVBORw0KGgo..." -> "iVBORw0KGgo..."
     * 
     * @param dataUri 完整的 data URI 字符串
     * @return base64 编码的字符串（不含前缀）
     */
    public static String extractBase64FromDataUri(String dataUri) {
        if (dataUri == null || dataUri.isEmpty()) {
            return null;
        }
        
        // 检查是否包含 data URI 前缀
        if (dataUri.startsWith("data:")) {
            int commaIndex = dataUri.indexOf(',');
            if (commaIndex != -1 && commaIndex < dataUri.length() - 1) {
                return dataUri.substring(commaIndex + 1);
            }
        }
        
        // 如果已经是纯 base64 字符串，直接返回
        return dataUri;
    }

    /**
     * 将 base64 字符串解码并保存为图片文件
     * 
     * @param base64String base64 编码的图片数据（可以是完整的 data URI 或纯 base64 字符串）
     * @param outputPath 输出文件路径
     * @return 是否保存成功
     */
    public static boolean saveBase64ToFile(String base64String, String outputPath) {
        try {
            // 提取 base64 数据部分
            String base64Data = extractBase64FromDataUri(base64String);
            if (base64Data == null || base64Data.isEmpty()) {
                logger.error("Base64 字符串为空");
                return false;
            }

            // 解码 base64 数据
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            // 确保输出目录存在
            File outputFile = new File(outputPath);
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // 写入文件
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(imageBytes);
                fos.flush();
            }

            logger.info("Base64 图片已保存到: {}", outputPath);
            return true;
        } catch (IllegalArgumentException e) {
            logger.error("Base64 解码失败: {}", e.getMessage());
            return false;
        } catch (IOException e) {
            logger.error("保存文件失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 将 base64 字符串解码为字节数组
     * 
     * @param base64String base64 编码的字符串
     * @return 解码后的字节数组
     */
    public static byte[] decodeBase64(String base64String) {
        try {
            String base64Data = extractBase64FromDataUri(base64String);
            if (base64Data == null || base64Data.isEmpty()) {
                return null;
            }
            return Base64.getDecoder().decode(base64Data);
        } catch (IllegalArgumentException e) {
            logger.error("Base64 解码失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 检查字符串是否为有效的 base64 编码
     * 
     * @param base64String 待检查的字符串
     * @return 是否为有效的 base64 编码
     */
    public static boolean isValidBase64(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            return false;
        }
        
        try {
            String base64Data = extractBase64FromDataUri(base64String);
            Base64.getDecoder().decode(base64Data);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}




