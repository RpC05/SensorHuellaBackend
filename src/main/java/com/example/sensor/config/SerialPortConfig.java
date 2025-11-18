package com.example.sensor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @deprecated Esta configuración es solo para desarrollo local con puerto serial COM.
 * Para despliegue en la nube, usa {@link Esp32Config} en su lugar.
 */
@Deprecated(since = "1.0", forRemoval = false)
@Configuration
@ConfigurationProperties(prefix = "fingerprint.serial")
@Data
public class SerialPortConfig {
    private String port = "COM5";
    private Integer baudrate = 9600;
    private Integer timeout = 30000;
}