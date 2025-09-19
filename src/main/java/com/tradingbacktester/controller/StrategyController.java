package com.tradingbacktester.controller;

import com.tradingbacktester.service.SignalGenerationService;
import com.tradingbacktester.strategy.MovingAverageStrategy;
import com.tradingbacktester.strategy.TradingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for trading strategy operations
 */
@RestController
@RequestMapping("/api/strategy")
public class StrategyController {
    
    private static final Logger logger = LoggerFactory.getLogger(StrategyController.class);
    
    @Autowired
    private SignalGenerationService signalGenerationService;
    
    /**
     * Generate trading signals for a symbol using Moving Average strategy
     * @param symbol Stock symbol (e.g., AAPL)
     * @param shortPeriod Short-term MA period (default: 20)
     * @param longPeriod Long-term MA period (default: 50)
     * @param startDate Start date for analysis (optional, format: YYYY-MM-DD)
     * @param endDate End date for analysis (optional, format: YYYY-MM-DD)
     */
    @PostMapping("/moving-average/signals")
    public ResponseEntity<Map<String, Object>> generateMovingAverageSignals(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "20") int shortPeriod,
            @RequestParam(defaultValue = "50") int longPeriod,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        logger.info("Received request to generate MA signals for symbol: {} with periods {}/{}", 
                   symbol, shortPeriod, longPeriod);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate parameters
            if (shortPeriod >= longPeriod) {
                response.put("status", "error");
                response.put("message", "Short period must be less than long period");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Parse dates if provided
            LocalDate start = null;
            LocalDate end = null;
            
            if (startDate != null && !startDate.isEmpty()) {
                try {
                    start = LocalDate.parse(startDate);
                } catch (DateTimeParseException e) {
                    response.put("status", "error");
                    response.put("message", "Invalid start date format. Use YYYY-MM-DD");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            if (endDate != null && !endDate.isEmpty()) {
                try {
                    end = LocalDate.parse(endDate);
                } catch (DateTimeParseException e) {
                    response.put("status", "error");
                    response.put("message", "Invalid end date format. Use YYYY-MM-DD");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            // Create strategy with specified parameters
            TradingStrategy strategy = new MovingAverageStrategy(shortPeriod, longPeriod, 
                                                               java.math.BigDecimal.valueOf(0.01));
            
            // Generate signals
            int signalsGenerated = signalGenerationService.generateSignals(symbol, strategy, start, end);
            
            // Prepare response
            response.put("status", "completed");
            response.put("symbol", symbol);
            response.put("strategy", strategy.getStrategyName());
            response.put("strategyId", strategy.getStrategyId());
            response.put("signalsGenerated", signalsGenerated);
            response.put("parameters", strategy.getParameters());
            response.put("startDate", start != null ? start.toString() : "all available data");
            response.put("endDate", end != null ? end.toString() : "all available data");
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            
            logger.info("Successfully generated {} signals for {} using {}", 
                       signalsGenerated, symbol, strategy.getStrategyName());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error generating signals for symbol: {} - {}", symbol, e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Internal error: " + e.getMessage());
            response.put("symbol", symbol);
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Generate signals for multiple symbols
     */
    @PostMapping("/moving-average/signals/bulk")
    public ResponseEntity<Map<String, Object>> generateBulkMovingAverageSignals(
            @RequestParam String symbols,
            @RequestParam(defaultValue = "20") int shortPeriod,
            @RequestParam(defaultValue = "50") int longPeriod,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Parse symbol list
            List<String> symbolList = Arrays.asList(symbols.split(","));
            symbolList = symbolList.stream()
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .collect(java.util.stream.Collectors.toList());
            
            logger.info("Received request to generate MA signals for {} symbols: {}", 
                       symbolList.size(), symbolList);
            
            // Parse dates
            LocalDate start = startDate != null && !startDate.isEmpty() ? LocalDate.parse(startDate) : null;
            LocalDate end = endDate != null && !endDate.isEmpty() ? LocalDate.parse(endDate) : null;
            
            // Create strategy
            TradingStrategy strategy = new MovingAverageStrategy(shortPeriod, longPeriod, 
                                                               java.math.BigDecimal.valueOf(0.01));
            
            // Generate signals for all symbols
            int totalSignals = signalGenerationService.generateSignalsForMultipleSymbols(
                symbolList, strategy, start, end);
            
            // Prepare response
            response.put("status", "completed");
            response.put("symbols", symbolList);
            response.put("strategy", strategy.getStrategyName());
            response.put("totalSignalsGenerated", totalSignals);
            response.put("parameters", strategy.getParameters());
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error generating bulk signals: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Internal error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Get strategy information
     */
    @GetMapping("/moving-average/info")
    public ResponseEntity<Map<String, Object>> getMovingAverageInfo(
            @RequestParam(defaultValue = "20") int shortPeriod,
            @RequestParam(defaultValue = "50") int longPeriod) {
        
        try {
            TradingStrategy strategy = new MovingAverageStrategy(shortPeriod, longPeriod, 
                                                               java.math.BigDecimal.valueOf(0.01));
            
            Map<String, Object> info = new HashMap<>();
            info.put("strategyId", strategy.getStrategyId());
            info.put("strategyName", strategy.getStrategyName());
            info.put("description", strategy.getDescription());
            info.put("parameters", strategy.getParameters());
            info.put("minimumDataPoints", strategy.getMinimumDataPoints());
            
            return ResponseEntity.ok(info);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Health check for strategy service
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Strategy Controller");
        health.put("timestamp", java.time.LocalDateTime.now().toString());
        
        return ResponseEntity.ok(health);
    }
}