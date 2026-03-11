package com.easymeeting.websocket.netty;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HandlerHeartBeat extends ChannelDuplexHandler {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent){
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state()== IdleState.READER_IDLE){
                // 修复：使用正确的AttributeKey获取userId
                String channelId = ctx.channel().id().toString();
                Attribute<String> attr = ctx.channel().attr(AttributeKey.valueOf(channelId));
                String userId = attr.get();
                log.info("心跳检测 - userId: {}, channelId: {}", userId, channelId);
                
                if (userId != null) {
                    log.info("用户 {} 心跳超时，关闭连接", userId);
                } else {
                    log.warn("心跳检测时未找到用户ID，关闭连接");
                }
                ctx.close();
            }else if (e.state()== IdleState.WRITER_IDLE){
                ctx.writeAndFlush("heart");
            }
        }
    }
}
