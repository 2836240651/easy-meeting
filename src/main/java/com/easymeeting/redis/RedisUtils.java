package com.easymeeting.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.sql.Time;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component("redisUtils")
public class RedisUtils<v> {
    @Resource
    private RedisTemplate<String,v> redisTemplate;
    private static final Logger logger = LoggerFactory.getLogger(RedisUtils.class) ;
        /*
        删除缓存
        可以传一个值 或 多个值
         */
    public void delete(String... key){
        if (key != null && key.length>0)
        {if (key.length == 1)
        {redisTemplate.delete(key[0]);
    } else {redisTemplate.delete((Collection<String>) CollectionUtils.arrayToList (key));
    }
        }
    }
    public v get(String key){
        return key ==null ? null : redisTemplate.opsForValue().get(key);

    }

    public boolean set(String key, v value){
        try{
            redisTemplate.opsForValue().set(key, value);
            return true;
        }catch (Exception e){
            logger.error("设置redisKey:{},value:{}失败",key,value);
            return false;
        }


    }
    public boolean setEx(String key,v value,Long time){
    try{
        if (time>0){

            redisTemplate.opsForValue().set(key,value,time, TimeUnit.SECONDS);
        }else {
            set(key,value);
        }
        return true;
    }catch (Exception e){}
    logger.error("设置redisKey:{},value:{}失败",key,value);
    return false;
    }
    public void hset(String key, String hashKey, v value){
        redisTemplate.opsForHash().put(key,hashKey,value);
    }
    public v hget(String key, String hashKey){
        return (v) redisTemplate.opsForHash().get(key,hashKey);
    }
    public void hdel(String key, String... hashKey){
        redisTemplate.opsForHash().delete(key,hashKey);
    }
    public List<v> hvals(String key){
        return (List<v>) redisTemplate.opsForHash().values(key);
    }

    }