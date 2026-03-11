//package com.easymeeting.test;
//
//import com.rabbitmq.client.*;
//import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
//public class RabbitMqSubscriberAutoAck  {
//    private static final String EXCHANGE_NAME = "fanout_exchange";
//    public static void main(String[] args)throws Exception {
//        ConnectionFactory factory = new ConnectionFactory();
//        factory.setHost("localhost");
//        factory.setPort(5672);
//        try{
//            Connection connection = factory.newConnection(); Channel channel = connection.createChannel();
//            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
//            String queueName = channel.queueDeclare().getQueue();
//            channel.queueBind(queueName, EXCHANGE_NAME, "");
//            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
//                try{
//                    String message = new String(delivery.getBody(), "UTF-8");
//                    log.info("Received message: {}", message);
//
//                }catch (Exception e){
//                    log.error("subscriber接受消息失败",e);
//                }
//            };
//            channel.basicConsume(queueName,true,deliverCallback,consumerTag->{});
//        }catch (Exception e){
//            log.error(e.getMessage(),e);
//        }
//    }
//
//}
