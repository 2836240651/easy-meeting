package com.easymeeting;

import com.alibaba.fastjson.parser.ParserConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

@SpringBootApplication(scanBasePackages = ("com.easymeeting"))
@MapperScan(basePackages = ("com.easymeeting.mappers"))
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
public class EasymeetingApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(EasymeetingApplication.class);

    
    public static void main(String[] args) {
        SpringApplication.run(EasymeetingApplication.class, args);
    }
}
