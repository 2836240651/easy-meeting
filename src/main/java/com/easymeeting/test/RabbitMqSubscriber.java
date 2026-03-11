//package com.easymeeting.test;
//
//import com.rabbitmq.client.*;
//import lombok.extern.slf4j.Slf4j;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Random;
//
//@Slf4j
//public class RabbitMqSubscriber {
//    private static final String EXCHANGE_NAME = "fanout_exchange";
//    private static final Integer MAX_RETRYTIMES = 3;
//    private static final String RETRY_COUNT_KEY = "retryCount";
//    public static void main(String[] args)throws Exception {
//        ConnectionFactory factory = new ConnectionFactory();
//        factory.setHost("localhost");
//        factory.setPort(5672);
//            Connection connection = factory.newConnection(); Channel channel = connection.createChannel();
//            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
//            String queueName = channel.queueDeclare().getQueue();
//            channel.queueBind(queueName, EXCHANGE_NAME, "");
//            Boolean autoAck =false;
//            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
//                try{
//                    String message = new String(delivery.getBody(), "UTF-8");
//                    log.info("Received message: {}", message+System.currentTimeMillis());
//                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
//                }catch (Exception e){
//                    log.error("subscriber接受消息失败",e);
//                   try{ handlerFaileMessage(channel,delivery,queueName);}catch (Exception e1){
//                       log.error(e1.getMessage(),e1);
//                   }
//                }
//            };
//            channel.basicConsume(queueName,autoAck,deliverCallback,consumerTag->{});
//            log.info("订阅已经启动,等待消息中....");
//    }
//    private static void handlerFaileMessage(Channel channel,Delivery delivery,String queueName) throws Exception{
//        Map<String, Object> headers = delivery.getProperties().getHeaders();
//        if (headers==null){
//            headers=new HashMap<>();
//        }
//        Integer retryCount=0;
//        if (headers.containsKey(RETRY_COUNT_KEY)){
//            retryCount = (Integer) headers.get(RETRY_COUNT_KEY);
//        }
//        if (retryCount<MAX_RETRYTIMES-1){
//            headers.put(RETRY_COUNT_KEY, retryCount+1);
//            AMQP.BasicProperties properties = new AMQP.BasicProperties().builder().headers(headers).build();
//            channel.basicPublish("",queueName,properties,delivery.getBody());
//            channel.basicAck(delivery.getEnvelope().getDeliveryTag(),false);
//
//        }else{
//        log.info("超过最大重试,放弃处理");
//        //超过最大重试次数放入死信队列
//        channel.basicReject(delivery.getEnvelope().getDeliveryTag(),false);
//        }
//    }
//
//}
//
