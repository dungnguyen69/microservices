package com.backendserver.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity serverHttpSecurity) {
//        serverHttpSecurity.csrf(ServerHttpSecurity.CsrfSpec::disable).authorizeExchange(
//                exchange -> exchange.pathMatchers("/eureka/**", "/api/users/**", "/api/devices/**", "/api/requests/**",
//                        "/api/keeper-orders/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll());
        serverHttpSecurity.csrf().disable().httpBasic().disable();

        //https://github.com/spring-projects/spring-security/issues/13446
        return serverHttpSecurity.build();
    }
}
