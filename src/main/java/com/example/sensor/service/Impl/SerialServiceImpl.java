package com.example.sensor.service.Impl;

import com.example.sensor.config.SerialPortConfig;
import com.example.sensor.exceptions.SerialCommunicationException;
import com.example.sensor.service.SerialService;
import com.fazecast.jSerialComm.SerialPort;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @deprecated Esta implementación usa puerto serial COM y solo funciona en desarrollo local.
 * Para despliegue en la nube, usa {@link Esp32HttpServiceImpl} en su lugar.
 * 
 * Esta clase se mantiene para compatibilidad con desarrollo local cuando el ESP32
 * está conectado directamente por USB.
 */
@Deprecated(since = "1.0", forRemoval = false)
@Service
@Slf4j
@RequiredArgsConstructor
public class SerialServiceImpl implements SerialService {

    private final SerialPortConfig config;
    private SerialPort serialPort;
    private BufferedReader reader;
    private PrintWriter writer;

    @PostConstruct
    public void init() {
        try {
            log.info("Inicializando puerto serial: {}", config.getPort());

            serialPort = SerialPort.getCommPort(config.getPort());
            serialPort.setBaudRate(config.getBaudrate());
            
            // TIMEOUT LARGO SOLO PARA ESPERAR "READY:" (el ESP32 tarda hasta 3s en reiniciar)
            serialPort.setComPortTimeouts(
                    SerialPort.TIMEOUT_READ_BLOCKING,
                    5000, // 5 segundos para esperar READY: del ESP32
                    0
            );

            if (!serialPort.openPort()) {
                throw new SerialCommunicationException("No se pudo abrir el puerto serial");
            }

            // Prepara el reader y writer con encoding correcto
            reader = new BufferedReader(
                    new InputStreamReader(serialPort.getInputStream(), "US-ASCII")
            );
            writer = new PrintWriter(
                    serialPort.getOutputStream(),
                    true // auto-flush
            );

            log.info("Puerto serial abierto en {}. Esperando 'READY:' del ESP32...", config.getPort());
        
            // Esperar el mensaje READY
            String response = reader.readLine();
            
            if (response != null && response.startsWith("READY:")) {
                log.info("Arduino LISTO - Conexión establecida ({})", response.trim());
            } else {
                log.warn("Arduino no envió 'READY:'. Recibido: {}", response);
            }
            
            serialPort.setComPortTimeouts(
                    SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
                    10,
                    0
            );
            
            log.info("Timeout ajustado a 10ms para operacion normal");

        } catch (Exception e) {
            log.error("Error fatal inicializando puerto serial: {}", e.getMessage());
            cleanup();
            throw new SerialCommunicationException("Error al conectar: " + e.getMessage());
        }
    }

    @Override
    public String sendCommand(String command) throws Exception {
        if (!isConnected()) {
            throw new SerialCommunicationException("Puerto serial no conectado");
        }

        // Limpiar buffer antes de enviar comando
        while (serialPort.getInputStream().available() > 0) {
            serialPort.getInputStream().read();
        }

        log.debug("Enviando comando: {}", command);
        writer.println(command);
        writer.flush();

        StringBuilder response = new StringBuilder();
        String line;

        long startTime = System.currentTimeMillis();
        while ((System.currentTimeMillis() - startTime) < config.getTimeout()) {
            if (reader.ready()) {
                line = reader.readLine();
                if (line != null) {
                    log.debug("Arduino: {}", line);
                    response.append(line).append("\n");

                    // Detectar fin de respuesta
                    if (isEndOfResponse(line)) {
                        break;
                    }
                }
            }
            Thread.sleep(100);
        }

        String result = response.toString().trim();
        if (result.isEmpty()) {
            throw new SerialCommunicationException("Timeout esperando respuesta del sensor");
        }

        return result;
    }

    @Override
    public List<String> sendCommandWithProgress(String command) throws Exception {
        if (!isConnected()) {
            throw new SerialCommunicationException("Puerto serial no conectado");
        }

        while (serialPort.getInputStream().available() > 0) {
            serialPort.getInputStream().read();
        }

        log.info(">>> Enviando comando: [{}]", command);
        writer.println(command);
        writer.flush();
        log.info("Comando enviado, esperando respuesta...");

        List<String> messages = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        long lastDataTime = System.currentTimeMillis();
        
        final long MAX_IDLE_TIME = 30000;
        
        while ((System.currentTimeMillis() - startTime) < config.getTimeout()) {
            try {
                int available = serialPort.getInputStream().available();
                if (available > 0) {
                    lastDataTime = System.currentTimeMillis();
                    
                    if (reader.ready()) {
                        String line = reader.readLine();
                        if (line != null && !line.trim().isEmpty()) {
                            String cleanLine = line.trim();
                            
                            if (cleanLine.startsWith("READY:") || cleanLine.equals("SENSOR_OK") || cleanLine.equals("SENSOR_NOT_FOUND")) {
                                continue;
                            }
                            
                            log.info("<<< ESP32: {}", cleanLine);
                            messages.add(cleanLine);

                            if (isEndOfResponse(cleanLine)) {
                                log.info("Comando finalizado: {}", cleanLine);
                                break;
                            }
                        }
                    }
                } else {
                    long idleTime = System.currentTimeMillis() - lastDataTime;
                    if (idleTime > MAX_IDLE_TIME) {
                        log.warn("Timeout de inactividad: {} ms", idleTime);
                        break;
                    }
                    Thread.sleep(10);
                }
            } catch (IOException e) {
                log.error("Error leyendo respuesta: {}", e.getMessage());
                break;
            }
        }

        if (messages.isEmpty()) {
            log.error("TIMEOUT: No se recibio respuesta");
            throw new SerialCommunicationException("Timeout esperando respuesta del sensor");
        }

        return messages;
    }

    private boolean isEndOfResponse(String line) {
        if (line.startsWith("READY:") || line.equals("SENSOR_OK") || line.equals("SENSOR_NOT_FOUND") || line.equals("Waiting for finger")) {
            return false;
        }
        
        return line.equals("PONG") ||
                line.contains(" templates") ||
                line.equals("Database emptied!") ||
                line.equals("Could not clear database") ||
                line.equals("Deleted!") ||
                line.equals("Could not delete in that location") ||
                line.equals("Did not find a match") ||
                line.startsWith("Found ID #") ||
                line.equals("No finger detected") ||
                line.equals("Image too messy") ||
                line.equals("Unknown error") ||
                line.equals("Communication error") ||
                line.equals("Error writing to flash") ||
                line.equals("Could not store in that location") ||
                line.equals("Fingerprints did not match") ||
                line.equals("Could not find fingerprint features") ||
                line.equals("Imaging error") ||
                line.matches("\\d+");
    }

    @Override
    public boolean isConnected() {
        return serialPort != null && serialPort.isOpen();
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (serialPort != null && serialPort.isOpen()) {
                serialPort.closePort();
                log.info("Puerto serial cerrado");
            }
        } catch (Exception e) {
            log.error("Error cerrando puerto", e);
        }
    }
}
