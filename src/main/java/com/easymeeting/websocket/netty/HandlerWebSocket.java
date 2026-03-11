package com.easymeeting.websocket.netty;

import com.easymeeting.entity.constants.Constants;
import com.easymeeting.entity.dto.MessageSendDto;
import com.easymeeting.entity.dto.PeerConnectionDataDto;
import com.easymeeting.entity.dto.PeerMessageDto;
import com.easymeeting.entity.dto.TokenUserInfoDto;
import com.easymeeting.entity.enums.MessageSend2TypeEnum;
import com.easymeeting.entity.enums.MessageTypeEnum;
import com.easymeeting.entity.po.UserInfo;
import com.easymeeting.mappers.UserInfoMapper;
import com.easymeeting.redis.RedisComponent;
import com.easymeeting.utils.JsonUtils;
import com.easymeeting.websocket.message.MessageHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.xml.soap.Text;

@Component
@Slf4j
@ChannelHandler.Sharable
public class HandlerWebSocket extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private final UserInfoMapper userInfoMapper;
    private final RedisComponent redisComponent;
    
    @Resource
    private MessageHandler messageHandler;
    
    @Resource
    private com.easymeeting.websocket.ChannelContextUtils channelContextUtils;

    public HandlerWebSocket(UserInfoMapper userInfoMapper, RedisComponent redisComponent) {
        this.userInfoMapper = userInfoMapper;
        this.redisComponent = redisComponent;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("有新的连接加入....");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("有连接断开，channelId={}", ctx.channel().id());
        
        try {
            // 从 ChannelContextUtils 中查找对应的 userId
            String userId = channelContextUtils.getUserIdByChannel(ctx.channel());
            
            if (userId != null) {
                log.info("用户 {} 的连接断开，准备处理离线逻辑", userId);
                // 调用 ChannelContextUtils 处理用户离线
                channelContextUtils.handleUserOffline(userId);
                // 从 USER_CONTEXT_MAP 中移除
                channelContextUtils.removeContext(userId);
                log.info("用户 {} 断开连接，已处理离线逻辑", userId);
            } else {
                log.warn("连接断开时未找到对应的用户ID，channelId={}", ctx.channel().id());
            }
        } catch (Exception e) {
            log.error("处理连接断开失败: {}", e.getMessage(), e);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame textWebSocketFrame) throws Exception {
        String text = textWebSocketFrame.text();
        if (Constants.PING.equals(text)){
            // 响应心跳
            String userId = channelContextUtils.getUserIdByChannel(channelHandlerContext.channel());
            if (userId != null) {
                redisComponent.refreshUserHeartbeat(userId);
            }
            channelHandlerContext.writeAndFlush(new TextWebSocketFrame("pong"));
            return;
        }
        
        try {
            String userId = channelContextUtils.getUserIdByChannel(channelHandlerContext.channel());
            if (userId != null) {
                redisComponent.refreshUserHeartbeat(userId);
            }
            // 尝试解析为MessageSendDto（新格式，用于WebRTC Offer/Answer/ICE）
            MessageSendDto messageSendDto = JsonUtils.convertJson2Obj(text, MessageSendDto.class);
            
            // 检查是否是WebRTC消息（类型13/14/15）
            if (messageSendDto != null && messageSendDto.getMessageType() != null) {
                Integer messageType = messageSendDto.getMessageType();
                
                // WebRTC Offer (13), Answer (14), ICE Candidate (15), 
                // Screen Share Start (16), Screen Share Stop (17),
                // Screen Share Offer (18), Screen Share Answer (19), Screen Share ICE (20)
                if (messageType == 13 || messageType == 14 || messageType == 15 || 
                    messageType == 16 || messageType == 17 || messageType == 18 || 
                    messageType == 19 || messageType == 20) {
                    log.info("🎯🎯🎯 收到WebRTC/屏幕共享消息: 类型={}, 发送者={}, 接收者={}, messageSend2Type={}, meetingId={}", 
                             messageType, 
                             messageSendDto.getSendUserId(), 
                             messageSendDto.getReceiveUserId(),
                             messageSendDto.getMessageSend2Type(),
                             messageSendDto.getMeetingId());
                    log.info("🎯🎯🎯 消息内容: {}", JsonUtils.convertObj2Json(messageSendDto.getMessageContent()));
                    
                    // 直接转发消息
                    messageHandler.sendMessage(messageSendDto);
                    log.info("🎯🎯🎯 WebRTC/屏幕共享消息已发送到MessageHandler");
                    return;
                }
            }
            
            // 如果不是WebRTC消息，尝试解析为旧格式（PeerConnectionDataDto）
            PeerConnectionDataDto peerConnectionDataDto = JsonUtils.convertJson2Obj(text, PeerConnectionDataDto.class);
            TokenUserInfoDto tokenUserByToken = redisComponent.getTokenUserByToken(peerConnectionDataDto.getToken());
            if (tokenUserByToken == null){
                return;
            }
            MessageSendDto oldFormatMessage = new MessageSendDto();
            oldFormatMessage.setMessageType(MessageTypeEnum.PEER.getType());
            PeerMessageDto peerMessageDto = new PeerMessageDto();
            peerMessageDto.setSignalData(peerConnectionDataDto.getSignalData());
            peerMessageDto.setSignalType(peerConnectionDataDto.getSignalType());
            oldFormatMessage.setMessageContent(peerMessageDto);
            oldFormatMessage.setMeetingId(tokenUserByToken.getCurrentMeetingId());
            oldFormatMessage.setSendUserId(tokenUserByToken.getUserId());
            oldFormatMessage.setReceiveUserId(peerConnectionDataDto.getReceiveUserId());
            oldFormatMessage.setMessageSend2Type(MessageSend2TypeEnum.USER.getType());
            messageHandler.sendMessage(oldFormatMessage);
        } catch (Exception e) {
            log.error("处理WebSocket消息失败: {}", e.getMessage(), e);
        }
    }
}
