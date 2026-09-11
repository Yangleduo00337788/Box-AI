package com.boxai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = "com.boxai")
@MapperScan("com.boxai.infrastructure.persistence.mapper")
@EnableTransactionManagement
public class BoxApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoxApplication.class, args);
    }
}
