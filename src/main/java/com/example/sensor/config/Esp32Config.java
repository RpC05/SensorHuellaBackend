package com.example.sensor.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "esp32")
@Getter
@Setter
public class Esp32Config {
    
    /**
     * URL base del ESP32 (puede ser túnel de Cloudflare, ngrok o IP local)
     * Ejemplo: https://mi-sensor.trycloudflare.com
     * o http://192.168.1.100 para desarrollo local
     */
    private String baseUrl = "http://localhost";
    
    /**
     * Timeout de conexión en milisegundos (5 segundos por defecto)
     */
    private int connectionTimeout = 5000;
    
    /**
     * Timeout de lectura en milisegundos (30 segundos por defecto)
     * Debe ser largo porque enrollar huella puede tardar
     */
    private int readTimeout = 30000;
    
    /**
     * Timeout para comandos rápidos (ping, count) en milisegundos
     */
    private int quickTimeout = 5000;
    
    /**
     * Número de reintentos en caso de fallo
     */
    private int maxRetries = 3;
    
    /**
     * Habilitar logs de debug de las peticiones HTTP
     */
    private boolean debugEnabled = false;

    @Bean
    public WebClient esp32WebClient() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(readTimeout));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
