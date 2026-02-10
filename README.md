# 🌍 Gemini Ethos - Marathon Agent for Responsible Tourism

<p align="center">
  <img src="https://img.shields.io/badge/Gemini_3-Marathon_Agent-4285F4?style=for-the-badge&logo=google" alt="Gemini 3">
  <img src="https://img.shields.io/badge/Hackathon-Submission-FF6B6B?style=for-the-badge" alt="Hackathon">
  <img src="https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk" alt="Java 17">
  <img src="https://img.shields.io/badge/Vertex_AI-SDK-4285F4?style=for-the-badge&logo=googlecloud" alt="Vertex AI">
</p>

<p align="center">
  <b>🏆 Google DeepMind Gemini 3 Global Hackathon Submission</b><br>
  <i>Track: Marathon Agent - Long-Running Autonomous AI Systems</i>
</p>

---

## 📝 Gemini 3 Integration (~200 words)

**Gemini Ethos** is a **Marathon Agent** designed to promote responsible tourism. It utilizes the Gemini 3 Flash model via Google Vertex AI to analyze interactions between tourists and nature, identifying species and evaluating ethical risks in real-time.

---

### Key Features
- **Multimodal Analysis:** Objective identification of flora, fauna, and human behavior using computer vision.
- **Transparent Reasoning:** Generates an inference chain (Thought Signatures™) to explain the logic behind each risk assessment.
- **Scalable Architecture:** Deployed on Google Cloud Run with auto-scaling and high availability.
- **Causal Analysis:** Evaluates immediate and long-term impacts of human actions on specific ecosystems.

---

### Setup and Deployment

## Prerequisites
- Java 17+
- Maven 3.9+
- Google Cloud SDK (gcloud) installed.

### Environment Variables

```bash
# Clone
git clone https://github.com/lisbeth-callata/gemini-ethos.git
cd gemini-ethos

# Build
mvn clean package -DskipTests

# Configure (Linux/Mac)
export GOOGLE_CLOUD_PROJECT="your-project-id"
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/service-account.json"

# Configure (Windows PowerShell)
# $env:GOOGLE_CLOUD_PROJECT = "your-project-id"
# $env:GOOGLE_APPLICATION_CREDENTIALS = "C:\path\to\service-account.json"

# Run Marathon Agent
java -jar target/gemini-ethos-1.0.0-SNAPSHOT.jar

# Open Dashboard at http://localhost:8080/marathon-dashboard.html
```

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                    MARATHON PATROL AGENT                             │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                    PatrolMission                                │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │ │
│  │  │ Thought      │  │ Self-        │  │ Context              │  │ │
│  │  │ Signatures   │  │ Correction   │  │ Memory               │  │ │
│  │  │ (Chain)      │  │ Engine       │  │ (Continuity)         │  │ │
│  │  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────┘  │ │
│  │         └─────────────────┼──────────────────────┘              │ │
│  └────────────────────────────┼────────────────────────────────────┘ │
│                               ▼                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │              EthosGuardianAgent v3.0                           │ │
│  │  • gemini-3-flash-preview model                                │ │
│  │  • System Instruction with park-specific rules                 │ │
│  │  • Function Calling: regulations + alerts                      │ │
│  │  • Cause-Effect Analysis                                       │ │
│  └────────────────────────────┬───────────────────────────────────┘ │
└───────────────────────────────┼─────────────────────────────────────┘
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    GOOGLE CLOUD PLATFORM                             │
│                    Gemini 3 Flash API                                │
│                    Vertex AI - Global Endpoint                       │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 📡 API Endpoints (v3)

### Start a Patrol Mission
```bash
curl -X POST http://localhost:8080/api/v3/mission/start \
  -H "Content-Type: application/json" \
  -d '{
    "parkId": "galapagos",
    "type": "REAL_TIME_PATROL",
    "description": "Morning patrol of main tourist area",
    "durationHours": 4
  }'
```

### Analyze Image Within Mission Context
```bash
curl -X POST http://localhost:8080/api/v3/mission/MISSION-XXXX/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "imageBase64": "...",
    "mimeType": "image/jpeg",
    "location": "Playa Las Bachas"
  }'
```

### Get Thought Chain (Transparency)
```bash
curl http://localhost:8080/api/v3/mission/MISSION-XXXX/thoughts
```

### Get Self-Correction Log
```bash
curl http://localhost:8080/api/v3/mission/MISSION-XXXX
```

---

## 🧠 Thought Signatures Example

```json
{
  "thoughtId": "THOUGHT-42",
  "timestamp": "2026-02-08T15:30:00Z",
  "thinkingLevel": "high",
  "reasoning": "Critical proximity detected - tourist within 1m of nesting albatross",
  "observations": [
    "Human figure in left third of frame",
    "Adult albatross with visible chick",
    "Estimated distance: 0.8-1.2 meters",
    "Tourist posture suggests photo-taking behavior"
  ],
  "hypotheses": [
    "Tourist may be unaware of nesting site",
    "No visible barriers or signage in frame",
    "Time of day suggests peak activity period"
  ],
  "uncertainties": [
    "Cannot confirm if guide is present outside frame",
    "Wind direction unknown - may affect bird stress response"
  ],
  "confidence": 0.91,
  "nextAction": "Record incident and check for escalation"
}
```

---

## 🔄 Self-Correction in Action

```
[2026-02-08T16:45:00Z] SELF-CORRECTION #3: 
  'Previous CRITICAL assessment' -> 'May have been overestimated'
  Reason: Similar visual elements in current LOW-risk image suggest 
          previous assessment was too aggressive. Recalibrating 
          distance estimation parameters.
```

---

## Supported Parks

| Park | Country | Ecosystem | Unique Rules |
|------|---------|-----------|--------------|
| 🐢 Galápagos | Ecuador | Marine/Insular | 2m fauna distance |
| 🏔️ Machu Picchu | Peru | Mountain | No drones, restricted paths |
| 🌳 Amazon | Brazil/Peru | Rainforest | No flash photography |
| 🦙 Patagonia | Argentina/Chile | Glacier | Noise limits |
| 🦜 Costa Rica | Costa Rica | Cloud Forest | No feeding wildlife |

---

## Technical Metrics

| Metric | Value |
|--------|-------|
| Model | `gemini-3-flash-preview` |
| Max Output Tokens | 8192 |
| Temperature | 1.0 |
| Max Mission Duration | 72 hours |
| Checkpoint Interval | 15 minutes |
| Concurrent Analyses | 4 |
| Average Response Time | 3-8 seconds |

---

## 👥 Team

Built with ❤️ by the Gemini Ethos Team for the Google DeepMind Gemini 3 Global Hackathon.

---

<p align="center">
  <b>🌍 Protecting our planet's natural heritage, one patrol at a time 🌿</b>
</p>


