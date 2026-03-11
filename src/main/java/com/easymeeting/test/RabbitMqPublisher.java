//package com.easymeeting.test;
//
//import com.rabbitmq.client.BuiltinExchangeType;
//import com.rabbitmq.client.Channel;
//import com.rabbitmq.client.Connection;
//import com.rabbitmq.client.ConnectionFactory;
//import lombok.extern.slf4j.Slf4j;
//
//import java.io.IOException;
//import java.util.concurrent.TimeoutException;
//
//@Slf4j
//public class RabbitMqPublisher {
//private static final String EXCHANGE_NAME = "fanout_exchange";
//
//    public static void main(String[] args) throws Exception{
//        ConnectionFactory connectionFactory = new ConnectionFactory();
//        connectionFactory.setHost("localhost");
//        connectionFactory.setPort(5672);
//        try(Connection connection = connectionFactory.newConnection(); Channel channel = connection.createChannel()){
//        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
//        String message = "这是我发布的一条信息("+System.currentTimeMillis()+")";
//        channel.basicPublish(EXCHANGE_NAME, "", null, message.getBytes());
//        log.info("发布一条信息:{}",System.currentTimeMillis());
//        }
//    }
//
//
//
//
//
//}
