package com.tradingbacktester.strategy;

import com.tradingbacktester.model.MarketData;
import com.tradingbacktester.model.TradingSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Simple Moving Average Crossover Strategy
 * 
 * Strategy Logic:
 * - Calculate short-period and long-period simple moving averages
 * - Generate BUY signal when short MA crosses above long MA (golden cross)
 * - Generate SELL signal when short MA crosses below long MA (death cross)
 * - Hold position otherwise
 */
@Component
public class MovingAverageStrategy implements TradingStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(MovingAverageStrategy.class);
    
    // Strategy parameters
    private final int shortPeriod;
    private final int longPeriod;
    private final BigDecimal signalThreshold;
    
    /**
     * Default constructor with common parameters
     * Short period: 20 days, Long period: 50 days
     */
    public MovingAverageStrategy() {
        this(20, 50, new BigDecimal("0.01"));
    }
    
    /**
     * Constructor with custom parameters
     * @param shortPeriod Short-term moving average period (e.g., 20 days)
     * @param longPeriod Long-term moving average period (e.g., 50 days)
     * @param signalThreshold Minimum percentage difference to trigger signal (e.g., 0.01 = 1%)
     */
    public MovingAverageStrategy(int shortPeriod, int longPeriod, BigDecimal signalThreshold) {
        if (shortPeriod >= longPeriod) {
            throw new IllegalArgumentException("Short period must be less than long period");
        }
        if (shortPeriod <= 0 || longPeriod <= 0) {
            throw new IllegalArgumentException("Periods must be positive integers");
        }
        
        this.shortPeriod = shortPeriod;
        this.longPeriod = longPeriod;
        this.signalThreshold = signalThreshold;
    }
    
    @Override
    public List<TradingSignal> generateSignals(List<MarketData> marketData) {
        if (!validateData(marketData)) {
            logger.warn("Invalid market data for strategy: {}", getStrategyId());
            return new ArrayList<>();
        }
        
        logger.info("Generating signals for {} with {} data points", getStrategyId(), marketData.size());
        
        List<TradingSignal> signals = new ArrayList<>();
        
        // Calculate moving averages
        List<BigDecimal> shortMA = calculateSimpleMovingAverage(marketData, shortPeriod);
        List<BigDecimal> longMA = calculateSimpleMovingAverage(marketData, longPeriod);
        
        // Generate signals based on crossovers
        for (int i = 1; i < shortMA.size(); i++) {
            MarketData currentData = marketData.get(i + longPeriod - 1); // Adjust for MA offset
            
            BigDecimal currentShortMA = shortMA.get(i);
            BigDecimal previousShortMA = shortMA.get(i - 1);
            BigDecimal currentLongMA = longMA.get(i);
            BigDecimal previousLongMA = longMA.get(i - 1);
            
            // Check for golden cross (BUY signal)
            if (isGoldenCross(previousShortMA, previousLongMA, currentShortMA, currentLongMA)) {
                TradingSignal buySignal = createSignal(
                    TradingSignal.SignalType.BUY,
                    currentData,
                    currentShortMA,
                    currentLongMA
                );
                signals.add(buySignal);
                logger.debug("BUY signal generated for {} on {}", currentData.getSymbol(), currentData.getDate());
            }
            // Check for death cross (SELL signal)
            else if (isDeathCross(previousShortMA, previousLongMA, currentShortMA, currentLongMA)) {
                TradingSignal sellSignal = createSignal(
                    TradingSignal.SignalType.SELL,
                    currentData,
                    currentShortMA,
                    currentLongMA
                );
                signals.add(sellSignal);
                logger.debug("SELL signal generated for {} on {}", currentData.getSymbol(), currentData.getDate());
            }
        }
        
        logger.info("Generated {} signals for symbol: {}", signals.size(), 
                   marketData.isEmpty() ? "N/A" : marketData.get(0).getSymbol());
        
        return signals;
    }
    
    /**
     * Calculate Simple Moving Average for given period
     */
    private List<BigDecimal> calculateSimpleMovingAverage(List<MarketData> data, int period) {
        List<BigDecimal> movingAverages = new ArrayList<>();
        
        for (int i = period - 1; i < data.size(); i++) {
            BigDecimal sum = BigDecimal.ZERO;
            
            // Sum prices for the period
            for (int j = i - period + 1; j <= i; j++) {
                sum = sum.add(data.get(j).getClosePrice());
            }
            
            // Calculate average
            BigDecimal average = sum.divide(new BigDecimal(period), 4, RoundingMode.HALF_UP);
            movingAverages.add(average);
        }
        
        return movingAverages;
    }
    
    /**
     * Check if golden cross occurred (short MA crosses above long MA)
     */
    private boolean isGoldenCross(BigDecimal prevShort, BigDecimal prevLong, 
                                 BigDecimal currShort, BigDecimal currLong) {
        // Previously: short MA was below long MA
        boolean wasBelowBefore = prevShort.compareTo(prevLong) <= 0;
        
        // Currently: short MA is above long MA
        boolean isAboveNow = currShort.compareTo(currLong) > 0;
        
        // Check if the crossover is significant enough
        BigDecimal percentageDiff = currShort.subtract(currLong)
                                           .divide(currLong, 4, RoundingMode.HALF_UP);
        boolean significantCross = percentageDiff.compareTo(signalThreshold) >= 0;
        
        return wasBelowBefore && isAboveNow && significantCross;
    }
    
    /**
     * Check if death cross occurred (short MA crosses below long MA)
     */
    private boolean isDeathCross(BigDecimal prevShort, BigDecimal prevLong,
                                BigDecimal currShort, BigDecimal currLong) {
        // Previously: short MA was above long MA
        boolean wasAboveBefore = prevShort.compareTo(prevLong) >= 0;
        
        // Currently: short MA is below long MA
        boolean isBelowNow = currShort.compareTo(currLong) < 0;
        
        // Check if the crossover is significant enough
        BigDecimal percentageDiff = currLong.subtract(currShort)
                                           .divide(currLong, 4, RoundingMode.HALF_UP);
        boolean significantCross = percentageDiff.compareTo(signalThreshold) >= 0;
        
        return wasAboveBefore && isBelowNow && significantCross;
    }
    
    /**
     * Create a trading signal with metadata
     */
    private TradingSignal createSignal(TradingSignal.SignalType signalType, MarketData marketData,
                                      BigDecimal shortMA, BigDecimal longMA) {
        TradingSignal signal = new TradingSignal();
        signal.setStrategyId(getStrategyId());
        signal.setSymbol(marketData.getSymbol());
        signal.setSignalDate(marketData.getDate());
        signal.setSignalType(signalType);
        signal.setPriceAtSignal(marketData.getClosePrice());
        
        // Calculate signal strength based on MA separation
        BigDecimal separation = shortMA.subtract(longMA).abs();
        BigDecimal signalStrength = separation.divide(longMA, 4, RoundingMode.HALF_UP);
        signal.setSignalStrength(signalStrength);
        
        // Add metadata with MA values
        String metadata = String.format("{\"short_ma_%.2f\": %.2f, \"long_ma_%d\": %.2f, \"price\": %.2f}",
                (double) shortPeriod, shortMA.doubleValue(),
                longPeriod, longMA.doubleValue(),
                marketData.getClosePrice().doubleValue());
        signal.setMetadata(metadata);
        
        return signal;
    }
    
    @Override
    public String getStrategyId() {
        return String.format("sma_crossover_%d_%d", shortPeriod, longPeriod);
    }
    
    @Override
    public String getStrategyName() {
        return String.format("Simple Moving Average Crossover (%d/%d)", shortPeriod, longPeriod);
    }
    
    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("short_period", shortPeriod);
        params.put("long_period", longPeriod);
        params.put("signal_threshold", signalThreshold.doubleValue());
        return params;
    }
    
    @Override
    public int getMinimumDataPoints() {
        return longPeriod; // Need at least long period worth of data
    }
    
    @Override
    public String getDescription() {
        return String.format(
            "Simple Moving Average Crossover strategy using %d-day and %d-day moving averages. " +
            "Generates BUY signals when the %d-day MA crosses above the %d-day MA (golden cross), " +
            "and SELL signals when it crosses below (death cross). " +
            "Minimum signal threshold: %.1f%%",
            shortPeriod, longPeriod, shortPeriod, longPeriod, signalThreshold.doubleValue() * 100
        );
    }
}