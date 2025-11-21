package com.pramithamj.kafka.controller;

import com.pramithamj.kafka.model.Order;
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

    private static final String[] PRODUCTS = {
        "Item1", "Item2", "Item3", "Item4", "Item5",
        "Item6", "Item7", "Item8", "Item9", "Item10"
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
     * Generate a random order matching assignment requirements
     * Order Schema: orderId (string), product (string), price (float), timestamp (long)
     */
    private Order generateRandomOrder() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        // Generate orderId: "1001", "1002", etc.
        String orderId = String.valueOf(1000 + random.nextInt(1, 10000));
        
        // Select random product: "Item1", "Item2", etc.
        String product = PRODUCTS[random.nextInt(PRODUCTS.length)];
        
        // Generate random price between 10.00 and 500.00
        float price = (float) (10.0 + random.nextDouble() * 490.0);
        // Round to 2 decimal places
        price = Math.round(price * 100.0f) / 100.0f;
        
        // Build Order using Avro-generated builder
        return Order.newBuilder()
            .setOrderId(orderId)
            .setProduct(product)
            .setPrice(price)
            .setTimestamp(System.currentTimeMillis())
            .build();
    }
}
