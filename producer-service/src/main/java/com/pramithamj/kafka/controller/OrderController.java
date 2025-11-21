package com.pramithamj.kafka.controller;

import com.pramithamj.kafka.model.Address;
import com.pramithamj.kafka.model.Order;
import com.pramithamj.kafka.model.OrderStatus;
import com.pramithamj.kafka.model.PaymentMethod;
import com.pramithamj.kafka.producer.OrderProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderProducer orderProducer;

    private static final String[] PRODUCT_IDS = {
        "PROD-001", "PROD-002", "PROD-003", "PROD-004", "PROD-005",
        "PROD-006", "PROD-007", "PROD-008", "PROD-009", "PROD-010"
    };

    private static final String[] CITIES = {
        "New York", "Los Angeles", "Chicago", "Houston", "Phoenix",
        "Philadelphia", "San Antonio", "San Diego", "Dallas", "San Jose"
    };

    private static final String[] STATES = {
        "NY", "CA", "IL", "TX", "AZ", "PA", "TX", "CA", "TX", "CA"
    };

    /**
     * Create and send a single order
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody(required = false) Order order) {
        try {
            if (order == null) {
                order = generateRandomOrder();
            }
            
            orderProducer.sendOrder(order);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderId", order.getOrderId());
            response.put("message", "Order sent to Kafka successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating order", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Generate and send multiple random orders
     */
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> createBatchOrders(@RequestParam(defaultValue = "10") int count) {
        try {
            List<String> orderIds = new ArrayList<>();
            
            for (int i = 0; i < count; i++) {
                Order order = generateRandomOrder();
                orderProducer.sendOrder(order);
                orderIds.add(order.getOrderId().toString());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", count);
            response.put("orderIds", orderIds);
            response.put("message", count + " orders sent to Kafka successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating batch orders", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Send order to specific partition
     */
    @PostMapping("/partition/{partition}")
    public ResponseEntity<Map<String, Object>> createOrderToPartition(
            @PathVariable int partition,
            @RequestBody(required = false) Order order) {
        try {
            if (order == null) {
                order = generateRandomOrder();
            }
            
            orderProducer.sendOrderToPartition(order, partition);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderId", order.getOrderId());
            response.put("partition", partition);
            response.put("message", "Order sent to partition " + partition + " successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating order to partition", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "producer-service");
        return ResponseEntity.ok(response);
    }

    /**
     * Generate a random order for testing
     */
    private Order generateRandomOrder() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String customerId = "CUST-" + random.nextInt(1000, 9999);
        String productId = PRODUCT_IDS[random.nextInt(PRODUCT_IDS.length)];
        int quantity = random.nextInt(1, 10);
        double price = random.nextDouble(10.0, 500.0);
        double totalAmount = quantity * price;
        
        OrderStatus[] statuses = OrderStatus.values();
        OrderStatus status = statuses[random.nextInt(statuses.length)];
        
        PaymentMethod[] methods = PaymentMethod.values();
        PaymentMethod paymentMethod = methods[random.nextInt(methods.length)];
        
        int cityIndex = random.nextInt(CITIES.length);
        Address address = Address.newBuilder()
            .setStreet(random.nextInt(100, 9999) + " Main Street")
            .setCity(CITIES[cityIndex])
            .setState(STATES[cityIndex])
            .setZipCode(String.format("%05d", random.nextInt(10000, 99999)))
            .setCountry("USA")
            .build();
        
        return Order.newBuilder()
            .setOrderId(orderId)
            .setCustomerId(customerId)
            .setProductId(productId)
            .setQuantity(quantity)
            .setPrice(Math.round(price * 100.0) / 100.0)
            .setTotalAmount(Math.round(totalAmount * 100.0) / 100.0)
            .setOrderStatus(status)
            .setTimestamp(System.currentTimeMillis())
            .setShippingAddress(address)
            .setPaymentMethod(paymentMethod)
            .setNotes("Test order generated by producer service")
            .build();
    }
}
