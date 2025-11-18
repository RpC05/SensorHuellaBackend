#include <Adafruit_Fingerprint.h>
#include <HardwareSerial.h>

#if (defined(__AVR__) || defined(ESP8266)) && !defined(__AVR_ATmega2560__)
// Software serial para algunos boards
#else
#define mySerial Serial1
#endif

Adafruit_Fingerprint finger = Adafruit_Fingerprint(&Serial2);

// Variable global para ID de enrollment
uint8_t enrollID = 0;

void setup() {
  Serial.begin(9600);
  while (!Serial);
  delay(100);
  
  Serial2.begin(57600, SERIAL_8N1, 16, 17);

  Serial.println("Intentando encontrar el sensor...");

  if (finger.verifyPassword()) {
    Serial.println("SENSOR_OK");
  } else {
    Serial.println("ERROR:SENSOR_NOT_FOUND");
    while (1) { delay(1); }
  }

  finger.getParameters();
  finger.getTemplateCount();
  
  Serial.print("READY:"); 
  Serial.println(finger.templateCount);
}

void loop() {
  if (Serial.available()) {
    String command = Serial.readStringUntil('\n');
    command.trim();
    
    if (command.startsWith("ENROLL:")) {
      enrollID = command.substring(7).toInt();
      enrollFingerprint();
    }
    else if (command == "CAPTURE") {
      captureFingerprint(); 
    }
    else if (command == "PING") {
      Serial.println("PONG");
    }
    else {
      Serial.println("ERROR:UNKNOWN_COMMAND");
    }
  }
}


// ============================================
// ENROLLAR NUEVA HUELLA 
// ============================================
void enrollFingerprint() {
  Serial.print("STATUS:ENROLL_START:");
  Serial.println(enrollID);
  
  // Primera captura
  Serial.println("STATUS:PLACE_FINGER_1");
  int p = -1;
  
  unsigned long startTime = millis();
  while (p != FINGERPRINT_OK && (millis() - startTime) < 10000) {
    p = finger.getImage();
    delay(50);
  }
  
  if (p != FINGERPRINT_OK) {
    Serial.println("ERROR:TIMEOUT_1");
    return;
  }
  
  Serial.println("STATUS:IMAGE_1_OK");

  p = finger.image2Tz(1);
  if (p != FINGERPRINT_OK) {
    Serial.println("ERROR:CONVERT_1_FAIL");
    return;
  }

  Serial.println("STATUS:REMOVE_FINGER");
  delay(2000);
  
  // Esperar a que quite el dedo
  p = 0;
  startTime = millis();
  while (p != FINGERPRINT_NOFINGER && (millis() - startTime) < 5000) {
    p = finger.getImage();
    delay(50);
  }

  // Segunda captura
  Serial.println("STATUS:PLACE_FINGER_2");
  p = -1;
  startTime = millis();
  
  while (p != FINGERPRINT_OK && (millis() - startTime) < 10000) {
    p = finger.getImage();
    delay(50);
  }
  
  if (p != FINGERPRINT_OK) {
    Serial.println("ERROR:TIMEOUT_2");
    return;
  }
  
  Serial.println("STATUS:IMAGE_2_OK");

  p = finger.image2Tz(2);
  if (p != FINGERPRINT_OK) {
    Serial.println("ERROR:CONVERT_2_FAIL");
    return;
  }

  // Crear modelo
  Serial.println("STATUS:CREATING_MODEL");
  p = finger.createModel();
  if (p == FINGERPRINT_OK) {
    Serial.println("STATUS:MODEL_OK");
  } else if (p == FINGERPRINT_ENROLLMISMATCH) {
    Serial.println("ERROR:FINGERPRINTS_MISMATCH");
    return;
  } else {
    Serial.println("ERROR:CREATE_MODEL_FAIL");
    return;
  }

  // EN LUGAR DE GUARDAR, EXTRAER EL TEMPLATE
  Serial.println("STATUS:EXTRACTING_TEMPLATE");
  
  p = finger.getModel();
  if (p != FINGERPRINT_OK) {
    Serial.println("ERROR:GET_MODEL_FAIL");
    return;
  }

  // Recibir template del sensor
  uint8_t bytesReceived[534];
  memset(bytesReceived, 0xff, 534);

  startTime = millis();
  int i = 0;
  while (i < 534 && (millis() - startTime) < 20000) {
    if (Serial2.available()) {
      bytesReceived[i++] = Serial2.read();
    }
  }

  if (i < 534) {
    Serial.println("ERROR:INCOMPLETE_TEMPLATE");
    return;
  }

  // Extraer el template de 512 bytes
  uint8_t fingerTemplate[512];
  memset(fingerTemplate, 0xff, 512);

  int uindx = 9, index = 0;
  memcpy(fingerTemplate + index, bytesReceived + uindx, 256);
  uindx += 256 + 2 + 9;
  index += 256;
  memcpy(fingerTemplate + index, bytesReceived + uindx, 256);

  // Enviar template en hexadecimal
  Serial.print("TEMPLATE:");
  for (int i = 0; i < 512; i++) {
    if (fingerTemplate[i] < 0x10) Serial.print("0");
    Serial.print(fingerTemplate[i], HEX);
  }
  Serial.println();
  
  // Confirmar éxito (sin guardar en sensor)
  Serial.print("SUCCESS:");
  Serial.println(enrollID);
}

// ============================================
// CAPTURAR Y RETORNAR TEMPLATE 
// ============================================
void captureFingerprint() {
  Serial.println("STATUS:PLACE_FINGER");
  
  // ESPERA ACTIVA igual que en enrollment
  uint8_t p = -1;
  unsigned long startTime = millis();
  
  while (p != FINGERPRINT_OK && (millis() - startTime) < 15000) {
    p = finger.getImage();
    delay(50);
  }
  
  if (p != FINGERPRINT_OK) {
    if (p == FINGERPRINT_NOFINGER) {
      Serial.println("ERROR:NO_FINGER");
    } else {
      Serial.println("ERROR:IMAGE_FAIL");
    }
    return;
  }

  Serial.println("STATUS:IMAGE_OK");

  p = finger.image2Tz();
  if (p != FINGERPRINT_OK) {
    Serial.println("ERROR:CONVERT_FAIL");
    return;
  }

  // Extraer template
  p = finger.getModel();
  if (p != FINGERPRINT_OK) {
    Serial.println("ERROR:GET_MODEL_FAIL");
    return;
  }

  // Leer template del sensor
  uint8_t bytesReceived[534];
  memset(bytesReceived, 0xff, 534);

  startTime = millis();
  int i = 0;
  while (i < 534 && (millis() - startTime) < 20000) {
    if (Serial2.available()) {
      bytesReceived[i++] = Serial2.read();
    }
  }

  if (i < 534) {
    Serial.println("ERROR:INCOMPLETE_TEMPLATE");
    return;
  }

  uint8_t fingerTemplate[512];
  memset(fingerTemplate, 0xff, 512);

  int uindx = 9, index = 0;
  memcpy(fingerTemplate + index, bytesReceived + uindx, 256);
  uindx += 256 + 2 + 9;
  index += 256;
  memcpy(fingerTemplate + index, bytesReceived + uindx, 256);

  // Retornar template
  Serial.print("TEMPLATE:");
  for (int i = 0; i < 512; i++) {
    if (fingerTemplate[i] < 0x10) Serial.print("0");
    Serial.print(fingerTemplate[i], HEX);
  }
  Serial.println();
  
  Serial.println("STATUS:CAPTURE_OK");
}
