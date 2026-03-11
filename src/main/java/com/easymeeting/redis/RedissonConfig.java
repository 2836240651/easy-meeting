package com.easymeeting.redis;

import com.easymeeting.entity.constants.Constants;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
@Slf4j
@Component
@ConditionalOnProperty(name = Constants.MESSAGEING_HANDLE_CHANNEL_KEY,havingValue = Constants.MESSAGEING_HANDLE_CHANNEL_REDIS)
public class RedissonConfig {
@Value("${spring.redis.host}")
private String host;
@Value("${spring.redis.port}")
private int port;

@Bean(name = "redissonClient",destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
    try {
        Config config = new org.redisson.config.Config();
        config.useSingleServer().setAddress("redis://"+host + ":" + port);
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }catch (
            Exception e
    ){  log.error("redis配置失败");}

return null;
}


}
