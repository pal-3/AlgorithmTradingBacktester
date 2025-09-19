package com.tradingbacktester.config;

import com.tradingbacktester.batch.MarketDataProcessor;
import com.tradingbacktester.batch.MarketDataReader;
import com.tradingbacktester.batch.MarketDataWriter;
import com.tradingbacktester.model.MarketData;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SpringBatch configuration for market data processing jobs
 */
@Configuration
@EnableBatchProcessing
public class BatchConfig {
    
    @Autowired
    private JobBuilderFactory jobBuilderFactory;
    
    @Autowired
    private StepBuilderFactory stepBuilderFactory;
    
    @Value("${trading.backtester.chunk-size:100}")
    private int chunkSize;
    
    /**
     * Market data ingestion job
     */
    @Bean
    public Job marketDataIngestionJob(Step marketDataIngestionStep) {
        return jobBuilderFactory.get("marketDataIngestionJob")
                .incrementer(new RunIdIncrementer())
                .start(marketDataIngestionStep)
                .build();
    }
    
    /**
     * Step for processing market data
     */
    @Bean
    public Step marketDataIngestionStep(MarketDataReader reader,
                                       MarketDataProcessor processor,
                                       MarketDataWriter writer) {
        return stepBuilderFactory.get("marketDataIngestionStep")
                .<List<MarketData>, List<MarketData>>chunk(chunkSize)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }
    
    // Note: Reader, Processor, and Writer are auto-discovered via @Component annotations
    // No need to define them as @Bean methods here
}