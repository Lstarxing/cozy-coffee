package com.cozy.mall;

import com.cozy.common.constant.CouponTemplateConfig;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = { "com.cozy" })
@EnableConfigurationProperties(CouponTemplateConfig.class)
@EnableDubbo
@MapperScan("com.cozy.mall.mapper")
public class MallProviderApplication {
    public static void main(String[] args) {
        SpringApplication.run(MallProviderApplication.class, args);
        System.out.println("========================================");
        System.out.println("  CozyCoffee Mall Provider Started!    ");
        System.out.println("  Port: 8084 | Dubbo: 20884            ");
        System.out.println("  Database: cozy_mall                  ");
        System.out.println("========================================");
    }
}
