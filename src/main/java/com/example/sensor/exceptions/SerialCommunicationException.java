package com.example.sensor.exceptions;

public class SerialCommunicationException extends RuntimeException {
    public SerialCommunicationException(String message) {
        super(message);
    }
}
