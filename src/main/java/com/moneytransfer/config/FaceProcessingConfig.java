package com.moneytransfer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class FaceProcessingConfig {
    @Bean("faceExecutor")
    public Executor faceExecutor(@Value("${app.face.thread-pool-size:4}") int poolSize) {
        return Executors.newFixedThreadPool(poolSize);
    }
}
