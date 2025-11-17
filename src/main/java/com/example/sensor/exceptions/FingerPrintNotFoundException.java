package com.example.sensor.exceptions;

public class FingerPrintNotFoundException extends RuntimeException {
    public FingerPrintNotFoundException(Integer id) {
        super("Huella no encontrada con ID: " + id);
    }
}
