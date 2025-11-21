package com.pramithamj.kafka.dlq;

import com.pramithamj.kafka.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Dead Letter Queue handler for orders that failed all retry attempts
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DLQHandler {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.orders-dlq}")
    private String dlqTopic;

    /**
     * Send an order to the Dead Letter Queue
     * 
     * @param order The order that failed all processing attempts
     * @param exception The final exception that caused the failure
     * @param retryCount Number of retry attempts made
     */
    public void sendToDLQ(Order order, Exception exception, int retryCount) {
        log.error("Sending order to DLQ: orderId={}, retryCount={}, finalError={}", 
                order.getOrderId(), retryCount, exception.getMessage());

        try {
            kafkaTemplate.send(dlqTopic, order.getOrderId().toString(), order)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Order sent to DLQ successfully: orderId={}, partition={}, offset={}", 
                                order.getOrderId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                        
                        // In a real system, you might want to:
                        // - Send an alert/notification
                        // - Store error details in a database
                        // - Create a monitoring event
                        logDLQAlert(order, exception, retryCount);
                    } else {
                        log.error("CRITICAL: Failed to send order to DLQ: orderId={}, error={}", 
                                order.getOrderId(), ex.getMessage(), ex);
                        // This is critical - the message is lost if DLQ send fails
                        // Consider implementing a fallback mechanism (e.g., write to file, database)
                    }
                });
        } catch (Exception e) {
            log.error("CRITICAL: Exception while sending to DLQ: orderId={}", 
                    order.getOrderId(), e);
        }
    }

    /**
     * Log an alert for DLQ entries
     * In production, this would trigger monitoring/alerting systems
     */
    private void logDLQAlert(Order order, Exception exception, int retryCount) {
        log.warn("═══════════════════════════════════════════════════════════");
        log.warn("⚠️  DLQ ALERT - Order Failed All Processing Attempts");
        log.warn("═══════════════════════════════════════════════════════════");
        log.warn("Order ID:       {}", order.getOrderId());
        log.warn("Product:        {}", order.getProduct());
        log.warn("Price:          ${}", order.getPrice());
        log.warn("Retry Count:    {}", retryCount);
        log.warn("Final Error:    {}", exception.getMessage());
        log.warn("Timestamp:      {}", new java.util.Date(order.getTimestamp()));
        log.warn("═══════════════════════════════════════════════════════════");
        
        // In production, trigger:
        // - Email alerts to operations team
        // - PagerDuty/Slack notifications
        // - Metrics/Dashboard updates
        // - Database logging for investigation
    }

    /**
     * Process a message from the DLQ (for manual recovery)
     * 
     * @param order The order from DLQ to reprocess
     */
    public void reprocessFromDLQ(Order order) {
        log.info("Attempting to reprocess order from DLQ: orderId={}", order.getOrderId());
        // Implementation would depend on business requirements
        // This could involve:
        // - Manual validation
        // - Data correction
        // - Republishing to main topic
    }
}
