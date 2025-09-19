package com.tradingbacktester.strategy;

import com.tradingbacktester.model.MarketData;
import com.tradingbacktester.model.TradingSignal;

import java.util.List;
import java.util.Map;

/**
 * Interface for all trading strategies
 * Defines the contract that all trading algorithms must implement
 */
public interface TradingStrategy {
    
    /**
     * Generate trading signals based on historical market data
     * @param marketData List of historical price data sorted by date (oldest first)
     * @return List of trading signals (BUY/SELL/HOLD) with timestamps
     */
    List<TradingSignal> generateSignals(List<MarketData> marketData);
    
    /**
     * Get the unique identifier for this strategy
     * @return Strategy ID (e.g., "sma_crossover_20_50")
     */
    String getStrategyId();
    
    /**
     * Get the human-readable name of this strategy
     * @return Strategy name (e.g., "Simple Moving Average Crossover")
     */
    String getStrategyName();
    
    /**
     * Get the strategy parameters/configuration
     * @return Map of parameter names to values
     */
    Map<String, Object> getParameters();
    
    /**
     * Get the minimum number of data points required for this strategy to work
     * @return Minimum required data points (e.g., 50 for a 50-day moving average)
     */
    int getMinimumDataPoints();
    
    /**
     * Validate that the strategy can be applied to the given market data
     * @param marketData Historical price data
     * @return true if data is sufficient and valid, false otherwise
     */
    default boolean validateData(List<MarketData> marketData) {
        if (marketData == null || marketData.isEmpty()) {
            return false;
        }
        
        // Check minimum data points requirement
        if (marketData.size() < getMinimumDataPoints()) {
            return false;
        }
        
        // Check data is sorted by date (oldest first)
        for (int i = 1; i < marketData.size(); i++) {
            if (marketData.get(i).getDate().isBefore(marketData.get(i-1).getDate())) {
                return false; // Data not sorted properly
            }
        }
        
        return true;
    }
    
    /**
     * Get strategy description for documentation/UI purposes
     * @return Detailed description of how the strategy works
     */
    String getDescription();
}