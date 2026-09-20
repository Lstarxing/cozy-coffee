package com.cozy.user;

import com.cozy.common.constant.InviteRewardConfig;
import com.cozy.common.constant.ProfileRewardConfig;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = { "com.cozy.user", "com.cozy.common" })
@EnableConfigurationProperties({ InviteRewardConfig.class, ProfileRewardConfig.class })
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
