package com.cozy.member;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = { "com.cozy.member", "com.cozy.common" })
@EnableDubbo
@MapperScan("com.cozy.member.mapper")
public class MemberProviderApplication {
    public static void main(String[] args) {
        SpringApplication.run(MemberProviderApplication.class, args);
        System.out.println("=================================");
        System.out.println("  Cozy Member Provider Started!  ");
        System.out.println("  Port: 8082                     ");
        System.out.println("=================================");
    }
}
