package com.example.scheduler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppConfig(Availability availability) {
    public record Availability(int maxWindowDays) {
    }
}
