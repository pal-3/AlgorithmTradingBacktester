package com.tradingbacktester;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;

/**
 * Main Spring Boot application class for the Trading Backtester
 */
@SpringBootApplication
@EnableBatchProcessing
public class TradingBacktesterApplication {
    
    public static void main(String[] args) {
        System.out.println("Starting Trading Backtester Application...");
        SpringApplication.run(TradingBacktesterApplication.class, args);
        System.out.println("Trading Backtester Application started successfully!");
    }
}