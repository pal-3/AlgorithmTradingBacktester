package com.tradingbacktester.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for manually triggering batch jobs
 */
@RestController
@RequestMapping("/api/batch")
public class BatchController {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchController.class);
    
    @Autowired
    private JobLauncher jobLauncher;
    
    @Autowired
    private Job marketDataIngestionJob;
    
    /**
     * Trigger market data ingestion job
     * @param symbols Comma-separated list of stock symbols (e.g., "AAPL,TSLA")
     * @param outputSize "compact" for last 100 days, "full" for 20+ years
     */
    @PostMapping("/market-data/ingest")
    public ResponseEntity<Map<String, Object>> ingestMarketData(
            @RequestParam(defaultValue = "AAPL,TSLA") String symbols,
            @RequestParam(defaultValue = "compact") String outputSize) {
        
        logger.info("Received request to ingest market data for symbols: {} with output size: {}", symbols, outputSize);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Create job parameters
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("symbols", symbols)
                    .addString("outputSize", outputSize)
                    .addLong("timestamp", System.currentTimeMillis()) // Make each execution unique
                    .toJobParameters();
            
            // Launch the job
            JobExecution jobExecution = jobLauncher.run(marketDataIngestionJob, jobParameters);
            
            // Prepare response
            response.put("status", "started");
            response.put("jobExecutionId", jobExecution.getId());
            response.put("jobStatus", jobExecution.getStatus().toString());
            response.put("symbols", symbols);
            response.put("outputSize", outputSize);
            response.put("startTime", jobExecution.getStartTime());
            
            logger.info("Market data ingestion job started with execution ID: {}", jobExecution.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (JobExecutionAlreadyRunningException e) {
            logger.error("Job is already running: {}", e.getMessage());
            response.put("status", "error");
            response.put("message", "Job is already running");
            return ResponseEntity.badRequest().body(response);
            
        } catch (JobRestartException e) {
            logger.error("Job restart error: {}", e.getMessage());
            response.put("status", "error");
            response.put("message", "Job restart error: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (JobInstanceAlreadyCompleteException e) {
            logger.error("Job instance already complete: {}", e.getMessage());
            response.put("status", "error");
            response.put("message", "Job instance already complete");
            return ResponseEntity.badRequest().body(response);
            
        } catch (JobParametersInvalidException e) {
            logger.error("Invalid job parameters: {}", e.getMessage());
            response.put("status", "error");
            response.put("message", "Invalid job parameters: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            logger.error("Unexpected error starting job: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Unexpected error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Get job execution status
     */
    @GetMapping("/status/{jobExecutionId}")
    public ResponseEntity<Map<String, Object>> getJobStatus(@PathVariable Long jobExecutionId) {
        // This is a simplified status check - in a real application you'd query the job repository
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Job status endpoint - implementation needed with JobExplorer");
        response.put("jobExecutionId", jobExecutionId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Batch Controller");
        health.put("timestamp", java.time.LocalDateTime.now().toString());
        
        return ResponseEntity.ok(health);
    }
}