/**
 * ESP32 Unified Access Control - HTTP REST API Server
 * 
 * Sistema integrado que maneja:
 * - Sensor de huellas AS608
 * - Lector RFID MFRC522
 * - Display LCD I2C 16x2
 * 
 * Hardware:
 * - ESP32 DevKit v1
 * - Sensor AS608 (Serial2: GPIO 16=RX, GPIO 17=TX)
 * - MFRC522 RFID (SPI: GPIO 5=SS, GPIO 27=RST)
 * - LCD I2C 16x2 (dirección 0x27)
 * 
 * Endpoints Fingerprint:
 * - GET  /api/fingerprint/ping
 * - GET  /api/fingerprint/count
 * - POST /api/fingerprint/enroll
 * - POST /api/fingerprint/verify
 * - DELETE /api/fingerprint/{id}
 * - DELETE /api/fingerprint/empty
 * 
 * Endpoints RFID:
 * - GET  /api/rfid/ping
 * - POST /api/rfid/scan
 * 
 * @version 3.0 (Unified Version)
 */

#include <WiFi.h>
#include <WebServer.h>
#include <Adafruit_Fingerprint.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <ESPmDNS.h>
#include <SPI.h>
#include <MFRC522.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>

// ============================================
// CONFIGURACIÓN BACKEND
// ============================================
const char* BACKEND_URL = "https://nombre.onrender.com";
const char* ACCESS_ENDPOINT = "/api/v1/access/register";

// ============================================
// CONFIGURACIÓN WiFi
// ============================================
const char* WIFI_SSID = "***********";
const char* WIFI_PASSWORD = "**************";

// ============================================
// CONFIGURACIÓN PINES
// ============================================
// RFID
#define SS_PIN  5
#define RST_PIN 27

// ============================================
// INICIALIZACIÓN DE DISPOSITIVOS
// ============================================
WebServer server(80);
Adafruit_Fingerprint finger = Adafruit_Fingerprint(&Serial2);
MFRC522 rfid(SS_PIN, RST_PIN);
LiquidCrystal_I2C lcd(0x27, 16, 2);

// ============================================
// ESTRUCTURAS DE DATOS
// ============================================
struct EnrollState {
  bool inProgress = false;
  JsonDocument messages;
  int messagesCount = 0;
  uint8_t enrolledId = 0;
  
  void reset() {
    inProgress = false;
    messages.clear();
    messagesCount = 0;
    enrolledId = 0;
  }
  
  void addMessage(const char* msg) {
    messages[messagesCount++] = msg;
  }
};

EnrollState enrollState;

// Variables para control de RFID
unsigned long lastCardTime = 0;

// ============================================
// SETUP
// ============================================
void setup() {
  Serial.begin(115200);
  Serial.println("\n\n=== ESP32 Unified Access Control Server ===");
  
  // ===== INICIALIZAR LCD =====
  lcd.init();
  lcd.backlight();
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Iniciando...");
  
  // ===== INICIALIZAR SENSOR AS608 =====
  Serial2.begin(57600, SERIAL_8N1, 16, 17);
  Serial.print("Sensor AS608... ");
  if (finger.verifyPassword()) {
    Serial.println("OK");
    finger.getTemplateCount();
    Serial.printf("Templates: %d\n", finger.templateCount);
  } else {
    Serial.println("ERROR");
    lcd.setCursor(0, 1);
    lcd.print("AS608 ERROR");
    while (1) { delay(1); }
  }
  
  // ===== INICIALIZAR RFID =====
  SPI.begin();
  rfid.PCD_Init();
  Serial.println("RFID MFRC522... OK");
  
  // ===== CONECTAR WiFi =====
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("WiFi...");
  
  Serial.printf("\nConectando a: %s ", WIFI_SSID);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 30) {
    delay(500);
    Serial.print(".");
    attempts++;
  }
  
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("\nERROR WiFi");
    lcd.setCursor(0, 1);
    lcd.print("WiFi ERROR");
    while (1) { delay(1); }
  }
  
  Serial.println(" OK!");
  Serial.print("IP: ");
  Serial.println(WiFi.localIP());
  
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("IP:");
  lcd.setCursor(0, 1);
  lcd.print(WiFi.localIP());
  delay(3000);
  
  // ===== mDNS =====
  if (MDNS.begin("sensorupaoiot")) {
    Serial.println("mDNS: http://sensorupaoiot.local");
  }
  
  // ===== CONFIGURAR ENDPOINTS =====
  // Fingerprint
  server.on("/api/fingerprint/ping", HTTP_GET, handleFingerprintPing);
  server.on("/api/fingerprint/count", HTTP_GET, handleFingerprintCount);
  server.on("/api/fingerprint/enroll", HTTP_POST, handleFingerprintEnroll);
  server.on("/api/fingerprint/verify", HTTP_POST, handleFingerprintVerify);
  server.on("/api/fingerprint/empty", HTTP_DELETE, handleFingerprintEmpty);
  server.on("/api/fingerprint/*", HTTP_DELETE, handleDeleteFingerprint);
  
  // RFID
  server.on("/api/rfid/ping", HTTP_GET, handleRfidPing);
  server.on("/api/rfid/scan", HTTP_POST, handleRfidScan);
  
  server.onNotFound(handleNotFound);
  
  server.begin();
  Serial.println("Servidor HTTP iniciado");
  Serial.println("==========================================\n");
  
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Sistema listo");
  lcd.setCursor(0, 1);
  lcd.print("Esperando...");
}

// ============================================
// LOOP
// ============================================
void loop() {
  server.handleClient();
  
  // Auto-detección de tarjeta RFID (opcional, para mostrar en LCD)
  checkRfidCard();
}

// ============================================
// HANDLERS - FINGERPRINT
// ============================================

void handleFingerprintPing() {
  Serial.println("GET /api/fingerprint/ping");
  
  JsonDocument doc;
  doc["status"] = "ok";
  doc["device"] = "fingerprint";
  
  String response;
  serializeJson(doc, response);
  server.send(200, "application/json", response);
}

void handleFingerprintCount() {
  Serial.println("GET /api/fingerprint/count");
  
  finger.getTemplateCount();
  
  JsonDocument doc;
  doc["count"] = finger.templateCount;
  
  String response;
  serializeJson(doc, response);
  server.send(200, "application/json", response);
}

void handleFingerprintEnroll() {
  Serial.println("POST /api/fingerprint/enroll");
  
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Enrollando...");
  
  enrollState.reset();
  enrollState.inProgress = true;
  
  bool success = performEnroll();
  
  JsonDocument doc;
  
  if (success) {
    doc["status"] = "success";
    doc["id"] = enrollState.enrolledId;
    
    JsonArray messagesArray = doc["messages"].to<JsonArray>();
    for (int i = 0; i < enrollState.messagesCount; i++) {
      messagesArray.add(enrollState.messages[i].as<const char*>());
    }
    
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Huella OK");
    lcd.setCursor(0, 1);
    lcd.print("ID: ");
    lcd.print(enrollState.enrolledId);
    delay(2000);
  } else {
    doc["status"] = "error";
    doc["error"] = "Enroll failed";
    
    JsonArray messagesArray = doc["messages"].to<JsonArray>();
    for (int i = 0; i < enrollState.messagesCount; i++) {
      messagesArray.add(enrollState.messages[i].as<const char*>());
    }
    
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Error enroll");
    delay(2000);
  }
  
  String response;
  serializeJson(doc, response);
  
  server.send(success ? 200 : 400, "application/json", response);
  enrollState.reset();
  
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Sistema listo");
}

void handleFingerprintVerify() {
  Serial.println("POST /api/fingerprint/verify");
  
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Verificando...");
  
  JsonDocument doc;
  
  uint8_t p = finger.getImage();
  if (p != FINGERPRINT_OK) {
    doc["found"] = false;
    doc["message"] = "No finger detected";
    
    String response;
    serializeJson(doc, response);
    server.send(200, "application/json", response);
    
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("No detectado");
    delay(1000);
    return;
  }
  
  p = finger.image2Tz();
  if (p != FINGERPRINT_OK) {
    doc["found"] = false;
    doc["message"] = "Image too messy";
    
    String response;
    serializeJson(doc, response);
    server.send(200, "application/json", response);
    
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Imagen mala");
    delay(1000);
    return;
  }
  
  p = finger.fingerFastSearch();
  if (p == FINGERPRINT_OK) {
    doc["found"] = true;
    doc["id"] = finger.fingerID;
    doc["confidence"] = finger.confidence;
    
    char msg[100];
    sprintf(msg, "Found ID #%d with confidence of %d", finger.fingerID, finger.confidence);
    doc["message"] = msg;
    
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("ID: ");
    lcd.print(finger.fingerID);
    lcd.setCursor(0, 1);
    lcd.print("Conf: ");
    lcd.print(finger.confidence);
    delay(2000);
  } else {
    doc["found"] = false;
    doc["message"] = "Did not find a match";
    
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("No encontrado");
    delay(1000);
  }
  
  String response;
  serializeJson(doc, response);
  server.send(200, "application/json", response);
  
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Sistema listo");
}

void handleDeleteFingerprint() {
  String idParam = server.pathArg(0);
  uint8_t id = idParam.toInt();
  
  Serial.printf("DELETE /api/fingerprint/%d\n", id);
  
  uint8_t p = finger.deleteModel(id);
  
  if (p == FINGERPRINT_OK) {
    server.send(200, "application/json", "{\"status\":\"deleted\"}");
  } else {
    server.send(400, "application/json", "{\"status\":\"error\"}");
  }
}

void handleFingerprintEmpty() {
  Serial.println("DELETE /api/fingerprint/empty");
  
  uint8_t p = finger.emptyDatabase();
  
  if (p == FINGERPRINT_OK) {
    server.send(200, "application/json", "{\"status\":\"emptied\"}");
  } else {
    server.send(400, "application/json", "{\"status\":\"error\"}");
  }
}

// ============================================
// HANDLERS - RFID
// ============================================

void handleRfidPing() {
  Serial.println("GET /api/rfid/ping");
  
  JsonDocument doc;
  doc["status"] = "ok";
  doc["device"] = "rfid";
  
  String response;
  serializeJson(doc, response);
  server.send(200, "application/json", response);
}

void handleRfidScan() {
  Serial.println("POST /api/rfid/scan");
  
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Escanea tarjeta");
  
  JsonDocument doc;
  
  // Esperar tarjeta (timeout 5 segundos)
  unsigned long startTime = millis();
  bool cardDetected = false;
  
  while (millis() - startTime < 5000 && !cardDetected) {
    server.handleClient();
    
    if (rfid.PICC_IsNewCardPresent() && rfid.PICC_ReadCardSerial()) {
      cardDetected = true;
      break;
    }
    delay(50);
  }
  
  if (!cardDetected) {
    doc["success"] = false;
    doc["message"] = "No card detected";
    
    String response;
    serializeJson(doc, response);
    server.send(200, "application/json", response);
    
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Sin tarjeta");
    delay(1000);
    
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Sistema listo");
    return;
  }
  
  // Obtener UID
  String uid = "";
  for (byte i = 0; i < rfid.uid.size; i++) {
    if (i > 0) uid += ":";
    if (rfid.uid.uidByte[i] < 0x10) uid += "0";
    uid += String(rfid.uid.uidByte[i], HEX);
  }
  uid.toUpperCase();
  
  doc["success"] = true;
  doc["uid"] = uid;
  
  String response;
  serializeJson(doc, response);
  server.send(200, "application/json", response);
  
  Serial.printf("Tarjeta detectada: %s\n", uid.c_str());
  
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("UID:");
  lcd.setCursor(0, 1);
  lcd.print(uid.substring(0, 16));
  delay(2000);
  
  rfid.PICC_HaltA();
  rfid.PCD_StopCrypto1();
  
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Sistema listo");
}

// ============================================
// FUNCIÓN AUXILIAR RFID
// ============================================
void sendAccessToBackend(String cardUid) {
  HTTPClient http;
  
  String fullUrl = String(BACKEND_URL) + String(ACCESS_ENDPOINT);
  
  http.begin(fullUrl);
  http.addHeader("Content-Type", "application/json");
  
  JsonDocument doc;
  doc["cardUid"] = cardUid;
  doc["location"] = "Puerta Principal";
  doc["deviceId"] = "ESP32-001";
  
  String payload;
  serializeJson(doc, payload);
  
  Serial.printf("Enviando al backend: %s\n", payload.c_str());
  
  int httpCode = http.POST(payload);
  
  if (httpCode > 0) {
    String response = http.getString();
    Serial.printf("Backend respuesta [%d]: %s\n", httpCode, response.c_str());
    
    JsonDocument responseDoc;
    DeserializationError error = deserializeJson(responseDoc, response);
    
    if (!error) {
      bool authorized = responseDoc["authorized"] | false;
      String personName = responseDoc["personName"] | "Desconocido";
      
      lcd.clear();
      if (authorized) {
        lcd.setCursor(0, 0);
        lcd.print("ACCESO OK");
        lcd.setCursor(0, 1);
        lcd.print(personName.substring(0, 16));
      } else {
        lcd.setCursor(0, 0);
        lcd.print("ACCESO DENEGADO");
        lcd.setCursor(0, 1);
        lcd.print(personName.substring(0, 16));
      }
      delay(3000);
    }
  } else {
    Serial.printf("Error HTTP: %s\n", http.errorToString(httpCode).c_str());
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Error Backend");
    delay(2000);
  }
  
  http.end();
}

void checkRfidCard() {
  unsigned long currentTime = millis();
  
  // Reinicializar RFID cada 5 segundos si no hay actividad
  if (currentTime - lastCardTime > 5000) {
    rfid.PCD_Init();
    lastCardTime = currentTime;
  }
  
  // Verificar si hay tarjeta (sin bloquear)
  if (!rfid.PICC_IsNewCardPresent() || !rfid.PICC_ReadCardSerial())
    return;
  
  lastCardTime = currentTime;
  
  // Obtener UID para mostrar en LCD
  String uid = "";
  for (byte i = 0; i < rfid.uid.size; i++) {
    if (i > 0) uid += ":";
    if (rfid.uid.uidByte[i] < 0x10) uid += "0";
    uid += String(rfid.uid.uidByte[i], HEX);
  }
  uid.toUpperCase();
  
  Serial.printf("Tarjeta detectada: %s\n", uid.c_str());
  
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("RFID:");
  lcd.setCursor(0, 1);
  lcd.print(uid.substring(0, 16));
  
  delay(500);

  sendAccessToBackend(uid);
  
  rfid.PICC_HaltA();
  rfid.PCD_StopCrypto1();
  
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Sistema listo");
}

// ============================================
// FUNCIÓN ENROLL FINGERPRINT
// ============================================

bool performEnroll() {
  enrollState.addMessage("Waiting for valid finger to enroll");
  
  lcd.setCursor(0, 1);
  lcd.print("Coloca dedo 1");
  
  int p = -1;
  while (p != FINGERPRINT_OK) {
    server.handleClient();
    p = finger.getImage();
    if (p == FINGERPRINT_NOFINGER) {
      delay(50);
      continue;
    } else if (p == FINGERPRINT_OK) {
      enrollState.addMessage("Image taken");
      break;
    } else {
      enrollState.addMessage("Imaging error");
      return false;
    }
  }
  
  p = finger.image2Tz(1);
  if (p != FINGERPRINT_OK) {
    enrollState.addMessage("Image too messy");
    return false;
  }
  enrollState.addMessage("Image converted");
  
  lcd.setCursor(0, 1);
  lcd.print("Remueve dedo  ");
  enrollState.addMessage("Remove finger");
  delay(2000);
  
  p = 0;
  while (p != FINGERPRINT_NOFINGER) {
    server.handleClient();
    p = finger.getImage();
    delay(50);
  }
  
  lcd.setCursor(0, 1);
  lcd.print("Coloca dedo 2");
  enrollState.addMessage("Place same finger again");
  
  p = -1;
  while (p != FINGERPRINT_OK) {
    server.handleClient();
    p = finger.getImage();
    if (p == FINGERPRINT_NOFINGER) {
      delay(50);
      continue;
    } else if (p == FINGERPRINT_OK) {
      enrollState.addMessage("Image taken");
      break;
    } else {
      enrollState.addMessage("Imaging error");
      return false;
    }
  }
  
  p = finger.image2Tz(2);
  if (p != FINGERPRINT_OK) {
    enrollState.addMessage("Image too messy");
    return false;
  }
  enrollState.addMessage("Image converted");
  
  enrollState.addMessage("Creating model");
  
  p = finger.createModel();
  if (p == FINGERPRINT_OK) {
    enrollState.addMessage("Prints matched!");
  } else if (p == FINGERPRINT_ENROLLMISMATCH) {
    enrollState.addMessage("Fingerprints did not match");
    return false;
  } else {
    enrollState.addMessage("Unknown error");
    return false;
  }
  
  finger.getTemplateCount();
  uint8_t id = finger.templateCount + 1;
  
  char idMsg[20];
  sprintf(idMsg, "ID %d", id);
  enrollState.addMessage(idMsg);
  
  p = finger.storeModel(id);
  if (p == FINGERPRINT_OK) {
    enrollState.addMessage("Stored!");
    enrollState.enrolledId = id;
    return true;
  } else {
    enrollState.addMessage("Error writing to flash");
    return false;
  }
}

// ============================================
// HANDLER 404
// ============================================

void handleNotFound() {
  Serial.printf("404: %s\n", server.uri().c_str());
  
  JsonDocument doc;
  doc["error"] = "Not Found";
  doc["path"] = server.uri();
  
  String response;
  serializeJson(doc, response);
  
  server.send(404, "application/json", response);
}
