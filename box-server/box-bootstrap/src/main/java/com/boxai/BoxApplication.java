package com.boxai;

import com.boxai.bootstrap.config.LocalDotenv;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = "com.boxai")
@MapperScan("com.boxai.infrastructure.persistence.mapper")
@EnableTransactionManagement
public class BoxApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(BoxApplication.class);
        application.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) event ->
                LocalDotenv.apply(event.getEnvironment()));
        application.run(args);
    }
}
