package com.fullstack.Backend;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@EnableCaching
@EnableDiscoveryClient
@OpenAPIDefinition(info = @Info(title = "Request API", version = "1.0", description = "Documentation Request API v1.0"))
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
