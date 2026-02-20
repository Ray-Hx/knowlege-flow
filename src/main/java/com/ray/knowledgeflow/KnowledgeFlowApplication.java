package com.ray.knowledgeflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@org.springframework.context.annotation.ComponentScan(basePackages = "com.ray.knowledgeflow")
@EnableScheduling
@EnableAsync
public class KnowledgeFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeFlowApplication.class, args);
    }

}
