package com.cozy.member;

import com.cozy.common.constant.BirthdayRewardConfig;
import com.cozy.common.constant.FirstOrderRewardConfig;
import com.cozy.common.constant.MemberLevelConfig;
import com.cozy.common.constant.MonthlyChallengeConfig;
import com.cozy.common.constant.SigninRewardConfig;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = { "com.cozy.member", "com.cozy.common" })
@EnableConfigurationProperties({ MemberLevelConfig.class, MonthlyChallengeConfig.class,
        BirthdayRewardConfig.class, SigninRewardConfig.class, FirstOrderRewardConfig.class })
@EnableDubbo
@MapperScan("com.cozy.member.mapper")
@org.springframework.scheduling.annotation.EnableScheduling
public class MemberProviderApplication {
    public static void main(String[] args) {
        SpringApplication.run(MemberProviderApplication.class, args);
        System.out.println("=================================");
        System.out.println("  Cozy Member Provider Started!  ");
        System.out.println("  Port: 8082                     ");
        System.out.println("=================================");
    }
}
