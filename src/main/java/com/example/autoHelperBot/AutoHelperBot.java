package com.example.autoHelperBot;

import com.example.autoHelperBot.service.Scheduler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.util.concurrent.TimeUnit;

@SpringBootApplication
@ComponentScan(basePackages = "com.example.autoHelperBot")
public class AutoHelperBot {
    public static void main(String[] args) {
        Scheduler scheduler = new Scheduler() {
        };
        SpringApplication.run(AutoHelperBot.class, args);
        scheduler.scheduleApiCallAtFixedRate(0, 1, TimeUnit.HOURS);
    }
}