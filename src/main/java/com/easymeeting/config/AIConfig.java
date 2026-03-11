package com.easymeeting.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * AI配置类
 */
@Configuration
public class AIConfig {
    
    /**
     * RestTemplate Bean用于调用AI API
     * 配置超时时间避免无限等待
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 连接超时10秒
        factory.setReadTimeout(60000);    // 读取超时60秒
        return new RestTemplate(factory);
    }
}
