package com.tradingbacktester.batch;

import com.tradingbacktester.model.MarketData;
import com.tradingbacktester.service.BigQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SpringBatch ItemWriter for storing market data in BigQuery
 */
@Component
public class MarketDataWriter implements ItemWriter<List<MarketData>> {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketDataWriter.class);
    
    @Autowired
    private BigQueryService bigQueryService;
    
    private int totalRecordsWritten = 0;
    
    @Override
    public void write(List<? extends List<MarketData>> chunks) throws Exception {
        logger.info("Writing {} chunks of market data to BigQuery", chunks.size());
        
        for (List<MarketData> chunk : chunks) {
            if (chunk != null && !chunk.isEmpty()) {
                String symbol = chunk.get(0).getSymbol();
                logger.info("Writing {} records for symbol: {}", chunk.size(), symbol);
                
                try {
                    boolean success = bigQueryService.insertMarketData(chunk);
                    
                    if (success) {
                        totalRecordsWritten += chunk.size();
                        logger.info("Successfully wrote {} records for symbol: {}. Total records written: {}", 
                                   chunk.size(), symbol, totalRecordsWritten);
                    } else {
                        logger.error("Failed to write market data for symbol: {}", symbol);
                        throw new RuntimeException("Failed to write market data to BigQuery for symbol: " + symbol);
                    }
                    
                } catch (Exception e) {
                    logger.error("Error writing market data for symbol: {} - {}", symbol, e.getMessage(), e);
                    throw e;
                }
            } else {
                logger.warn("Received empty chunk, skipping write operation");
            }
        }
        
        logger.info("Completed writing batch. Total records written so far: {}", totalRecordsWritten);
    }
    
    /**
     * Get the total number of records written
     */
    public int getTotalRecordsWritten() {
        return totalRecordsWritten;
    }
    
    /**
     * Reset the counter (useful for testing)
     */
    public void resetCounter() {
        this.totalRecordsWritten = 0;
        logger.info("Reset records written counter");
    }
}