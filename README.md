# SensorHuellaBackend 🔐

Backend Spring Boot para gestión de huellas dactilares con sensor AS608 vía ESP32.

## 🚀 Características

- ✅ **Desplegable en la nube** (Render, AWS, Azure, etc.)
- ✅ Comunicación HTTP con ESP32 (sin necesidad de puerto serial)
- ✅ API REST completa para gestión de huellas
- ✅ Integración con PostgreSQL
- ✅ Soporte para Cloudflare Tunnel (sin IP pública)

## 📋 Endpoints API

### Gestión de Huellas

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/fingerprints` | Listar todas las huellas |
| `GET` | `/fingerprints/{id}` | Obtener huella por ID |
| `POST` | `/fingerprints` | Enrollar nueva huella |
| `POST` | `/fingerprints/verify` | Verificar huella |
| `DELETE` | `/fingerprints/{id}` | Eliminar huella |
| `GET` | `/fingerprints/count` | Contar huellas en sensor |
| `DELETE` | `/fingerprints/empty` | Vaciar base de datos del sensor |

## 🏗️ Arquitectura

```
[Cliente] --> [Spring Boot API] --> [PostgreSQL]
                      |
                      v (HTTP)
              [Cloudflare Tunnel]
                      |
                      v
                  [ESP32 HTTP Server] --> [AS608 Sensor]
```

## 🔧 Setup Rápido

### 1. Requisitos

- Java 17+
- Maven 3.6+
- PostgreSQL
- ESP32 con WiFi
- Sensor AS608

### 2. Configurar Base de Datos

```sql
CREATE DATABASE fingerprint_db;
```

### 3. Variables de Entorno

Crea archivo `.env`:

```properties
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/fingerprint_db
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=tu_password

# ESP32
ESP32_BASE_URL=http://192.168.1.100
# O con túnel:
# ESP32_BASE_URL=https://abc123.trycloudflare.com
```

### 4. Compilar y Ejecutar

```bash
mvn clean install
mvn spring-boot:run
```

La API estará disponible en: `http://localhost:8080`

## 🌐 Despliegue en la Nube

### Paso 1: Configurar ESP32

1. Carga `fingerprint_esp32_http_server.ino` en tu ESP32
2. Configura WiFi en el código:
   ```cpp
   const char* WIFI_SSID = "TuRedWiFi";
   const char* WIFI_PASSWORD = "TuPassword";
   ```
3. Anota la IP que obtiene el ESP32

### Paso 2: Crear Túnel Cloudflare

```bash
cloudflared tunnel --url http://192.168.1.100
```

Anota la URL pública (ej: `https://abc123.trycloudflare.com`)

### Paso 3: Desplegar en Render

1. Conecta tu repo de GitHub a Render
2. Configura variables de entorno:
   ```
   ESP32_BASE_URL=https://abc123.trycloudflare.com
   DATABASE_URL=tu_postgresql_url
   ```
3. Deploy!

📚 **Guías detalladas:**
- [Migración a HTTP (MIGRATION_TO_HTTP.md)](MIGRATION_TO_HTTP.md)
- [Configurar Cloudflare Tunnel (CLOUDFLARE_TUNNEL_GUIDE.md)](CLOUDFLARE_TUNNEL_GUIDE.md)

## 🧪 Testing

### Configuración para Pruebas Locales (Backend en PC + ESP32)

Para que el sistema funcione correctamente en local, necesitas configurar la comunicación en ambas direcciones:

1. **ESP32 ➡️ Backend (Registrar accesos):**
   - El ESP32 necesita llegar a tu PC.
   - **Opción A (IP Local):** En el código `.ino`, usa la IP de tu PC: `const char* BACKEND_URL = "http://192.168.X.X:8080";`
   - **Opción B (Túnel):** Usa un túnel Cloudflare que apunte a `localhost:8080`.

2. **Backend ➡️ ESP32 (Enrollar huellas):**
   - El backend necesita llegar al ESP32.
   - En `.env`, usa mDNS o IP directa: `ESP32_BASE_URL=http://sensorupaoiot.local` o `http://192.168.X.X`.
   - **Nota:** Si usas un túnel para el ESP32, asegúrate de que `cloudflared` esté corriendo en la misma red para poder resolver `sensorupaoiot.local`.

### Comandos de Prueba Manual

```bash
# Health check
curl http://localhost:8080/fingerprints/count

# Enroll (Inicia proceso en ESP32)
curl -X POST http://localhost:8080/fingerprints \
  -H "Content-Type: application/json" \
  -d '{"nombres":"Juan Perez","codigo":"12345"}'

# Verify
curl -X POST http://localhost:8080/fingerprints/verify
```

## 📁 Estructura del Proyecto

```
src/
├── main/
│   ├── java/com/example/sensor/
│   │   ├── api/              # Controllers REST
│   │   ├── config/           # Configuración (Esp32Config, etc.)
│   │   ├── model/
│   │   │   ├── dto/          # DTOs para API y ESP32
│   │   │   └── entity/       # Entidades JPA
│   │   ├── repository/       # Repositorios
│   │   ├── service/          # Lógica de negocio
│   │   │   └── Impl/
│   │   │       ├── Esp32HttpServiceImpl  # ⭐ Nuevo
│   │   │       ├── SerialServiceImpl     # Deprecated
│   │   │       └── FingerPrintServiceImpl
│   │   └── exceptions/       # Manejo de errores
│   └── resources/
│       └── application.properties
├── fingerprint_esp32_http_server.ino  # Código Arduino
├── MIGRATION_TO_HTTP.md               # Guía de migración
└── CLOUDFLARE_TUNNEL_GUIDE.md        # Guía del túnel
```

## 🔐 Seguridad

### Recomendaciones para Producción:

1. **HTTPS obligatorio** (Cloudflare lo provee gratis)
2. **Autenticación JWT** para la API
3. **API Key** para comunicación con ESP32
4. **Rate limiting** en endpoints sensibles
5. **Validación de inputs** (ya implementado con `@Valid`)

## 🐛 Troubleshooting

### "ESP32 no responde al ping"

✅ Verifica que:
- ESP32 esté encendido y conectado a WiFi
- Cloudflare Tunnel esté corriendo
- `ESP32_BASE_URL` sea correcta

### "Timeout esperando respuesta del sensor"

✅ El timeout está configurado en 30s. Si el proceso de enroll tarda más:
1. Revisa la calidad del WiFi del ESP32
2. Aumenta `esp32.read-timeout` en `application.properties`

### "Could not find fingerprint features"

✅ Este error viene del sensor AS608:
- Asegúrate de presionar el dedo firmemente
- Limpia el sensor
- Usa un dedo sin cortes ni humedad excesiva

## 📊 Tecnologías

- **Backend:** Spring Boot 3.5.7, Java 17
- **Database:** PostgreSQL + JPA/Hibernate
- **HTTP Client:** Spring WebFlux (WebClient)
- **Hardware:** ESP32 + AS608 Sensor
- **Tunnel:** Cloudflare Tunnel
- **Deployment:** Render (o cualquier PaaS)

## 🤝 Contribuir

1. Fork el proyecto
2. Crea una rama: `git checkout -b feature/nueva-funcionalidad`
3. Commit: `git commit -m 'Agregar nueva funcionalidad'`
4. Push: `git push origin feature/nueva-funcionalidad`
5. Abre un Pull Request

## 📝 Licencia

MIT License

## 👨‍💻 Autor

[@RpC05](https://github.com/RpC05)

---

**¿Necesitas ayuda?** Revisa las guías detalladas en:
- [MIGRATION_TO_HTTP.md](MIGRATION_TO_HTTP.md)
- [CLOUDFLARE_TUNNEL_GUIDE.md](CLOUDFLARE_TUNNEL_GUIDE.md)
