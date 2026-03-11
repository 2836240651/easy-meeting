package com.easymeeting.annotation;

import org.apache.ibatis.annotations.Mapper;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Mapper
@Documented
public @interface globalInterceptor {
boolean checkLogin() default true;
boolean checkAdmin() default false;

}
