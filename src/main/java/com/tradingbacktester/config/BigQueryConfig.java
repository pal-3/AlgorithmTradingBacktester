package com.tradingbacktester.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Configuration class for Google BigQuery integration
 */
@Configuration
public class BigQueryConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(BigQueryConfig.class);
    
    @Value("${google.cloud.bigquery.project-id}")
    private String projectId;
    
    @Value("${google.cloud.bigquery.dataset-id}")
    private String datasetId;
    
    @Value("${google.cloud.bigquery.credentials-path:}")
    private String credentialsPath;
    
    /**
     * Creates and configures BigQuery client
     * Only creates the bean if credentials are available
     */
    @Bean
    //@ConditionalOnProperty(value = "bigquery.enabled", havingValue = "true", matchIfMissing = false)
    public BigQuery bigQuery() throws IOException {
        logger.info("Initializing BigQuery client for project: {}", projectId);

        // Check if credentials file exists if path is provided
        if (credentialsPath != null && !credentialsPath.isEmpty()) {
            File credentialsFile = new File(credentialsPath);
            if (!credentialsFile.exists()) {
                logger.error("BigQuery credentials file not found at: {}", credentialsPath);
                throw new IOException("BigQuery credentials file not found: " + credentialsPath);
            }
        }

        BigQueryOptions.Builder optionsBuilder = BigQueryOptions.newBuilder()
                .setProjectId(projectId);

        // Configure credentials if path is provided
        if (credentialsPath != null && !credentialsPath.isEmpty()) {
            logger.info("Using service account credentials from: {}", credentialsPath);
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(new FileInputStream(credentialsPath));
            optionsBuilder.setCredentials(credentials);
        } else {
            logger.info("Using default application credentials (environment variable or metadata service)");
            // Will use GOOGLE_APPLICATION_CREDENTIALS environment variable or
            // metadata service if running on GCP
        }

        BigQuery bigQuery = optionsBuilder.build().getService();

        // Test connection
        try {
            bigQuery.getDataset(datasetId);
            logger.info("Successfully connected to BigQuery dataset: {}", datasetId);
        } catch (Exception e) {
            logger.warn("Could not verify BigQuery dataset access: {}", e.getMessage());
            logger.info("Dataset will be created if it doesn't exist during first operation");
        }

        return bigQuery;
    }
    
    /**
     * Getter for project ID
     */
    public String getProjectId() {
        return projectId;
    }
    
    /**
     * Getter for dataset ID  
     */
    public String getDatasetId() {
        return datasetId;
    }
    
    /**
     * Helper method to get fully qualified table name
     */
    public String getTableId(String tableName) {
        return String.format("%s.%s.%s", projectId, datasetId, tableName);
    }
}