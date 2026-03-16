package com.seodashboard.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.seodashboard")
@EntityScan(basePackages = "com.seodashboard.common.domain")
@EnableJpaRepositories(basePackages = "com.seodashboard.api")
@org.springframework.scheduling.annotation.EnableAsync
public class SeoDashboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeoDashboardApplication.class, args);
    }
}
