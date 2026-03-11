package com.easymeeting.aspect;

import com.easymeeting.annotation.globalInterceptor;
import com.easymeeting.entity.dto.TokenUserInfoDto;
import com.easymeeting.entity.enums.ResponseCodeEnum;
import com.easymeeting.exception.BusinessException;
import com.easymeeting.redis.RedisComponent;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
public class globalOperationAspect {
    @Resource
    private RedisComponent redisComponent;

    public globalOperationAspect(RedisComponent redisComponent) {
        this.redisComponent = redisComponent;
    }

    //定义切点
    @Before("@annotation(com.easymeeting.annotation.globalInterceptor)")
    public void interceptorDo(JoinPoint point) {
      try{
          Method method = ((MethodSignature)point.getSignature()).getMethod();
          globalInterceptor Interceptor = method.getAnnotation(globalInterceptor.class);
          if (Interceptor == null) {
              return;
          }
          if (Interceptor.checkAdmin()|| Interceptor.checkLogin()){
              checkLogin(Interceptor.checkAdmin());
          }
      }catch (BusinessException e) {
          log.error("全局拦截器异常");
         throw e;
      }catch (Exception e) {
          throw new BusinessException(ResponseCodeEnum.CODE_500);
      }
        log.info("我进入切面了");
    }   
    private TokenUserInfoDto checkLogin(boolean checkAdmin){
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest servletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
        String token = servletRequest.getHeader("token");
        TokenUserInfoDto tokenUserByToken = redisComponent.getTokenUserByToken(token);
        if (tokenUserByToken == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        if (checkAdmin && !tokenUserByToken.getAdmin()) {
            //如果校验是否为管理员 且 tokenuser并没有admin 则 返回902状态码！
            throw new BusinessException(ResponseCodeEnum.CODE_902);
        }
        return tokenUserByToken;
    }



}
