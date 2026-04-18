package com.cozy.order;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = { "com.cozy" })
@EnableDubbo
@EnableScheduling
@MapperScan("com.cozy.order.mapper")
public class OrderProviderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderProviderApplication.class, args);
        System.out.println("========================================");
        System.out.println("  CozyCoffee Order Provider Started!   ");
        System.out.println("  Port: 8083 | Dubbo: 20883            ");
        System.out.println("  Database: cozy_order                 ");
        System.out.println("========================================");
    }
}
