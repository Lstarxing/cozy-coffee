package com.cozy.user;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// 本模块不再有 @ConfigurationProperties 绑定（`cozy.user.profile` 已随 ProfileRewardConfig 迁到消费端），
// 故不再需要 @EnableConfigurationProperties
@SpringBootApplication(scanBasePackages = { "com.cozy.user", "com.cozy.common" })
@EnableDubbo
@EnableScheduling
@MapperScan("com.cozy.user.mapper")
public class UserProviderApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserProviderApplication.class, args);
        System.out.println("=================================");
        System.out.println("  Cozy User Provider Started!   ");
        System.out.println("  Port: 8081                    ");
        System.out.println("=================================");
    }
}
