package com.easymeeting.websocket;

import com.easymeeting.websocket.message.MessageHandler;
import com.easymeeting.websocket.netty.NettyWebSocketStarter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
@Slf4j
@Component
public class InitRun implements ApplicationRunner {
    @Resource
    private NettyWebSocketStarter nettyWebSocketStarter;
    @Resource
    private MessageHandler messageHandler;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        new Thread(nettyWebSocketStarter).start();
        listenAsync();
    }
    @Async
    public void listenAsync() {
        try {
            messageHandler.listenMessage();
        }catch (Exception e){
            log.error(e.getMessage(),e);
        }
    }
}