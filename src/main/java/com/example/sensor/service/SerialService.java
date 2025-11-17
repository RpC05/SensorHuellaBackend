package com.example.sensor.service;

import java.util.List;

public interface SerialService {
    String sendCommand(String command) throws Exception;
    List<String> sendCommandWithProgress(String command) throws Exception;
    boolean isConnected();
}
