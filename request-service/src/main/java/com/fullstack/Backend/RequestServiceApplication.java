package com.fullstack.Backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableAsync
@EnableCaching
@EnableTransactionManagement
@EnableDiscoveryClient
public class RequestServiceApplication {

    public static void main(String[] args) {

        try {
            SpringApplication app = new SpringApplication(RequestServiceApplication.class);
            app.run(args);
        } catch(Throwable ex) {
            ex.printStackTrace();
        }
    }
}