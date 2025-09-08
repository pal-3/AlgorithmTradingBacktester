package com.tradingbacktester.batch;

import com.tradingbacktester.model.MarketData;
import com.tradingbacktester.service.AlphaVantageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

/**
 * SpringBatch ItemReader for fetching market data from Alpha Vantage API
 */
@Component
@StepScope
public class MarketDataReader implements ItemReader<List<MarketData>> {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketDataReader.class);
    
    @Autowired
    private AlphaVantageService alphaVantageService;
    
    @Value("#{jobParameters['symbols'] ?: 'AAPL,TSLA'}")
    private String symbolsParam;
    
    @Value("#{jobParameters['outputSize'] ?: 'compact'}")
    private String outputSize;
    
    private Iterator<String> symbolIterator;
    private boolean initialized = false;
    
    @Override
    public List<MarketData> read() throws Exception {
        if (!initialized) {
            initialize();
        }
        
        if (symbolIterator.hasNext()) {
            String symbol = symbolIterator.next();
            logger.info("Reading market data for symbol: {}", symbol);
            
            try {
                // Add delay to respect API rate limits (5 calls per minute for free tier)
                Thread.sleep(12000); // 12 seconds between calls = 5 calls per minute
                
                List<MarketData> marketData;
                if ("compact".equals(outputSize)) {
                    marketData = alphaVantageService.getDailyTimeSeriesCompact(symbol);
                } else {
                    marketData = alphaVantageService.getDailyTimeSeriesFull(symbol);
                }
                
                if (marketData.isEmpty()) {
                    logger.warn("No market data received for symbol: {}", symbol);
                    return null; // Skip to next symbol
                }
                
                logger.info("Successfully read {} records for symbol: {}", marketData.size(), symbol);
                return marketData;
                
            } catch (Exception e) {
                logger.error("Error reading market data for symbol: {} - {}", symbol, e.getMessage(), e);
                return null; // Skip to next symbol
            }
        }
        
        logger.info("Finished reading market data for all symbols");
        return null; // No more data to read
    }
    
    /**
     * Initialize the reader with symbols to process
     */
    private void initialize() {
        logger.info("Initializing MarketDataReader with symbols: {} and output size: {}", 
                   symbolsParam, outputSize);
        
        if (!alphaVantageService.isApiKeyConfigured()) {
            logger.error("Alpha Vantage API key is not configured properly");
            throw new RuntimeException("Alpha Vantage API key is not configured");
        }
        
        // Parse symbols from job parameters
        String[] symbols = symbolsParam.split(",");
        java.util.List<String> symbolList = java.util.Arrays.asList(symbols);
        
        // Clean up symbols (trim whitespace and convert to uppercase)
        symbolList = symbolList.stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(java.util.stream.Collectors.toList());
        
        this.symbolIterator = symbolList.iterator();
        this.initialized = true;
        
        logger.info("Initialized with {} symbols: {}", symbolList.size(), symbolList);
    }
}