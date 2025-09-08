package com.tradingbacktester.service;

import com.google.cloud.bigquery.*;
import com.tradingbacktester.config.BigQueryConfig;
import com.tradingbacktester.model.MarketData;
import com.tradingbacktester.model.TradingSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for BigQuery operations - inserting and querying data
 */
@Service
public class BigQueryService {
    
    private static final Logger logger = LoggerFactory.getLogger(BigQueryService.class);
    
    private final BigQuery bigQuery;
    private final BigQueryConfig bigQueryConfig;
    
    @Autowired
    public BigQueryService(BigQuery bigQuery, BigQueryConfig bigQueryConfig) {
        this.bigQuery = bigQuery;
        this.bigQueryConfig = bigQueryConfig;
    }
    
    /**
     * Insert market data records into BigQuery
     */
    public boolean insertMarketData(List<MarketData> marketDataList) {
        if (marketDataList.isEmpty()) {
            logger.warn("No market data to insert");
            return true;
        }
        
        try {
            TableId tableId = TableId.of(bigQueryConfig.getDatasetId(), "market_data");
            
            // Convert MarketData objects to BigQuery rows
            InsertAllRequest.Builder requestBuilder = InsertAllRequest.newBuilder(tableId);
            
            for (MarketData data : marketDataList) {
                Map<String, Object> rowContent = new HashMap<>();
                rowContent.put("symbol", data.getSymbol());
                rowContent.put("trade_date", data.getDate().toString());
                rowContent.put("open_price", data.getOpenPrice());
                rowContent.put("high_price", data.getHighPrice());
                rowContent.put("low_price", data.getLowPrice());
                rowContent.put("close_price", data.getClosePrice());
                rowContent.put("adjusted_close", data.getAdjustedClose());
                rowContent.put("volume", data.getVolume());
                rowContent.put("data_source", "alpha_vantage");
                
                // Use symbol + date as unique ID for deduplication
                String insertId = data.getSymbol() + "_" + data.getDate().toString();
                requestBuilder.addRow(insertId, rowContent);
            }
            
            InsertAllRequest request = requestBuilder.build();
            InsertAllResponse response = bigQuery.insertAll(request);
            
            if (response.hasErrors()) {
                logger.error("Errors occurred while inserting market data:");
                response.getInsertErrors().forEach((key, errors) -> {
                    logger.error("Row {}: {}", key, errors);
                });
                return false;
            }
            
            logger.info("Successfully inserted {} market data records", marketDataList.size());
            return true;
            
        } catch (Exception e) {
            logger.error("Error inserting market data: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Insert trading signals into BigQuery
     */
    public boolean insertTradingSignals(List<TradingSignal> signals) {
        if (signals.isEmpty()) {
            logger.warn("No trading signals to insert");
            return true;
        }
        
        try {
            TableId tableId = TableId.of(bigQueryConfig.getDatasetId(), "trading_signals");
            InsertAllRequest.Builder requestBuilder = InsertAllRequest.newBuilder(tableId);
            
            for (TradingSignal signal : signals) {
                Map<String, Object> rowContent = new HashMap<>();
                rowContent.put("signal_id", UUID.randomUUID().toString());
                rowContent.put("strategy_id", signal.getStrategyId());
                rowContent.put("symbol", signal.getSymbol());
                rowContent.put("signal_date", signal.getSignalDate().toString());
                rowContent.put("signal_type", signal.getSignalType().toString());
                rowContent.put("price_at_signal", signal.getPriceAtSignal());
                rowContent.put("signal_strength", signal.getSignalStrength());
                
                if (signal.getMetadata() != null) {
                    rowContent.put("metadata", signal.getMetadata());
                }
                
                String insertId = signal.getSymbol() + "_" + signal.getSignalDate().toString() + "_" + 
                               signal.getStrategyId() + "_" + System.currentTimeMillis();
                requestBuilder.addRow(insertId, rowContent);
            }
            
            InsertAllRequest request = requestBuilder.build();
            InsertAllResponse response = bigQuery.insertAll(request);
            
            if (response.hasErrors()) {
                logger.error("Errors occurred while inserting trading signals:");
                response.getInsertErrors().forEach((key, errors) -> {
                    logger.error("Row {}: {}", key, errors);
                });
                return false;
            }
            
            logger.info("Successfully inserted {} trading signals", signals.size());
            return true;
            
        } catch (Exception e) {
            logger.error("Error inserting trading signals: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Query market data for a specific symbol and date range
     */
    public TableResult queryMarketData(String symbol, String startDate, String endDate) {
        try {
            String query = String.format(
                "SELECT * FROM `%s.%s.market_data` " +
                "WHERE symbol = '%s' AND trade_date BETWEEN '%s' AND '%s' " +
                "ORDER BY trade_date ASC",
                bigQueryConfig.getProjectId(), bigQueryConfig.getDatasetId(), 
                symbol, startDate, endDate
            );
            
            QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
            JobId jobId = JobId.of(UUID.randomUUID().toString());
            Job queryJob = bigQuery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());
            
            queryJob = queryJob.waitFor();
            
            if (queryJob == null) {
                throw new RuntimeException("Job no longer exists");
            } else if (queryJob.getStatus().getError() != null) {
                throw new RuntimeException(queryJob.getStatus().getError().toString());
            }
            
            return queryJob.getQueryResults();
            
        } catch (Exception e) {
            logger.error("Error querying market data: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Check if market data exists for a symbol
     */
    public boolean hasMarketData(String symbol) {
        try {
            String query = String.format(
                "SELECT COUNT(*) as count FROM `%s.%s.market_data` WHERE symbol = '%s' LIMIT 1",
                bigQueryConfig.getProjectId(), bigQueryConfig.getDatasetId(), symbol
            );
            
            QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
            TableResult result = bigQuery.query(queryConfig);
            
            for (FieldValueList row : result.iterateAll()) {
                long count = row.get("count").getLongValue();
                return count > 0;
            }
            
            return false;
            
        } catch (Exception e) {
            logger.error("Error checking market data existence: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get the latest date of market data for a symbol
     */
    public String getLatestMarketDataDate(String symbol) {
        try {
            String query = String.format(
                "SELECT MAX(trade_date) as latest_date FROM `%s.%s.market_data` WHERE symbol = '%s'",
                bigQueryConfig.getProjectId(), bigQueryConfig.getDatasetId(), symbol
            );
            
            QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
            TableResult result = bigQuery.query(queryConfig);
            
            for (FieldValueList row : result.iterateAll()) {
                FieldValue latestDate = row.get("latest_date");
                if (!latestDate.isNull()) {
                    return latestDate.getStringValue();
                }
            }
            
            return null;
            
        } catch (Exception e) {
            logger.error("Error getting latest market data date: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Test BigQuery connection
     */
    public boolean testConnection() {
        try {
            Dataset dataset = bigQuery.getDataset(bigQueryConfig.getDatasetId());
            if (dataset != null) {
                logger.info("Successfully connected to BigQuery dataset: {}", bigQueryConfig.getDatasetId());
                return true;
            } else {
                logger.error("Dataset not found: {}", bigQueryConfig.getDatasetId());
                return false;
            }
        } catch (Exception e) {
            logger.error("Error testing BigQuery connection: {}", e.getMessage(), e);
            return false;
        }
    }
}