# 🚀 Guía de Inicio Rápido - Gemini Ethos

Configura y ejecuta Gemini Ethos en menos de 5 minutos.

---

## Requisitos

- ✅ Java 17+
- ✅ Maven 3.9+
- ✅ Proyecto de Google Cloud con Vertex AI habilitado
- ✅ Service Account con permisos

---

## Paso 1: Clonar el Proyecto

```bash
git clone <repository-url>
cd gemini-ethos
```

---

## Paso 2: Configurar Credenciales

### 2.1 Crear Service Account (si no existe)

```bash
# En Google Cloud Console o con gcloud:
gcloud iam service-accounts create ethos-sa \
    --display-name="Gemini Ethos Service Account"

# Asignar permisos
gcloud projects add-iam-policy-binding TU_PROYECTO \
    --member="serviceAccount:ethos-sa@TU_PROYECTO.iam.gserviceaccount.com" \
    --role="roles/aiplatform.user"

# Descargar clave
gcloud iam service-accounts keys create credentials/service-account.json \
    --iam-account=ethos-sa@TU_PROYECTO.iam.gserviceaccount.com
```

### 2.2 Colocar el archivo de credenciales

```
gemini-ethos/
└── credentials/
    └── service-account.json  ← Colocar aquí
```

---

## Paso 3: Compilar

```bash
mvn clean package -DskipTests
```

Resultado esperado:
```
[INFO] BUILD SUCCESS
[INFO] target/gemini-ethos-1.0.0-SNAPSHOT.jar
```

---

## Paso 4: Ejecutar

### Windows PowerShell

```powershell
$env:GOOGLE_CLOUD_PROJECT = "tu-proyecto-id"
$env:GOOGLE_APPLICATION_CREDENTIALS = "C:\ruta\gemini-ethos\credentials\service-account.json"
java -jar target\gemini-ethos-1.0.0-SNAPSHOT.jar
```

### Linux / macOS

```bash
export GOOGLE_CLOUD_PROJECT="tu-proyecto-id"
export GOOGLE_APPLICATION_CREDENTIALS="/ruta/gemini-ethos/credentials/service-account.json"
java -jar target/gemini-ethos-1.0.0-SNAPSHOT.jar
```

---

## Paso 5: Verificar

```bash
curl http://localhost:8080/health
```

Respuesta esperada:
```json
{"status":"healthy","service":"gemini-ethos","version":"1.0.0","activeSessions":0}
```

---

## Paso 6: Abrir Frontend

Abre en tu navegador:
```
frontend/index.html
```

O desde terminal:
```bash
# Windows
start frontend\index.html

# macOS
open frontend/index.html

# Linux
xdg-open frontend/index.html
```

---

## ✅ ¡Listo!

Ahora puedes:
1. Subir una imagen arrastrándola
2. Usar la cámara en vivo
3. Seleccionar la ubicación del parque
4. Hacer clic en "Analizar con IA"

---

## 🐛 Troubleshooting Rápido

| Problema | Solución |
|----------|----------|
| `PERMISSION_DENIED` | Habilitar Vertex AI API en Cloud Console |
| `Unable to access jarfile` | Ejecutar desde el directorio `gemini-ethos` |
| `Connection refused` | Verificar que el servidor esté corriendo |
| Frontend no conecta | Verificar que el servidor esté en puerto 8080 |

---

## 📚 Más Documentación

- [README.md](README.md) - Documentación completa
- [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) - Guía de desarrollo
- [docs/API.md](docs/API.md) - Referencia de la API

---

<p align="center">
  <strong>🌍 Protegiendo el patrimonio natural del planeta 🌿</strong>
</p>
