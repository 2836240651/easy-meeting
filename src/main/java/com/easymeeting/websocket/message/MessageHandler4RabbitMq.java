package com.easymeeting.websocket.message;

import com.easymeeting.entity.constants.Constants;
import com.easymeeting.entity.dto.MessageSendDto;
import com.easymeeting.entity.enums.MessageSend2TypeEnum;
import com.easymeeting.utils.JsonUtils;
import com.easymeeting.websocket.ChannelContextUtils;
import com.mysql.cj.util.StringUtils;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@ConditionalOnProperty(name = Constants.MESSAGEING_HANDLE_CHANNEL_KEY,havingValue = Constants.MESSAGEING_HANDLE_CHANNEL_RABBITMQ)
public class MessageHandler4RabbitMq implements MessageHandler {
    private static final String EXCHANGE_NAME = "fanout_exchange";
    private static final Integer MAX_RETRYTIMES = 3;
    private static final String RETRY_COUNT_KEY = "retryCount";
    @Value("${rabbitmq.host:}")
    private String host;
    @Value("${rabbitmq.port:}")
    private Integer port;
    @Resource
    private ChannelContextUtils channelContextUtils;
    private Connection connection;
    private Channel channel;
    @Override
    public void listenMessage() {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        try{
            connection  = connectionFactory.newConnection();
            channel =connection.createChannel();
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, EXCHANGE_NAME, "");
            Boolean autoAck = false;
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
              try{
                  String message = new String(delivery.getBody(), "UTF-8");
                  log.info("收到信息 ：{}",message+System.currentTimeMillis());
                  channelContextUtils.sendMessage(JsonUtils.convertJson2Obj(message,MessageSendDto.class));
                  MessageSendDto sendDto = JsonUtils.convertJson2Obj(message, MessageSendDto.class);
                  log.info(sendDto.toString()+"----------------rabbitmq收到消息");
                  channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
              }catch (Exception e){
                  log.error("接收信息失败",e);
                  handlerFaileMessage(channel,delivery,queueName);
              }
            };
            channel.basicConsume(queueName,autoAck,deliverCallback,consumerTag ->{
            });
        }catch (Exception e){
            log.error("rabbitMq监听消息失败",e);
        }


    }
    private void handlerFaileMessage(Channel channel,Delivery delivery,String queueName) throws IOException {
        Map<String, Object> headers = delivery.getProperties().getHeaders();
        if (headers==null){
            headers=new HashMap<>();
        }
        Integer retryCount = 0;
        if (headers.containsKey(RETRY_COUNT_KEY)){
            retryCount = (Integer) headers.get(RETRY_COUNT_KEY);
        }
        if (retryCount<MAX_RETRYTIMES-1){
            headers.put(RETRY_COUNT_KEY, retryCount+1);
            AMQP.BasicProperties properties = new AMQP.BasicProperties().builder().headers(headers).build();
            channel.basicPublish(EXCHANGE_NAME, queueName, properties, delivery.getBody());
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }else {
        log.error("重试达最大次数信息转入死信队列");
            channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);
        }
    }
    
    @Override
    public void sendMessage(MessageSendDto messageSendDto) {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        if (messageSendDto==null){
            log.error("发送消息为空无法发送");
            return;
        }
        String message = JsonUtils.convertObj2Json(messageSendDto);
        if (message==null){
            log.error("转换失败消息对象为空");
            return;
        }
        try{
            connection  = connectionFactory.newConnection();
            channel = connection.createChannel();
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);

            channel.basicPublish(EXCHANGE_NAME,"",null,message.getBytes());
        }catch (Exception e){
            log.error(e.getMessage(),e);
        }
    }
    @PreDestroy
    public void destroy() throws IOException, TimeoutException {
        if (channel!=null&&channel.isOpen()){
            channel.close();
        }
        if (connection!=null&&connection.isOpen()){
            connection.close();
        }
    }
}
