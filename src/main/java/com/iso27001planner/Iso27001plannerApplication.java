package com.iso27001planner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Iso27001plannerApplication {

    public static void main(String[] args) {
        SpringApplication.run(Iso27001plannerApplication.class, args);
    }

}
