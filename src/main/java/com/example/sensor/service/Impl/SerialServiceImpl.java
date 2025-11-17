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
            // 1. Usamos el Timeout de tu config (que es 30000ms)
            //    Si no, usa el de 10s que tenías (10000). 30s es más seguro.
            serialPort.setComPortTimeouts(
                    SerialPort.TIMEOUT_READ_BLOCKING,
                    1000, // 30000 ms
                    0
            );

            if (!serialPort.openPort()) {
                throw new SerialCommunicationException("No se pudo abrir el puerto serial");
            }

            log.info("Puerto serial abierto. Esperando el saludo 'READY:' del ESP32...");

            // Prepara el reader y writer
            reader = new BufferedReader(
                    new InputStreamReader(serialPort.getInputStream())
            );
            writer = new PrintWriter(
                    serialPort.getOutputStream(),
                    true
            );

            // --- Lógica robusta para el Handshake ---

            long startTime = System.currentTimeMillis();
            long timeoutMillis = config.getTimeout(); // 30000 ms
            boolean isReady = false;

            log.info("Puerto serial abierto. Esperando 'READY:' del ESP32 (max {}s)...", timeoutMillis / 1000);

            // Bucle para esperar el "READY:"
            while (!isReady && (System.currentTimeMillis() - startTime) < timeoutMillis) {
                try {
                    // readLine() esperará MÁXIMO 1 segundo (por el timeout que pusimos)
                    String response = reader.readLine();

                    if (response != null) {
                        log.debug("Datos recibidos del ESP32: {}", response); // Loguea todo

                        if (response.startsWith("READY:")) {
                            log.info("¡Arduino está LISTO! Conexión establecida. ({})", response);
                            isReady = true; // ¡Éxito! Salimos del bucle
                        } else {
                            // Es basura de bootloader u otro mensaje (SENSOR_OK, etc.)
                            log.warn("Ignorando línea (no es READY): {}", response);
                            // El bucle continuará y leerá la siguiente línea
                        }
                    }
                } catch (Exception e) {
                    // Esto puede pasar si el timeout de 1s se cumple y no hay nada
                    // No es un error, solo significa "nada que leer, sigue intentando"
                    log.trace("Timeout de lectura, reintentando...");
                }
            }

            // Si salimos del bucle sin éxito, es un error de timeout.
            if (!isReady) {
                log.error("Timeout: Arduino no envió 'READY:' en {} segundos.", timeoutMillis / 1000);
                throw new SerialCommunicationException("No se recibió 'READY:' del Arduino (timeout).");
            }

        } catch (Exception e) {
            log.error("Error fatal inicializando puerto serial: {}", e.getMessage());
            cleanup(); // Cierra el puerto si algo falla
            throw new SerialCommunicationException("Error al conectar: " + e.getMessage());
        }
    }

    private void waitForReady() throws Exception {
        String response;
        long startTime = System.currentTimeMillis();

        while ((System.currentTimeMillis() - startTime) < 5000) {
            if (reader.ready()) {
                response = reader.readLine();
                log.info("Arduino: {}", response);
                if (response != null && response.startsWith("READY:")) {
                    log.info("Arduino listo");
                    return;
                }
            }
            Thread.sleep(100);
        }
    }

    @Override
    public String sendCommand(String command) throws Exception {
        if (!isConnected()) {
            throw new SerialCommunicationException("Puerto serial no conectado");
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

        log.debug("Enviando comando con progreso: {}", command);
        writer.println(command);
        writer.flush();

        List<String> messages = new ArrayList<>();
        String line;

        long startTime = System.currentTimeMillis();
        while ((System.currentTimeMillis() - startTime) < config.getTimeout()) {
            if (reader.ready()) {
                line = reader.readLine();
                if (line != null) {
                    log.debug("Arduino: {}", line);
                    messages.add(line);

                    if (line.startsWith("SUCCESS:") || line.startsWith("ERROR:")) {
                        break;
                    }
                }
            }
            Thread.sleep(100);
        }

        if (messages.isEmpty()) {
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
