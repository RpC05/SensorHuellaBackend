#include <Adafruit_Fingerprint.h>
#include <HardwareSerial.h>

Adafruit_Fingerprint finger = Adafruit_Fingerprint(&Serial2);

void setup() {
  Serial.begin(9600);
  while (!Serial);
  delay(100);
  
  Serial2.begin(57600, SERIAL_8N1, 16, 17);

  if (finger.verifyPassword()) {
    Serial.println("SENSOR_OK");
  } else {
    Serial.println("SENSOR_NOT_FOUND");
    while (1) { delay(1); }
  }

  finger.getTemplateCount();
  Serial.print("READY:");
  Serial.println(finger.templateCount);
}

void loop() {
  if (Serial.available()) {
    String command = Serial.readStringUntil('\n');
    command.trim();
    
    if (command.equals("ENROLL")) {
      enrollFingerprint();
    } 
    else if (command.startsWith("DELETE ")) {
      uint8_t id = command.substring(7).toInt();
      deleteFingerprint(id);
    }
    else if (command.equals("VERIFY")) {
      verifyFingerprint();
    } 
    else if (command.equals("EMPTY")) {
      emptyDatabase();
    } 
    else if (command.equals("COUNT")) {
      finger.getTemplateCount();
      Serial.print("Sensor contains ");
      Serial.print(finger.templateCount);
      Serial.println(" templates");
    } 
    else if (command.equals("PING")) {
      Serial.println("PONG");
    }
  }
}

void verifyFingerprint() {
  uint8_t p = finger.getImage();
  if (p != FINGERPRINT_OK) {
    Serial.println("No finger detected");
    return;
  }
  
  Serial.println("Image taken");

  p = finger.image2Tz();
  if (p != FINGERPRINT_OK) {
    Serial.println("Image too messy");
    return;
  }
  
  Serial.println("Image converted");

  p = finger.fingerFastSearch();
  if (p == FINGERPRINT_OK) {
    Serial.println("Found a print match!");
    Serial.print("Found ID #"); 
    Serial.print(finger.fingerID);
    Serial.print(" with confidence of "); 
    Serial.println(finger.confidence);
  } else if (p == FINGERPRINT_NOTFOUND) {
    Serial.println("Did not find a match");
  } else {
    Serial.println("Unknown error");
  }
}

void enrollFingerprint() {
  Serial.println("Waiting for valid finger to enroll");
  
  int p = -1;
  while (p != FINGERPRINT_OK) {
    p = finger.getImage();
    switch (p) {
      case FINGERPRINT_OK:
        Serial.println("Image taken");
        break;
      case FINGERPRINT_NOFINGER:
        Serial.println("Waiting for finger");
        break;
      case FINGERPRINT_PACKETRECIEVEERR:
        Serial.println("Communication error");
        return;
      case FINGERPRINT_IMAGEFAIL:
        Serial.println("Imaging error");
        return;
      default:
        Serial.println("Unknown error");
        return;
    }
  }

  p = finger.image2Tz(1);
  switch (p) {
    case FINGERPRINT_OK:
      Serial.println("Image converted");
      break;
    case FINGERPRINT_IMAGEMESS:
      Serial.println("Image too messy");
      return;
    case FINGERPRINT_PACKETRECIEVEERR:
      Serial.println("Communication error");
      return;
    case FINGERPRINT_FEATUREFAIL:
    case FINGERPRINT_INVALIDIMAGE:
      Serial.println("Could not find fingerprint features");
      return;
    default:
      Serial.println("Unknown error");
      return;
  }

  Serial.println("Remove finger");
  delay(2000);
  
  p = 0;
  while (p != FINGERPRINT_NOFINGER) {
    p = finger.getImage();
  }

  Serial.println("Place same finger again");
  p = -1;
  while (p != FINGERPRINT_OK) {
    p = finger.getImage();
    switch (p) {
      case FINGERPRINT_OK:
        Serial.println("Image taken");
        break;
      case FINGERPRINT_NOFINGER:
        Serial.println("Waiting for finger");
        break;
      case FINGERPRINT_PACKETRECIEVEERR:
        Serial.println("Communication error");
        return;
      case FINGERPRINT_IMAGEFAIL:
        Serial.println("Imaging error");
        return;
      default:
        Serial.println("Unknown error");
        return;
    }
  }

  p = finger.image2Tz(2);
  switch (p) {
    case FINGERPRINT_OK:
      Serial.println("Image converted");
      break;
    case FINGERPRINT_IMAGEMESS:
      Serial.println("Image too messy");
      return;
    case FINGERPRINT_PACKETRECIEVEERR:
      Serial.println("Communication error");
      return;
    case FINGERPRINT_FEATUREFAIL:
    case FINGERPRINT_INVALIDIMAGE:
      Serial.println("Could not find fingerprint features");
      return;
    default:
      Serial.println("Unknown error");
      return;
  }

  Serial.println("Creating model");
  p = finger.createModel();
  if (p == FINGERPRINT_OK) {
    Serial.println("Prints matched!");
  } else if (p == FINGERPRINT_PACKETRECIEVEERR) {
    Serial.println("Communication error");
    return;
  } else if (p == FINGERPRINT_ENROLLMISMATCH) {
    Serial.println("Fingerprints did not match");
    return;
  } else {
    Serial.println("Unknown error");
    return;
  }

  finger.getTemplateCount();
  uint8_t id = finger.templateCount + 1;
  
  Serial.print("ID "); 
  Serial.println(id);
  
  p = finger.storeModel(id);
  if (p == FINGERPRINT_OK) {
    Serial.println("Stored!");
    Serial.println(id);
  } else if (p == FINGERPRINT_PACKETRECIEVEERR) {
    Serial.println("Communication error");
  } else if (p == FINGERPRINT_BADLOCATION) {
    Serial.println("Could not store in that location");
  } else if (p == FINGERPRINT_FLASHERR) {
    Serial.println("Error writing to flash");
  } else {
    Serial.println("Unknown error");
  }
}

void emptyDatabase() {
  uint8_t p = finger.emptyDatabase();
  if (p == FINGERPRINT_OK) {
    Serial.println("Database emptied!");
  } else if (p == FINGERPRINT_PACKETRECIEVEERR) {
    Serial.println("Communication error");
  } else if (p == FINGERPRINT_DBCLEARFAIL) {
    Serial.println("Could not clear database");
  } else {
    Serial.println("Unknown error");
  }
}

void deleteFingerprint(uint8_t id) {
  Serial.print("Deleting ID #");
  Serial.println(id);
  
  uint8_t p = finger.deleteModel(id);
  
  if (p == FINGERPRINT_OK) {
    Serial.println("Deleted!");
  } else if (p == FINGERPRINT_PACKETRECIEVEERR) {
    Serial.println("Communication error");
  } else if (p == FINGERPRINT_BADLOCATION) {
    Serial.println("Could not delete in that location");
  } else if (p == FINGERPRINT_FLASHERR) {
    Serial.println("Error writing to flash");
  } else {
    Serial.println("Unknown error");
  }
}
