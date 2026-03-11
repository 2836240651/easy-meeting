package com.easymeeting.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.easymeeting.entity.enums.ResponseCodeEnum;
import com.easymeeting.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class JsonUtils {
    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    public static SerializerFeature[] FEATURES = new SerializerFeature[]{SerializerFeature.WriteMapNullValue};

    static {
        // 配置 FastJSON 解析器，禁用 AutoType 功能，避免使用 ScriptEngine
        // 这样可以防止 FastJSON 尝试使用 JavaScript 引擎来解析某些 JSON 特性
        ParserConfig.getGlobalInstance().setAutoTypeSupport(false);
    }

    public static String convertObj2Json(Object obj) {
        return JSON.toJSONString(obj, FEATURES);
    }

    public static <T> T convertJson2Obj(String json, Class<T> clazz) {
        try{
            // 使用 JSON.parseObject 代替 JSONObject.parseObject，可以更好地控制解析过程
            // 并且避免触发 ScriptEngine 的使用
            return JSON.parseObject(json, clazz);
        }catch (Exception e){
            logger.error("convertJson2Obj,json：{}",json, e);
            throw new BusinessException(ResponseCodeEnum.CODE_603);
        }
    }
    public static <T> List<T> convertJsonArray2List(String json, Class<T> clazz) {
        try{
            // 使用 JSON.parseArray 代替 JSONArray.parseArray，可以更好地控制解析过程
            // 并且避免触发 ScriptEngine 的使用
            return JSON.parseArray(json, clazz);
        }catch (Exception e){
            logger.error("convertJsonArray2List,json:{}",json,e);
            throw new BusinessException(ResponseCodeEnum.CODE_603);
        }
    }






}
