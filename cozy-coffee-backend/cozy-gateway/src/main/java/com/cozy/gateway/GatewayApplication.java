package com.cozy.gateway;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = { "com.cozy.gateway", "com.cozy.common" }, exclude = {
        DataSourceAutoConfiguration.class })
@EnableDubbo
@EnableScheduling
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("=================================");
        System.out.println("  Cozy Coffee Gateway Started!  ");
        System.out.println("  Port: 8080                    ");
        System.out.println("=================================");
    }
}
