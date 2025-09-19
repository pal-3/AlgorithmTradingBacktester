package com.tradingbacktester.service;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.TableResult;
import com.tradingbacktester.model.MarketData;
import com.tradingbacktester.model.TradingSignal;
import com.tradingbacktester.strategy.TradingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for generating trading signals by applying strategies to market data
 */
@Service
public class SignalGenerationService {
    
    private static final Logger logger = LoggerFactory.getLogger(SignalGenerationService.class);
    
    @Autowired
    private BigQueryService bigQueryService;
    
    /**
     * Generate trading signals for a symbol using the specified strategy
     * @param symbol Stock symbol (e.g., "AAPL")
     * @param strategy Trading strategy to apply
     * @param startDate Start date for analysis (optional, uses all available data if null)
     * @param endDate End date for analysis (optional, uses all available data if null)
     * @return Number of signals generated and stored
     */
    public int generateSignals(String symbol, TradingStrategy strategy, LocalDate startDate, LocalDate endDate) {
        logger.info("Generating signals for symbol: {} using strategy: {}", symbol, strategy.getStrategyName());
        
        try {
            // Fetch market data for the symbol
            List<MarketData> marketData = fetchMarketData(symbol, startDate, endDate);
            
            if (marketData.isEmpty()) {
                logger.warn("No market data found for symbol: {}", symbol);
                return 0;
            }
            
            logger.info("Retrieved {} market data records for {}", marketData.size(), symbol);
            
            // Validate data is sufficient for strategy
            if (!strategy.validateData(marketData)) {
                logger.warn("Insufficient data for strategy: {}. Required: {}, Available: {}", 
                           strategy.getStrategyId(), strategy.getMinimumDataPoints(), marketData.size());
                return 0;
            }
            
            // Generate signals using the strategy
            List<TradingSignal> signals = strategy.generateSignals(marketData);
            
            if (signals.isEmpty()) {
                logger.info("No trading signals generated for {} using {}", symbol, strategy.getStrategyName());
                return 0;
            }
            
            // Store signals in BigQuery
            boolean success = bigQueryService.insertTradingSignals(signals);
            
            if (success) {
                logger.info("Successfully stored {} trading signals for {} using {}", 
                           signals.size(), symbol, strategy.getStrategyName());
                return signals.size();
            } else {
                logger.error("Failed to store trading signals for {}", symbol);
                return 0;
            }
            
        } catch (Exception e) {
            logger.error("Error generating signals for symbol: {} - {}", symbol, e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Generate signals for multiple symbols using the same strategy
     */
    public int generateSignalsForMultipleSymbols(List<String> symbols, TradingStrategy strategy, 
                                                LocalDate startDate, LocalDate endDate) {
        logger.info("Generating signals for {} symbols using strategy: {}", symbols.size(), strategy.getStrategyName());
        
        int totalSignals = 0;
        
        for (String symbol : symbols) {
            int signalsGenerated = generateSignals(symbol, strategy, startDate, endDate);
            totalSignals += signalsGenerated;
        }
        
        logger.info("Total signals generated across all symbols: {}", totalSignals);
        return totalSignals;
    }
    
    /**
     * Fetch market data from BigQuery for the specified symbol and date range
     */
    private List<MarketData> fetchMarketData(String symbol, LocalDate startDate, LocalDate endDate) {
        List<MarketData> marketDataList = new ArrayList<>();
        
        try {
            // Build date filter clause
            String dateFilter = "";
            if (startDate != null && endDate != null) {
                dateFilter = String.format(" AND trade_date BETWEEN '%s' AND '%s'", startDate, endDate);
            } else if (startDate != null) {
                dateFilter = String.format(" AND trade_date >= '%s'", startDate);
            } else if (endDate != null) {
                dateFilter = String.format(" AND trade_date <= '%s'", endDate);
            }
            
            // Query market data
            String query = String.format(
                "SELECT symbol, trade_date, open_price, high_price, low_price, close_price, " +
                "adjusted_close, volume FROM `%s.%s.market_data` " +
                "WHERE symbol = '%s'%s ORDER BY trade_date ASC",
                bigQueryService.testConnection() ? "trading-backtester-dev" : "trading-backtester-dev",
                "trading_data", symbol, dateFilter
            );
            
            TableResult result = bigQueryService.queryMarketData(symbol, 
                startDate != null ? startDate.toString() : "1900-01-01",
                endDate != null ? endDate.toString() : "2100-12-31");
            
            if (result != null) {
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                
                for (FieldValueList row : result.iterateAll()) {
                    MarketData data = new MarketData();
                    data.setSymbol(row.get("symbol").getStringValue());
                    data.setDate(LocalDate.parse(row.get("trade_date").getStringValue(), dateFormatter));
                    data.setOpenPrice(new BigDecimal(row.get("open_price").getDoubleValue()));
                    data.setHighPrice(new BigDecimal(row.get("high_price").getDoubleValue()));
                    data.setLowPrice(new BigDecimal(row.get("low_price").getDoubleValue()));
                    data.setClosePrice(new BigDecimal(row.get("close_price").getDoubleValue()));
                    data.setAdjustedClose(new BigDecimal(row.get("adjusted_close").getDoubleValue()));
                    data.setVolume(row.get("volume").getLongValue());
                    
                    marketDataList.add(data);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error fetching market data for symbol: {} - {}", symbol, e.getMessage(), e);
        }
        
        return marketDataList;
    }
    
    /**
     * Get summary of existing signals for a symbol and strategy
     */
    public String getSignalSummary(String symbol, String strategyId) {
        try {
            String query = String.format(
                "SELECT signal_type, COUNT(*) as count FROM `trading-backtester-dev.trading_data.trading_signals` " +
                "WHERE symbol = '%s' AND strategy_id = '%s' GROUP BY signal_type",
                symbol, strategyId
            );
            
            // This would require extending BigQueryService to support custom queries
            // For now, return a placeholder
            return String.format("Signal summary for %s using %s - implement custom query method", symbol, strategyId);
            
        } catch (Exception e) {
            logger.error("Error getting signal summary: {}", e.getMessage(), e);
            return "Error retrieving signal summary";
        }
    }
}