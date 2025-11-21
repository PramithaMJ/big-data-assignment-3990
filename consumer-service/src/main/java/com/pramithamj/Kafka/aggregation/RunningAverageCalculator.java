package com.pramithamj.kafka.aggregation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Running average calculator for order amounts
 * Uses atomic operations for thread-safety
 */
@Slf4j
@Component
public class RunningAverageCalculator {

    private final AtomicLong totalAmount = new AtomicLong(0);
    private final AtomicInteger count = new AtomicInteger(0);

    /**
     * Add a new order amount to the running average
     * 
     * @param amount Order amount in cents (to avoid floating-point precision issues)
     * @return The new running average
     */
    public double addAmount(double amount) {
        // Convert to cents to work with long integers
        long amountInCents = Math.round(amount * 100);
        
        long newTotal = totalAmount.addAndGet(amountInCents);
        int newCount = count.incrementAndGet();
        
        double average = (double) newTotal / (newCount * 100);
        
        log.debug("Added amount: ${}, New running average: ${}, Total orders: {}", 
                 amount, average, newCount);
        
        return average;
    }

    /**
     * Get the current running average
     * 
     * @return The current average or 0.0 if no orders processed
     */
    public double getCurrentAverage() {
        int currentCount = count.get();
        if (currentCount == 0) {
            return 0.0;
        }
        
        long currentTotal = totalAmount.get();
        return (double) currentTotal / (currentCount * 100);
    }

    /**
     * Get the total number of orders processed
     * 
     * @return Total order count
     */
    public int getOrderCount() {
        return count.get();
    }

    /**
     * Get the total amount processed
     * 
     * @return Total amount
     */
    public double getTotalAmount() {
        return totalAmount.get() / 100.0;
    }

    /**
     * Reset the running average calculator
     */
    public void reset() {
        totalAmount.set(0);
        count.set(0);
        log.info("Running average calculator has been reset");
    }

    /**
     * Get statistics as a formatted string
     * 
     * @return Statistics string
     */
    public String getStatistics() {
        int currentCount = getOrderCount();
        double currentTotal = getTotalAmount();
        double currentAverage = getCurrentAverage();
        
        return String.format(
            "Orders Processed: %d | Total Amount: $%.2f | Running Average: $%.2f",
            currentCount, currentTotal, currentAverage
        );
    }
}
