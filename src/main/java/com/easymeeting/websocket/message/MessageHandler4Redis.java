package com.easymeeting.websocket.message;

import com.easymeeting.entity.constants.Constants;
import com.easymeeting.entity.dto.MessageSendDto;
import com.easymeeting.redis.RedissonConfig;
import com.easymeeting.utils.JsonUtils;
import com.easymeeting.websocket.ChannelContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.bridge.IMessageHandler;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

@Component("messageHandler")
@Slf4j
@ConditionalOnProperty(name = Constants.MESSAGEING_HANDLE_CHANNEL_KEY,havingValue = Constants.MESSAGEING_HANDLE_CHANNEL_REDIS)
public class MessageHandler4Redis implements MessageHandler {

    private static final String MESSAGE_TOPIC="message.topic";
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private ChannelContextUtils channelContextUtils;
    @Override
    public void listenMessage() {
        if (redissonClient != null) {
            RTopic topic = redissonClient.getTopic(MESSAGE_TOPIC);
            topic.addListener(MessageSendDto.class,(MessageSendDto,sendDto)->{
                log.info("🟡🟡🟡 从Redis Topic收到消息");
                log.info("🟡🟡🟡 消息类型: {}, 发送者: {}, 接收者: {}, messageSend2Type: {}", 
                         sendDto.getMessageType(),
                         sendDto.getSendUserId(),
                         sendDto.getReceiveUserId(),
                         sendDto.getMessageSend2Type());
                log.info("🟡🟡🟡 消息详情: {}", JsonUtils.convertObj2Json(sendDto));
                channelContextUtils.sendMessage(sendDto);
            });
            log.info("🟡🟡🟡 Redis消息监听器已启动");
        }
    }

    @Override
    public void sendMessage(MessageSendDto messageSendDto) {
        log.info("🔴🔴🔴 MessageHandler4Redis.sendMessage 被调用");
        log.info("🔴🔴🔴 消息类型: {}, 发送者: {}, 接收者: {}, messageSend2Type: {}", 
                 messageSendDto.getMessageType(),
                 messageSendDto.getSendUserId(),
                 messageSendDto.getReceiveUserId(),
                 messageSendDto.getMessageSend2Type());
        
        if (redissonClient != null) {
            RTopic topic = redissonClient.getTopic(MESSAGE_TOPIC);
            topic.publish(messageSendDto);
            log.info("🔴🔴🔴 消息已发布到Redis Topic");
        } else {
            log.error("🔴🔴🔴 RedissonClient 为 null，无法发送消息");
        }
    }
    @PreDestroy
    public void destroy() {
        if (redissonClient != null) {
            redissonClient.shutdown();
        }
    }




}
