package com.example.sensor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "fingerprint.serial")
@Data
public class SerialPortConfig {
    private String port = "COM5";
    private Integer baudrate = 9600;
    private Integer timeout = 30000;
}