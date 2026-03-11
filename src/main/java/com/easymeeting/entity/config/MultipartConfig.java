package com.easymeeting.entity.config;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import javax.servlet.MultipartConfigElement;
import java.io.File;

/**
 * 文件上传配置
 */
@Configuration
public class MultipartConfig {

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        
        // 设置文件大小限制
        factory.setMaxFileSize(DataSize.ofMegabytes(15));
        factory.setMaxRequestSize(DataSize.ofMegabytes(15));
        
        // 设置临时文件存储位置
        String tempDir = System.getProperty("java.io.tmpdir");
        File uploadTempDir = new File(tempDir, "easymeeting-uploads");
        if (!uploadTempDir.exists()) {
            uploadTempDir.mkdirs();
        }
        factory.setLocation(uploadTempDir.getAbsolutePath());
        
        return factory.createMultipartConfig();
    }
}