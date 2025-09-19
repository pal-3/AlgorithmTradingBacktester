package com.tradingbacktester.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingbacktester.model.MarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Service to fetch market data from Alpha Vantage API
 */
@Service
public class AlphaVantageService {
    
    private static final Logger logger = LoggerFactory.getLogger(AlphaVantageService.class);
    
    @Value("${alpha-vantage.api-key}")
    private String apiKey;
    
    @Value("${alpha-vantage.base-url}")
    private String baseUrl;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public AlphaVantageService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Fetch daily time series data for a given symbol
     * @param symbol Stock symbol (e.g., "AAPL", "TSLA")
     * @param outputSize "compact" for last 100 days, "full" for 20+ years
     * @return List of MarketData objects
     */
    public List<MarketData> getDailyTimeSeries(String symbol, String outputSize) {
        logger.info("Fetching daily time series for symbol: {} with output size: {}", symbol, outputSize);
        
        try {
            String url = buildTimeSeriesUrl(symbol, outputSize);
            String response = restTemplate.getForObject(url, String.class);
            
            if (response == null || response.isEmpty()) {
                logger.error("Empty response received from Alpha Vantage for symbol: {}", symbol);
                return new ArrayList<>();
            }
            
            return parseTimeSeriesResponse(response, symbol);
            
        } catch (Exception e) {
            logger.error("Error fetching data for symbol: {} - {}", symbol, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Fetch compact daily data (last 100 trading days)
     */
    public List<MarketData> getDailyTimeSeriesCompact(String symbol) {
        return getDailyTimeSeries(symbol, "compact");
    }
    
    /**
     * Fetch full daily data (20+ years of history)
     */
    public List<MarketData> getDailyTimeSeriesFull(String symbol) {
        return getDailyTimeSeries(symbol, "full");
    }
    
    /**
     * Build the API URL for time series request
     */
    private String buildTimeSeriesUrl(String symbol, String outputSize) {
        return String.format("%s?function=TIME_SERIES_DAILY&symbol=%s&outputsize=%s&apikey=%s",
                baseUrl, symbol, outputSize, apiKey);
    }
    
    /**
     * Parse the JSON response from Alpha Vantage
     */
    private List<MarketData> parseTimeSeriesResponse(String response, String symbol) throws Exception {
        JsonNode rootNode = objectMapper.readTree(response);
        
        // Debug: Log the actual response structure
        logger.info("API Response keys: {}", rootNode.fieldNames());
        logger.info("Full response: {}", response.substring(0, Math.min(500, response.length())));
        // Check for API errors
        if (rootNode.has("Error Message")) {
            throw new RuntimeException("API Error: " + rootNode.get("Error Message").asText());
        }
        
        if (rootNode.has("Note")) {
            logger.warn("API Rate limit warning: {}", rootNode.get("Note").asText());
            throw new RuntimeException("API rate limit exceeded");
        }
        
        // Get the time series data
        JsonNode timeSeriesNode = rootNode.get("Time Series (Daily)");
        if (timeSeriesNode == null) {
            logger.error("No time series data found in response for symbol: {}", symbol);
            return new ArrayList<>();
        }
        
        List<MarketData> marketDataList = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        // Iterate through each date in the time series
        Iterator<Map.Entry<String, JsonNode>> fields = timeSeriesNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String dateStr = entry.getKey();
            JsonNode dailyData = entry.getValue();
            
            try {
                MarketData marketData = new MarketData();
                marketData.setSymbol(symbol);
                marketData.setDate(LocalDate.parse(dateStr, dateFormatter));
                
                // Parse price data
                marketData.setOpenPrice(new BigDecimal(dailyData.get("1. open").asText()));
                marketData.setHighPrice(new BigDecimal(dailyData.get("2. high").asText()));
                marketData.setLowPrice(new BigDecimal(dailyData.get("3. low").asText()));
                marketData.setClosePrice(new BigDecimal(dailyData.get("4. close").asText()));
                marketData.setAdjustedClose(new BigDecimal(dailyData.get("4. close").asText())); // Use close price as adjusted close for free endpoint
                marketData.setVolume(Long.parseLong(dailyData.get("5. volume").asText()));
                
                marketDataList.add(marketData);
                
            } catch (Exception e) {
                logger.warn("Error parsing data for date {} and symbol {}: {}", dateStr, symbol, e.getMessage());
            }
        }
        
        logger.info("Successfully parsed {} market data records for symbol: {}", marketDataList.size(), symbol);
        return marketDataList;
    }
    
    /**
     * Check if API key is configured
     */
    public boolean isApiKeyConfigured() {
        return apiKey != null && !apiKey.isEmpty() && !"demo".equals(apiKey);
    }
    
    /**
     * Get current API rate limit info
     */
    public String getApiInfo() {
        return String.format("Alpha Vantage API - Key configured: %s, Base URL: %s", 
                isApiKeyConfigured(), baseUrl);
    }
}