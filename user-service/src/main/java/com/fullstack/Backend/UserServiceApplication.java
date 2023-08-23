package com.fullstack.Backend;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@EnableDiscoveryClient
@OpenAPIDefinition(info = @Info(title = "User API", version = "1.0", description = "Documentation User API v1.0 \n | Username: admin \n | Password: 12345678"))
public class UserServiceApplication {

    public static void main(String[] args) {

        try {
            SpringApplication app = new SpringApplication(UserServiceApplication.class);
            app.run(args);
        } catch(Throwable ex) {
            ex.printStackTrace();
        }
    }
}
