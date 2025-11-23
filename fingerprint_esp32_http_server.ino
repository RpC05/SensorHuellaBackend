/**
 * ESP32 Fingerprint Sensor - HTTP REST API Server
 * 
 * Este código convierte el ESP32 en un servidor REST que expone el sensor AS608
 * mediante endpoints HTTP para poder ser consumido desde la nube.
 * 
 * Hardware:
 * - ESP32 DevKit v1 (o similar)
 * - Sensor AS608 conectado a Serial2 (GPIO 16=RX, GPIO 17=TX)
 * 
 * Endpoints:
 * - GET  /api/ping        -> Health check
 * - GET  /api/count       -> Obtener número de huellas
 * - POST /api/enroll      -> Enrollar nueva huella
 * - POST /api/verify      -> Verificar huella
 * - DELETE /api/fingerprint/{id} -> Eliminar huella
 * - DELETE /api/empty     -> Vaciar base de datos
 * 
 * @author SensorHuellaBackend
 * @version 2.0 (HTTP Version)
 */

#include <WiFi.h>
#include <WebServer.h>
#include <Adafruit_Fingerprint.h>
#include <ArduinoJson.h>
#include <ESPmDNS.h>

// ============================================
// CONFIGURACIÓN WiFi (MODIFICAR CON TUS DATOS)
// ============================================
const char* WIFI_SSID = "*********";        // Cambia esto
const char* WIFI_PASSWORD = "**********";    // Cambia esto

// ============================================
// CONFIGURACIÓN DEL SERVIDOR
// ============================================
const int HTTP_PORT = 80;

// ============================================
// INICIALIZACIÓN
// ============================================
WebServer server(HTTP_PORT);
Adafruit_Fingerprint finger = Adafruit_Fingerprint(&Serial2);

// Variables globales para tracking de enroll
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

// ============================================
// SETUP
// ============================================
void setup() {
  Serial.begin(115200);
  Serial.println("\n\n=== ESP32 Fingerprint HTTP Server ===");
  
  // Inicializar sensor AS608
  Serial2.begin(57600, SERIAL_8N1, 16, 17);
  
  Serial.print("Inicializando sensor AS608... ");
  if (finger.verifyPassword()) {
    Serial.println("OK");
    finger.getTemplateCount();
    Serial.printf("Sensor contiene %d templates\n", finger.templateCount);
  } else {
    Serial.println("ERROR");
    Serial.println("No se encontró el sensor. Verifica la conexión.");
    while (1) { delay(1); }
  }
  
  // Conectar a WiFi
  Serial.printf("\nConectando a WiFi: %s ", WIFI_SSID);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 30) {
    delay(500);
    Serial.print(".");
    attempts++;
  }
  
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("\nERROR: No se pudo conectar a WiFi");
    Serial.println("Verifica SSID y password");
    while (1) { delay(1); }
  }

  if (MDNS.begin("sensorupaoiot")) { // El nombre del dispositivo será "sensor"
    Serial.println("mDNS iniciado. Hostname: http://sensorupaoiot.local");
  } else {
    Serial.println("Error iniciando mDNS");
  }
  
  Serial.println(" Conectado!");
  Serial.print("IP Address: ");
  Serial.println(WiFi.localIP());
  
  // Configurar endpoints REST
  server.on("/api/ping", HTTP_GET, handlePing);
  server.on("/api/count", HTTP_GET, handleCount);
  server.on("/api/enroll", HTTP_POST, handleEnroll);
  server.on("/api/verify", HTTP_POST, handleVerify);
  server.on("/api/empty", HTTP_DELETE, handleEmpty);
  
  // Endpoint con parámetro de ruta para DELETE
  server.on("/api/fingerprint/*", HTTP_DELETE, handleDeleteFingerprint);
  
  // Endpoint para 404
  server.onNotFound(handleNotFound);
  
  // Iniciar servidor
  server.begin();
  Serial.printf("Servidor HTTP iniciado en puerto %d\n", HTTP_PORT);
  Serial.println("==========================================\n");
}

// ============================================
// LOOP
// ============================================
void loop() {
  server.handleClient();
}

// ============================================
// HANDLERS DE ENDPOINTS
// ============================================

/**
 * GET /api/ping
 * Health check simple
 */
void handlePing() {
  Serial.println("GET /api/ping");
  
  JsonDocument doc;
  doc["status"] = "ok";
  doc["message"] = "PONG";
  
  String response;
  serializeJson(doc, response);
  
  server.send(200, "application/json", response);
}

/**
 * GET /api/count
 * Retorna el número de huellas almacenadas
 */
void handleCount() {
  Serial.println("GET /api/count");
  
  finger.getTemplateCount();
  
  JsonDocument doc;
  doc["count"] = finger.templateCount;
  
  String response;
  serializeJson(doc, response);
  
  Serial.printf("Respuesta: %d templates\n", finger.templateCount);
  server.send(200, "application/json", response);
}

/**
 * POST /api/enroll
 * Enrolla una nueva huella (proceso completo)
 */
void handleEnroll() {
  Serial.println("POST /api/enroll");
  
  enrollState.reset();
  enrollState.inProgress = true;
  
  // Ejecutar proceso de enroll
  bool success = performEnroll();
  
  JsonDocument doc;
  
  if (success) {
    doc["status"] = "success";
    doc["id"] = enrollState.enrolledId;
    
    // Copiar mensajes
    JsonArray messagesArray = doc["messages"].to<JsonArray>();
    for (int i = 0; i < enrollState.messagesCount; i++) {
      messagesArray.add(enrollState.messages[i].as<const char*>());
    }
  } else {
    doc["status"] = "error";
    doc["error"] = "Enroll failed";
    
    JsonArray messagesArray = doc["messages"].to<JsonArray>();
    for (int i = 0; i < enrollState.messagesCount; i++) {
      messagesArray.add(enrollState.messages[i].as<const char*>());
    }
  }
  
  String response;
  serializeJson(doc, response);
  
  server.send(success ? 200 : 400, "application/json", response);
  enrollState.reset();
}

/**
 * POST /api/verify
 * Verifica una huella contra la base de datos
 */
void handleVerify() {
  Serial.println("POST /api/verify");
  
  JsonDocument doc;
  
  // Tomar imagen
  uint8_t p = finger.getImage();
  if (p != FINGERPRINT_OK) {
    doc["found"] = false;
    doc["message"] = "No finger detected";
    
    String response;
    serializeJson(doc, response);
    server.send(200, "application/json", response);
    return;
  }
  
  Serial.println("Image taken");
  
  // Convertir imagen
  p = finger.image2Tz();
  if (p != FINGERPRINT_OK) {
    doc["found"] = false;
    doc["message"] = "Image too messy";
    
    String response;
    serializeJson(doc, response);
    server.send(200, "application/json", response);
    return;
  }
  
  Serial.println("Image converted");
  
  // Buscar coincidencia
  p = finger.fingerFastSearch();
  if (p == FINGERPRINT_OK) {
    doc["found"] = true;
    doc["id"] = finger.fingerID;
    doc["confidence"] = finger.confidence;
    
    char msg[100];
    sprintf(msg, "Found ID #%d with confidence of %d", finger.fingerID, finger.confidence);
    doc["message"] = msg;
    
    Serial.println(msg);
  } else if (p == FINGERPRINT_NOTFOUND) {
    doc["found"] = false;
    doc["message"] = "Did not find a match";
    Serial.println("No match found");
  } else {
    doc["found"] = false;
    doc["message"] = "Unknown error";
    Serial.println("Verify error");
  }
  
  String response;
  serializeJson(doc, response);
  server.send(200, "application/json", response);
}

/**
 * DELETE /api/fingerprint/{id}
 * Elimina una huella por ID
 */
void handleDeleteFingerprint() {
  String idParam = server.pathArg(0);
  uint8_t id = idParam.toInt();
  
  Serial.printf("DELETE /api/fingerprint/%d\n", id);
  
  JsonDocument doc;
  
  uint8_t p = finger.deleteModel(id);
  
  if (p == FINGERPRINT_OK) {
    doc["status"] = "deleted";
    doc["message"] = "Deleted!";
    Serial.printf("ID %d deleted\n", id);
    server.send(200, "application/json", "Deleted!");
  } else {
    doc["status"] = "error";
    doc["message"] = "Could not delete in that location";
    
    String response;
    serializeJson(doc, response);
    server.send(400, "application/json", response);
  }
}

/**
 * DELETE /api/empty
 * Vacía toda la base de datos del sensor
 */
void handleEmpty() {
  Serial.println("DELETE /api/empty");
  
  JsonDocument doc;
  
  uint8_t p = finger.emptyDatabase();
  
  if (p == FINGERPRINT_OK) {
    doc["status"] = "emptied";
    doc["message"] = "Database emptied!";
    Serial.println("Database emptied");
    server.send(200, "application/json", "Database emptied!");
  } else {
    doc["status"] = "error";
    doc["message"] = "Could not clear database";
    
    String response;
    serializeJson(doc, response);
    server.send(400, "application/json", response);
  }
}

/**
 * 404 Handler
 */
void handleNotFound() {
  Serial.printf("404: %s\n", server.uri().c_str());
  
  JsonDocument doc;
  doc["error"] = "Not Found";
  doc["path"] = server.uri();
  
  String response;
  serializeJson(doc, response);
  
  server.send(404, "application/json", response);
}

// ============================================
// FUNCIONES DE LÓGICA DEL SENSOR
// ============================================

/**
 * Ejecuta el proceso completo de enroll
 * Retorna true si fue exitoso, false si falló
 */
bool performEnroll() {
  enrollState.addMessage("Waiting for valid finger to enroll");
  Serial.println("Esperando primer dedo...");
  
  // Primera imagen
  int p = -1;
  while (p != FINGERPRINT_OK) {
    server.handleClient(); // Mantener servidor vivo durante espera
    p = finger.getImage();
    if (p == FINGERPRINT_NOFINGER) {
      delay(50);
      continue;
    } else if (p == FINGERPRINT_OK) {
      enrollState.addMessage("Image taken");
      Serial.println("Primera imagen capturada");
      break;
    } else {
      enrollState.addMessage("Imaging error");
      return false;
    }
  }
  
  // Convertir primera imagen
  p = finger.image2Tz(1);
  if (p != FINGERPRINT_OK) {
    enrollState.addMessage("Image too messy");
    return false;
  }
  enrollState.addMessage("Image converted");
  
  // Solicitar remover dedo
  enrollState.addMessage("Remove finger");
  Serial.println("Remueva el dedo...");
  delay(20);
  
  p = 0;
  while (p != FINGERPRINT_NOFINGER) {
    server.handleClient(); // Mantener servidor vivo durante espera
    p = finger.getImage();
    delay(50);
  }
  
  // Solicitar segundo escaneo
  enrollState.addMessage("Place same finger again");
  Serial.println("Esperando segundo dedo...");
  
  p = -1;
  while (p != FINGERPRINT_OK) {
    server.handleClient(); // Mantener servidor vivo durante espera
    p = finger.getImage();
    if (p == FINGERPRINT_NOFINGER) {
      delay(50);
      continue;
    } else if (p == FINGERPRINT_OK) {
      enrollState.addMessage("Image taken");
      Serial.println("Segunda imagen capturada");
      break;
    } else {
      enrollState.addMessage("Imaging error");
      return false;
    }
  }
  
  // Convertir segunda imagen
  p = finger.image2Tz(2);
  if (p != FINGERPRINT_OK) {
    enrollState.addMessage("Image too messy");
    return false;
  }
  enrollState.addMessage("Image converted");
  
  // Crear modelo
  enrollState.addMessage("Creating model");
  Serial.println("Creando modelo...");
  
  p = finger.createModel();
  if (p == FINGERPRINT_OK) {
    enrollState.addMessage("Prints matched!");
    Serial.println("Huellas coinciden");
  } else if (p == FINGERPRINT_ENROLLMISMATCH) {
    enrollState.addMessage("Fingerprints did not match");
    return false;
  } else {
    enrollState.addMessage("Unknown error");
    return false;
  }
  
  // Determinar ID
  finger.getTemplateCount();
  uint8_t id = finger.templateCount + 1;
  
  char idMsg[20];
  sprintf(idMsg, "ID %d", id);
  enrollState.addMessage(idMsg);
  
  // Almacenar modelo
  p = finger.storeModel(id);
  if (p == FINGERPRINT_OK) {
    enrollState.addMessage("Stored!");
    enrollState.enrolledId = id;
    Serial.printf("Huella almacenada con ID %d\n", id);
    return true;
  } else {
    enrollState.addMessage("Error writing to flash");
    return false;
  }
}
