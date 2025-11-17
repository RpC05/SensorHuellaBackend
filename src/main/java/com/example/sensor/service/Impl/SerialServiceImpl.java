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
            
            serialPort.setComPortTimeouts(
                    SerialPort.TIMEOUT_READ_BLOCKING,
                    1000,
                    0
            );

            if (!serialPort.openPort()) {
                throw new SerialCommunicationException("No se pudo abrir el puerto serial");
            }

            // Dar un pequeño delay después de abrir el puerto
            Thread.sleep(100);

            // Prepara el reader y writer con encoding correcto
            reader = new BufferedReader(
                    new InputStreamReader(serialPort.getInputStream(), "US-ASCII")
            );
            writer = new PrintWriter(
                    serialPort.getOutputStream(),
                    true // auto-flush
            );

            log.info("✓ Puerto serial abierto correctamente en {}", config.getPort());
            
            // Limpiar mensajes de inicio del ESP32 inmediatamente
            log.info("Limpiando buffer inicial...");
            Thread.sleep(20); // Solo para que lleguen los mensajes del bootloader
            int bytesDiscarded = 0;
            while (serialPort.getInputStream().available() > 0) {
                serialPort.getInputStream().read();
                bytesDiscarded++;
            }
            log.info("Buffer limpiado: {} bytes", bytesDiscarded);
            
            log.info("✓ Listo - sensor responde en 50ms");

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
        log.debug("Limpiando buffer...");
        int bytesCleared = 0;
        while (serialPort.getInputStream().available() > 0) {
            serialPort.getInputStream().read();
            bytesCleared++;
        }
        if (bytesCleared > 0) {
            log.debug("Buffer limpiado: {} bytes descartados", bytesCleared);
        }

        log.info(">>> Enviando comando: [{}]", command);
        
        // Enviar con más detalle de debug
        String fullCommand = command + "\n";
        byte[] commandBytes = fullCommand.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        log.info("Bytes a enviar: {} bytes = {}", commandBytes.length, java.util.Arrays.toString(commandBytes));
        
        writer.print(fullCommand);
        writer.flush();
        
        log.info("✓ Comando enviado, esperando respuesta...");

        List<String> messages = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        int checksWithoutData = 0;
        
        while ((System.currentTimeMillis() - startTime) < config.getTimeout()) {
            try {
                int available = serialPort.getInputStream().available();
                if (available > 0) {
                    log.info("Datos detectados: {} bytes disponibles", available);
                    String line = reader.readLine();
                    if (line != null && !line.trim().isEmpty()) {
                        String cleanLine = line.replaceAll("[^\\x20-\\x7E:]", "").trim();
                        if (!cleanLine.isEmpty()) {
                            log.info("<<< ESP32: {}", cleanLine);
                            messages.add(cleanLine);

                            if (cleanLine.startsWith("SUCCESS:") || cleanLine.startsWith("ERROR:")) {
                                log.info("✓ Comando finalizado: {}", cleanLine);
                                break;
                            }
                        }
                    }
                } else {
                    checksWithoutData++;
                    if (checksWithoutData % 100 == 0) {
                        log.debug("Esperando datos... ({} checks sin respuesta)", checksWithoutData);
                    }
                }
                Thread.sleep(10);
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
