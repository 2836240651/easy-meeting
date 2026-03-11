package com.easymeeting.websocket.netty;

import com.easymeeting.entity.config.AppConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

@Component
@Slf4j
public class NettyWebSocketStarter implements Runnable {
    private EventLoopGroup bossGroup=new NioEventLoopGroup();
    private EventLoopGroup workerGroup=new NioEventLoopGroup();
    @Resource
    private HandlerWebSocket handlerWebSocket;
    @Resource
    private HandlerTokenValidation handlerTokenValidation;
    @Resource
    private AppConfig appConfig;
    @Override
    public void run() {
    try{
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup,workerGroup);
        serverBootstrap.channel(NioServerSocketChannel.class).handler(new LoggingHandler(LogLevel.DEBUG)).childHandler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
                ChannelPipeline pipeline = channel.pipeline();

                /*
                * 对http协议的支持,使用http的编码器和解码器
                * */
                pipeline.addLast(new HttpServerCodec());
                /*
                * 这是一个http消息聚合器 主要 将分片的http消息 聚合成完整的Fullhttprequest Fullhttpresponse
                * */
                pipeline.addLast(new HttpObjectAggregator(64 * 1024));
                /*
                * int readIdleTimeSeconds   未收到客户端数据
                * int writerIdleTimeSeconds 一段时间未向客户端发送数据
                * int allIdleTimeSeconds 读和写都无活动
                * 调整为45秒，给客户端30秒心跳留出缓冲时间
                * */
                pipeline.addLast(new IdleStateHandler(45,0,0));
                pipeline.addLast(new HandlerHeartBeat());

                /*
                * token校验
                * 拦截channelRead事件
                * */
                pipeline.addLast(handlerTokenValidation);
                /*
                * websocket 协议处理器
                *String websocketPath, 路径
                *  String subprotocols, 指定支持的子协议
                *  boolean allowExtensions, 允许websocket扩展
                * int maxFrameSize, 设置最大帧数 (增加到65536以支持WebRTC的SDP消息)
                *  boolean allowMaskMismatch 是否允许掩码不匹配
                * boolean checkStartsWith, 是否严格检查路径开头
                * long handshakeTimeoutMillis  握手超时时间(单位毫秒)
                * */
                pipeline.addLast(new WebSocketServerProtocolHandler("/ws",
                        null,true,65536,
                        true,true,10000L));
                /*
                *
                * */
                pipeline.addLast(handlerWebSocket);
            }
        });

        Channel channel = serverBootstrap.bind(appConfig.getWsPort()).sync().channel();
        log.info("netty服务启动成功端口:{}",appConfig.getWsPort());
        channel.closeFuture().sync();
    }catch (Exception e) {
        log.error("netty启动失败",e);
    }finally {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    }
    @PreDestroy
    public void close(){
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}