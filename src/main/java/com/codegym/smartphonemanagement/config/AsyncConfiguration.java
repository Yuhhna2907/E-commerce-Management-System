package com.codegym.smartphonemanagement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous processing
 * Provides thread pool configuration for webhook processing and other async tasks
 */
@Configuration
@EnableAsync
public class AsyncConfiguration {

    @Value("${app.payment.webhook.async-pool-size:10}")
    private int webhookPoolSize;

    /**
     * Creates thread pool executor for webhook processing
     * Configured for high-throughput webhook processing with bounded queue
     * 
     * @return ThreadPoolTaskExecutor configured for webhook processing
     */
    @Bean(name = "webhookExecutor")
    public Executor webhookExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core pool size - number of threads to keep alive
        executor.setCorePoolSize(webhookPoolSize);
        
        // Maximum pool size - maximum number of threads
        executor.setMaxPoolSize(webhookPoolSize * 2);
        
        // Queue capacity - number of tasks to queue when all threads are busy
        executor.setQueueCapacity(100);
        
        // Thread name prefix for easier debugging
        executor.setThreadNamePrefix("webhook-");
        
        // Reject policy - caller runs the task if queue is full
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        
        return executor;
    }
}