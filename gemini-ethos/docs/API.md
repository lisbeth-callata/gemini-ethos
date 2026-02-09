# 🔌 API Reference - Gemini Ethos

Documentación completa de la API REST del servidor Gemini Ethos.

---

## Base URL

```
http://localhost:8080
```

## Autenticación

La API actualmente no requiere autenticación del cliente. La autenticación con Google Cloud se maneja internamente mediante Service Account.

---

## Endpoints

### 🩺 Health Check

Verifica el estado del servidor.

```http
GET /health
```

#### Response 200 OK

```json
{
  "status": "healthy",
  "service": "gemini-ethos",
  "version": "1.0.0",
  "activeSessions": 2
}
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `status` | string | Estado del servidor: `healthy` |
| `service` | string | Nombre del servicio |
| `version` | string | Versión de la API |
| `activeSessions` | integer | Sesiones de análisis activas |

---

### 📋 API Info

Información general de la API y endpoints disponibles.

```http
GET /api/v1
```

#### Response 200 OK

```json
{
  "name": "Gemini Ethos API",
  "version": "1.0.0",
  "description": "AI-powered ethical tourism guardian",
  "endpoints": {
    "POST /api/v1/analyze/frame": "Analyze a single image frame",
    "POST /api/v1/analyze/stream": "Analyze streaming video frame with metadata",
    "POST /api/v1/detect/place": "Detect location from image",
    "DELETE /api/v1/session/:sessionId": "Clear session history"
  }
}
```

---

### 📷 Analizar Imagen

Analiza una imagen para detectar comportamientos turísticos y evaluar impacto ambiental.

```http
POST /api/v1/analyze/frame
Content-Type: application/json
```

#### Request Body

```json
{
  "imageBase64": "string (required)",
  "mimeType": "string (optional, default: image/jpeg)",
  "location": "string (optional)",
  "sessionId": "string (optional)",
  "geoLocation": {
    "latitude": 0.0,
    "longitude": 0.0,
    "accuracy": 10.0
  },
  "timestamp": "2026-02-08T12:00:00Z"
}
```

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `imageBase64` | string | ✅ | Imagen codificada en Base64 |
| `mimeType` | string | ❌ | Tipo MIME: `image/jpeg`, `image/png` |
| `location` | string | ❌ | ID del parque: `galapagos`, `machu_picchu`, `amazon`, `patagonia`, `costa_rica` |
| `sessionId` | string | ❌ | ID de sesión para mantener contexto |
| `geoLocation` | object | ❌ | Coordenadas GPS |
| `timestamp` | string | ❌ | Marca de tiempo ISO 8601 |

#### Response 200 OK

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-02-08T18:30:00Z",
  "overallRiskLevel": "MEDIUM",
  "summary": "Se detecta interacción cercana con fauna silvestre en las Islas Galápagos. El turista se encuentra a una distancia estimada de 1.5 metros de una tortuga gigante, lo cual está por debajo del mínimo recomendado de 2 metros.",
  "detectedBehaviors": [
    {
      "behaviorType": "Proximidad excesiva a fauna",
      "description": "Turista a menos de 2 metros de tortuga gigante de Galápagos",
      "riskLevel": "HIGH",
      "confidence": 0.92,
      "location": "Centro-derecha de la imagen"
    },
    {
      "behaviorType": "Uso de cámara con flash",
      "description": "Se detecta flash activo en dispositivo móvil",
      "riskLevel": "MEDIUM",
      "confidence": 0.78,
      "location": "Manos del turista"
    }
  ],
  "guidelines": [
    {
      "category": "Fauna Silvestre",
      "guideline": "Mantener una distancia mínima de 2 metros de todos los animales",
      "culturalContext": "Las tortugas gigantes de Galápagos son una especie endémica y vulnerable",
      "environmentalImpact": "El acercamiento excesivo puede causar estrés y alterar comportamientos reproductivos"
    },
    {
      "category": "Fotografía",
      "guideline": "Desactivar el flash al fotografiar fauna",
      "culturalContext": "El flash puede desorientar a especies nocturnas y marinas",
      "environmentalImpact": "Puede causar ceguera temporal y comportamiento errático"
    }
  ],
  "immediateActions": [
    "Retroceder lentamente al menos 2 metros",
    "Desactivar el flash de la cámara",
    "Observar desde la distancia sin movimientos bruscos"
  ],
  "regulationInfo": {
    "parkName": "Parque Nacional Galápagos",
    "region": "Ecuador",
    "applicableRules": [
      "Art. 15: Distancia mínima de 2 metros a toda fauna",
      "Art. 23: Prohibido uso de flash en fotografía de fauna"
    ],
    "penalties": {
      "proximidad_fauna": "Multa de $100-500 USD",
      "flash_fotografia": "Advertencia o multa de $50 USD"
    },
    "source": "Reglamento de Turismo del PNG"
  },
  "environmentalAlert": {
    "level": "MEDIO",
    "justification": "El comportamiento detectado representa un riesgo moderado para la fauna local",
    "technicalAnalysis": "Análisis basado en distancia estimada y comportamiento del animal",
    "visualEvidence": [
      "Turista inclinado hacia el animal",
      "Distancia aproximada de 1.5m basada en proporción",
      "Flash visible en dispositivo móvil"
    ],
    "severityScore": 0.65
  },
  "reasoningProcess": {
    "visualObservations": [
      {
        "element": "Tortuga gigante",
        "description": "Chelonoidis niger adulto en posición de alimentación",
        "spatialLocation": "Centro de la imagen",
        "confidence": 0.95,
        "relevanceToRisk": "Especie endémica vulnerable"
      },
      {
        "element": "Turista",
        "description": "Persona adulta con cámara, postura inclinada hacia el animal",
        "spatialLocation": "Derecha de la imagen",
        "confidence": 0.98,
        "relevanceToRisk": "Proximidad excesiva detectada"
      }
    ],
    "inferenceChain": [
      "Detecto una tortuga gigante de Galápagos en el centro de la imagen",
      "Un turista se encuentra a aproximadamente 1.5 metros basado en la proporción de tamaños",
      "La postura del turista (inclinado hacia adelante) sugiere intención de acercarse más",
      "Se observa un destello característico de flash en el dispositivo",
      "La combinación de proximidad + flash representa riesgo acumulativo"
    ],
    "contextualAssessment": "La situación ocurre en un entorno natural de Galápagos donde las tortugas están habituadas a presencia humana pero siguen siendo vulnerables al estrés",
    "riskJustification": "Clasifico como MEDIUM porque aunque hay violación de distancia mínima, no hay contacto físico directo y el animal no muestra signos evidentes de estrés",
    "uncertainties": [
      "La distancia exacta es una estimación basada en proporción",
      "No puedo confirmar si el flash estaba activo en el momento de la foto"
    ]
  },
  "causalAnalysis": {
    "primaryCause": "Falta de señalización visible y supervisión de guía naturalista",
    "effectChains": [
      {
        "cause": "Proximidad excesiva",
        "immediateEffect": "Potencial estrés en el animal",
        "secondaryEffect": "Interrupción del comportamiento alimenticio",
        "ecosystemImpact": "Alteración de patrones de alimentación de la población"
      }
    ],
    "ecosystemSpecificImpact": "Las tortugas gigantes son ingenieras del ecosistema; su estrés afecta la dispersión de semillas",
    "shortTermConsequence": "El animal puede interrumpir su alimentación y retirarse",
    "longTermConsequence": "Habituación negativa a humanos o aversión a zonas de alimentación",
    "mitigationStrategies": [
      "Implementar barreras visuales naturales",
      "Aumentar frecuencia de patrullaje de guardaparques",
      "Programa de educación pre-visita obligatorio"
    ]
  }
}
```

#### Response 400 Bad Request

```json
{
  "error": "Missing 'imageBase64' or 'imageData' field"
}
```

#### Response 500 Internal Server Error

```json
{
  "error": "Error message from Vertex AI or internal processing"
}
```

---

### 📍 Detectar Lugar

Detecta automáticamente la ubicación/parque a partir de una imagen usando Gemini Vision.

```http
POST /api/v1/detect/place
Content-Type: application/json
```

#### Request Body

```json
{
  "imageBase64": "string (required)",
  "mimeType": "string (optional, default: image/jpeg)"
}
```

#### Response 200 OK

```json
{
  "placeName": "Islas Galápagos",
  "parkId": "galapagos",
  "description": "Identificado por la presencia de tortugas gigantes endémicas y la vegetación árida característica del archipiélago",
  "confidence": 92,
  "countryCode": "EC",
  "ecosystem": "marino",
  "visualClues": [
    "Tortuga gigante de Galápagos visible",
    "Vegetación de cactus Opuntia",
    "Terreno volcánico"
  ]
}
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `placeName` | string | Nombre del lugar detectado |
| `parkId` | string | ID del parque: `galapagos`, `machu_picchu`, `amazon`, `patagonia`, `costa_rica`, `unknown` |
| `description` | string | Explicación de por qué se identificó este lugar |
| `confidence` | integer | Nivel de confianza 0-100 |
| `countryCode` | string | Código de país ISO |
| `ecosystem` | string | Tipo de ecosistema |
| `visualClues` | array | Pistas visuales usadas para la identificación |

---

### 🎬 Analizar Stream

Analiza un frame de video en streaming con metadatos completos.

```http
POST /api/v1/analyze/stream
Content-Type: application/json
```

#### Request Body

```json
{
  "sessionId": "string (required)",
  "frameNumber": 1,
  "timestamp": "2026-02-08T12:00:00Z",
  "imageData": "base64_string (required)",
  "mimeType": "image/jpeg",
  "location": {
    "latitude": -0.628815,
    "longitude": -90.363875,
    "locationName": "Santa Cruz Island",
    "parkName": "galapagos"
  }
}
```

#### Response 200 OK

Mismo formato que `/api/v1/analyze/frame`.

---

### 🗑️ Limpiar Sesión

Elimina el historial de una sesión de análisis.

```http
DELETE /api/v1/session/:sessionId
```

#### Response 200 OK

```json
{
  "success": true,
  "message": "Session cleared: 550e8400-e29b-41d4-a716-446655440000"
}
```

---

### 📤 Subir Imagen (Multipart)

Endpoint alternativo para subir imagen como archivo.

```http
POST /api/v1/analyze/upload
Content-Type: multipart/form-data
```

#### Form Fields

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `image` | file | ✅ | Archivo de imagen |
| `sessionId` | string | ❌ | ID de sesión |
| `parkName` | string | ❌ | ID del parque |
| `latitude` | number | ❌ | Latitud GPS |
| `longitude` | number | ❌ | Longitud GPS |

#### Response 200 OK

Mismo formato que `/api/v1/analyze/frame`.

---

## Códigos de Estado

| Código | Descripción |
|--------|-------------|
| 200 | OK - Solicitud exitosa |
| 400 | Bad Request - Parámetros inválidos o faltantes |
| 500 | Internal Server Error - Error del servidor o Vertex AI |

---

## Rate Limits

No hay rate limits implementados a nivel de aplicación. Los límites dependen de tu cuota de Vertex AI.

---

## CORS

La API permite solicitudes desde cualquier origen:

```
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Authorization
```

---

## Ejemplos de Uso

### cURL

```bash
# Health check
curl http://localhost:8080/health

# Analizar imagen
curl -X POST http://localhost:8080/api/v1/analyze/frame \
  -H "Content-Type: application/json" \
  -d '{
    "imageBase64": "'$(base64 -w 0 image.jpg)'",
    "mimeType": "image/jpeg",
    "location": "galapagos"
  }'
```

### PowerShell

```powershell
# Health check
Invoke-RestMethod -Uri "http://localhost:8080/health"

# Analizar imagen
$imageBytes = [IO.File]::ReadAllBytes("C:\path\to\image.jpg")
$base64 = [Convert]::ToBase64String($imageBytes)

$body = @{
    imageBase64 = $base64
    mimeType = "image/jpeg"
    location = "galapagos"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/v1/analyze/frame" `
    -Method Post -Body $body -ContentType "application/json"
```

### JavaScript (Fetch)

```javascript
// Analizar imagen desde archivo
async function analyzeImage(file) {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    
    reader.onload = async () => {
        const base64 = reader.result.split(',')[1];
        
        const response = await fetch('http://localhost:8080/api/v1/analyze/frame', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                imageBase64: base64,
                mimeType: file.type,
                location: 'galapagos'
            })
        });
        
        const result = await response.json();
        console.log(result);
    };
}
```

### Python

```python
import requests
import base64

# Analizar imagen
with open("image.jpg", "rb") as f:
    image_base64 = base64.b64encode(f.read()).decode()

response = requests.post(
    "http://localhost:8080/api/v1/analyze/frame",
    json={
        "imageBase64": image_base64,
        "mimeType": "image/jpeg",
        "location": "galapagos"
    }
)

result = response.json()
print(result["overallRiskLevel"])
print(result["summary"])
```

---

## Tipos de Datos

### RiskLevel

```
LOW      - Comportamiento responsable
MEDIUM   - Precaución recomendada
HIGH     - Intervención sugerida
CRITICAL - Acción inmediata requerida
```

### AlertLevel

```
BAJO    - Sin amenaza inmediata
MEDIO   - Potencial impacto reversible
ALTO    - Riesgo significativo
CRITICO - Daño inminente o en progreso
```

### Park IDs

```
galapagos    - Islas Galápagos, Ecuador
machu_picchu - Machu Picchu, Perú
amazon       - Amazonía
patagonia    - Patagonia, Argentina/Chile
costa_rica   - Costa Rica
```

---

<p align="center">
  <strong>🌍 Gemini Ethos API v1.0.0</strong>
</p>
