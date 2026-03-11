package com.easymeeting.controller;

import com.easymeeting.annotation.globalInterceptor;
import com.easymeeting.entity.vo.ResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/testmessage")
public class testSendMessage extends ABaseController {

    @RequestMapping("/redission")
    @globalInterceptor(checkAdmin = true)
    public ResponseVO rabbitmqSend() {
        return getFailResponseVO("测试消息接口已下线");
    }
}
