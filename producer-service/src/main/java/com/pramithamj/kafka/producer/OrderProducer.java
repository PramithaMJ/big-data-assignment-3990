package com.pramithamj.kafka.producer;

import com.pramithamj.kafka.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.orders}")
    private String ordersTopic;

    /**
     * Send an order to Kafka with async callback handling
     * 
     * @param order The order to send
     * @return CompletableFuture for async handling
     */
    public CompletableFuture<SendResult<String, Object>> sendOrder(Order order) {
        log.info("Sending order to Kafka: orderId={}, product={}, price=${}", 
                 order.getOrderId(), order.getProduct(), order.getPrice());
        
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
            ordersTopic, 
            order.getOrderId().toString(), 
            order
        );
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Order sent successfully: orderId={}, partition={}, offset={}", 
                         order.getOrderId(),
                         result.getRecordMetadata().partition(),
                         result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send order: orderId={}, error={}", 
                          order.getOrderId(), ex.getMessage(), ex);
            }
        });
        
        return future;
    }

    /**
     * Send an order synchronously (blocking)
     * 
     * @param order The order to send
     * @throws Exception if send fails
     */
    public void sendOrderSync(Order order) throws Exception {
        log.info("Sending order synchronously: orderId={}", order.getOrderId());
        
        SendResult<String, Object> result = kafkaTemplate.send(
            ordersTopic, 
            order.getOrderId().toString(), 
            order
        ).get(); // Blocking call
        
        log.info("Order sent successfully: orderId={}, partition={}, offset={}", 
                 order.getOrderId(),
                 result.getRecordMetadata().partition(),
                 result.getRecordMetadata().offset());
    }

    /**
     * Send order to a specific partition
     * 
     * @param order The order to send
     * @param partition Target partition
     * @return CompletableFuture for async handling
     */
    public CompletableFuture<SendResult<String, Object>> sendOrderToPartition(Order order, int partition) {
        log.info("Sending order to partition {}: orderId={}", partition, order.getOrderId());
        
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
            ordersTopic,
            partition,
            order.getOrderId().toString(),
            order
        );
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Order sent to partition successfully: orderId={}, partition={}, offset={}", 
                         order.getOrderId(),
                         result.getRecordMetadata().partition(),
                         result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send order to partition: orderId={}, partition={}, error={}", 
                          order.getOrderId(), partition, ex.getMessage(), ex);
            }
        });
        
        return future;
    }
}
