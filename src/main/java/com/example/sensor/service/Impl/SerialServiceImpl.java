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
            
            // Evita que los enrollments sean lentos
            serialPort.setComPortTimeouts(
                    SerialPort.TIMEOUT_READ_BLOCKING,
                    1000, // 1 segundo para operación normal
                    0
            );
            
            log.info("Timeout ajustado a 1s para operación normal");

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

        // Limpiar buffer antes de enviar comando
        log.info("Limpiando buffer antes de comando...");
        int bytesCleared = 0;
        
        // Leer todo lo que haya en el buffer hasta 3 veces
        for (int i = 0; i < 3; i++) {
            while (serialPort.getInputStream().available() > 0) {
                serialPort.getInputStream().read();
                bytesCleared++;
            }
            try { 
                Thread.sleep(50); 
            } catch (InterruptedException e) {}
        }
        
        if (bytesCleared > 0) {
            log.warn("Buffer tenía {} bytes antiguos - descartados", bytesCleared);
        }

        log.info(">>> Enviando comando: [{}]", command);
        
        // Enviar con más detalle de debug
        String fullCommand = command + "\n";
        byte[] commandBytes = fullCommand.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        log.info("Bytes a enviar: {} bytes = {}", commandBytes.length, java.util.Arrays.toString(commandBytes));
        
        writer.print(fullCommand);
        writer.flush();
        
        log.info("Comando enviado, esperando respuesta...");

        List<String> messages = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        long lastDataTime = System.currentTimeMillis();
        
        // Permitir hasta 40s de silencio entre mensajes (el ESP32 tarda en extraer y enviar template)
        final long MAX_IDLE_TIME = 40000;
        
        while ((System.currentTimeMillis() - startTime) < config.getTimeout()) {
            try {
                int available = serialPort.getInputStream().available();
                if (available > 0) {
                    lastDataTime = System.currentTimeMillis();
                    
                    String line = reader.readLine();
                    if (line != null && !line.trim().isEmpty()) {
                        String cleanLine = line.replaceAll("[^\\x20-\\x7E:]", "").trim();
                        if (!cleanLine.isEmpty()) {
                            // Log diferente para TEMPLATE (es muy largo)
                            if (cleanLine.startsWith("TEMPLATE:")) {
                                int templateLength = cleanLine.length() - 9;
                                log.info("<<< ESP32: TEMPLATE: {} caracteres hex", templateLength);
                            } else {
                                log.info("<<< ESP32: {}", cleanLine);
                            }
                            
                            messages.add(cleanLine);

                            if (cleanLine.startsWith("SUCCESS:") || cleanLine.startsWith("ERROR:")) {
                                log.info("Comando finalizado: {}", cleanLine);
                                break;
                            }
                        }
                    }
                } else {
                    // Sin datos - verificar timeout de inactividad
                    long idleTime = System.currentTimeMillis() - lastDataTime;
                    if (idleTime > MAX_IDLE_TIME) {
                        log.warn("Timeout de inactividad: {} ms sin datos desde último mensaje", idleTime);
                        break;
                    }
                }
                Thread.sleep(50); // Aumentado a 50ms para reducir CPU
            } catch (IOException e) {
                log.error("Error leyendo respuesta: {}", e.getMessage());
                break;
            }
        }

        if (messages.isEmpty()) {
            log.error("TIMEOUT: No se recibió respuesta del ESP32 en {} ms", config.getTimeout());
            throw new SerialCommunicationException("Timeout esperando respuesta del sensor");
        }

        return messages;
    }

    private boolean isEndOfResponse(String line) {
        return line.startsWith("SUCCESS:") ||
                line.startsWith("ERROR:") ||
                line.startsWith("VERIFIED:") ||
                line.equals("NOT_FOUND") ||
                line.startsWith("COUNT:") ||
                line.equals("PONG") ||
                line.startsWith("TEMPLATE:");
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
