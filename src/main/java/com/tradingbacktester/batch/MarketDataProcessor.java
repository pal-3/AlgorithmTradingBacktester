package com.tradingbacktester.batch;

import com.tradingbacktester.model.MarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SpringBatch ItemProcessor for cleaning and validating market data
 */
@Component
public class MarketDataProcessor implements ItemProcessor<List<MarketData>, List<MarketData>> {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketDataProcessor.class);
    
    @Override
    public List<MarketData> process(List<MarketData> marketDataList) throws Exception {
        if (marketDataList == null || marketDataList.isEmpty()) {
            logger.warn("Received empty market data list for processing");
            return null;
        }
        
        String symbol = marketDataList.get(0).getSymbol();
        logger.info("Processing {} market data records for symbol: {}", marketDataList.size(), symbol);
        
        // Filter and clean the data
        List<MarketData> processedData = marketDataList.stream()
                .filter(this::isValidMarketData)
                .map(this::cleanMarketData)
                .filter(data -> data != null)
                .collect(Collectors.toList());
        
        logger.info("After processing: {} valid records for symbol: {}", processedData.size(), symbol);
        
        if (processedData.isEmpty()) {
            logger.warn("No valid records after processing for symbol: {}", symbol);
            return null;
        }
        
        // Sort by date (oldest first)
        processedData.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        
        return processedData;
    }
    
    /**
     * Validate market data record
     */
    private boolean isValidMarketData(MarketData data) {
        if (data == null) {
            return false;
        }
        
        // Check required fields
        if (data.getSymbol() == null || data.getSymbol().trim().isEmpty()) {
            logger.warn("Invalid market data: missing symbol");
            return false;
        }
        
        if (data.getDate() == null) {
            logger.warn("Invalid market data: missing date for symbol {}", data.getSymbol());
            return false;
        }
        
        // Check date is not in the future
        if (data.getDate().isAfter(LocalDate.now())) {
            logger.warn("Invalid market data: future date {} for symbol {}", data.getDate(), data.getSymbol());
            return false;
        }
        
        // Check price fields are not null and are positive
        if (!isValidPrice(data.getOpenPrice()) || 
            !isValidPrice(data.getHighPrice()) || 
            !isValidPrice(data.getLowPrice()) || 
            !isValidPrice(data.getClosePrice()) || 
            !isValidPrice(data.getAdjustedClose())) {
            logger.warn("Invalid market data: invalid prices for symbol {} on date {}", 
                       data.getSymbol(), data.getDate());
            return false;
        }
        
        // Check volume is not null and is non-negative
        if (data.getVolume() == null || data.getVolume() < 0) {
            logger.warn("Invalid market data: invalid volume for symbol {} on date {}", 
                       data.getSymbol(), data.getDate());
            return false;
        }
        
        // Check price relationships (high >= low, etc.)
        if (data.getHighPrice().compareTo(data.getLowPrice()) < 0) {
            logger.warn("Invalid market data: high price < low price for symbol {} on date {}", 
                       data.getSymbol(), data.getDate());
            return false;
        }
        
        if (data.getHighPrice().compareTo(data.getOpenPrice()) < 0 || 
            data.getHighPrice().compareTo(data.getClosePrice()) < 0) {
            logger.warn("Invalid market data: high price < open/close price for symbol {} on date {}", 
                       data.getSymbol(), data.getDate());
            return false;
        }
        
        if (data.getLowPrice().compareTo(data.getOpenPrice()) > 0 || 
            data.getLowPrice().compareTo(data.getClosePrice()) > 0) {
            logger.warn("Invalid market data: low price > open/close price for symbol {} on date {}", 
                       data.getSymbol(), data.getDate());
            return false;
        }
        
        return true;
    }
    
    /**
     * Clean and normalize market data
     */
    private MarketData cleanMarketData(MarketData data) {
        try {
            // Normalize symbol to uppercase
            data.setSymbol(data.getSymbol().trim().toUpperCase());
            
            // Round prices to 2 decimal places
            data.setOpenPrice(roundToTwoDecimals(data.getOpenPrice()));
            data.setHighPrice(roundToTwoDecimals(data.getHighPrice()));
            data.setLowPrice(roundToTwoDecimals(data.getLowPrice()));
            data.setClosePrice(roundToTwoDecimals(data.getClosePrice()));
            data.setAdjustedClose(roundToTwoDecimals(data.getAdjustedClose()));
            
            return data;
            
        } catch (Exception e) {
            logger.error("Error cleaning market data for symbol {} on date {}: {}", 
                        data.getSymbol(), data.getDate(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if price is valid (not null and positive)
     */
    private boolean isValidPrice(BigDecimal price) {
        return price != null && price.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Round BigDecimal to 2 decimal places
     */
    private BigDecimal roundToTwoDecimals(BigDecimal value) {
        return value.setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}