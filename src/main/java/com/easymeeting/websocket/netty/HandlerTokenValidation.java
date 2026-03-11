package com.easymeeting.websocket.netty;

import com.easymeeting.entity.dto.TokenUserInfoDto;
import com.easymeeting.redis.RedisComponent;
import com.easymeeting.websocket.ChannelContextUtils;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.Charset;
import java.util.List;

@ChannelHandler.Sharable
@Component
@Slf4j
public class HandlerTokenValidation extends SimpleChannelInboundHandler<FullHttpRequest> {
    @Resource
    private ChannelContextUtils channelContextUtils;
    @Resource
    private RedisComponent redisComponent;
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest request) throws Exception {
        String uri = request.uri();
        log.info("uri:{}", uri);
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri);
        List<String> tokens = queryStringDecoder.parameters().get("token");
        log.info("token:{}", tokens);
        if (tokens == null || tokens.isEmpty()) {
            log.info("token为空");
            sendErrorResponse(channelHandlerContext);
            return;
        }
        String token = tokens.get(0);
        TokenUserInfoDto tokenUserInfoDto = checkToken(token);
        log.info("token:{}", tokenUserInfoDto);
        if (tokenUserInfoDto == null) {
            log.error("token校验失败：{}",token);
            sendErrorResponse(channelHandlerContext);
            return;
        }
        channelHandlerContext.fireChannelRead(request.retain());
        channelContextUtils.addContext(tokenUserInfoDto.getUserId(),channelHandlerContext.channel());


        //Todo 连接后的初始化工作
    }

    private TokenUserInfoDto checkToken(String token) {
        if (token == null || token.isEmpty()){
            return null;
        }
        return redisComponent.getTokenUserByToken(token);
    }




    private void sendErrorResponse(ChannelHandlerContext ctx) {
        FullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN, Unpooled.copiedBuffer("token无效", CharsetUtil.UTF_8));
        resp.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
        resp.headers().set(HttpHeaders.Names.CONTENT_LENGTH, resp.content().readableBytes());
        ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);

    }
}
