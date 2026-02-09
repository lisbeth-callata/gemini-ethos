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

**Gemini Ethos** is a **Marathon Agent** that autonomously patrols natural heritage sites for **hours or days**, detecting and responding to irresponsible tourist behavior in real-time.

### Gemini 3 Features Used:

1. **Thought Signatures™** - Every analysis includes a transparent reasoning chain. The agent explicitly shows what it observed, what inferences it made, and what uncertainties exist. This creates a forensic audit trail for each decision.

2. **Thinking Levels** - The agent dynamically adjusts reasoning depth (low/medium/high) based on situation complexity. Routine checks use "low" thinking, while critical incidents trigger "high" reasoning with full cause-effect analysis.

3. **Self-Correction** - During long-running patrols, the agent reviews its own past assessments and corrects mistakes. If it over-reacted to a situation, it recalibrates future analyses automatically.

4. **Context Continuity** - State is maintained across multi-step operations. The agent "remembers" patterns from previous images, building a coherent understanding over the entire patrol duration.

5. **Autonomous Multi-Tool Execution** - The agent chains tool calls (regulation lookup, alert level calculation, incident recording) without human supervision, escalating only when truly necessary.

This isn't a simple vision analyzer - it's an **autonomous guardian** that operates independently for extended periods, making Gemini 3's enhanced reasoning capabilities essential.

---

## 🎯 Why This Matters (Potential Impact)

- **$42 billion** wildlife tourism industry threatened by irresponsible behavior
- **70%** of natural sites report tourist-caused environmental damage
- **UNESCO** sites require 24/7 monitoring impossible with human staff alone
- This agent can patrol **continuously** at a fraction of the cost

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.9+
- Google Cloud Project with Vertex AI enabled

### Run in 30 Seconds

```bash
# Clone
git clone https://github.com/your-username/gemini-ethos.git
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

## 📍 Supported Parks

| Park | Country | Ecosystem | Unique Rules |
|------|---------|-----------|--------------|
| 🐢 Galápagos | Ecuador | Marine/Insular | 2m fauna distance |
| 🏔️ Machu Picchu | Peru | Mountain | No drones, restricted paths |
| 🌳 Amazon | Brazil/Peru | Rainforest | No flash photography |
| 🦙 Patagonia | Argentina/Chile | Glacier | Noise limits |
| 🦜 Costa Rica | Costa Rica | Cloud Forest | No feeding wildlife |

---

## 📊 Technical Metrics

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

## 🎬 Demo Video

📺 **[Watch 3-Minute Demo Video](https://youtube.com/your-demo-link)**

The video demonstrates:
1. Starting a patrol mission via API
2. Analyzing images with thought chain transparency
3. Self-correction when the agent detects its own mistake
4. Automatic escalation for critical incidents
5. Final mission summary with statistics

---

## 📁 Project Structure

```
gemini-ethos/
├── src/main/java/com/gemini/ethos/
│   ├── agent/
│   │   ├── EthosGuardianAgent.java    # Core AI agent
│   │   ├── MarathonPatrolAgent.java   # Long-running orchestrator ⭐
│   │   └── PatrolMission.java         # Mission state management ⭐
│   ├── api/
│   │   ├── EthosApiServer.java        # Standard API
│   │   └── MarathonApiServer.java     # Marathon API v3 ⭐
│   ├── config/
│   │   └── VertexAIConfig.java        # GCP configuration
│   ├── model/
│   │   ├── AnalysisResult.java        # Response DTOs
│   │   └── StreamingFrame.java        # Input DTO
│   ├── tools/
│   │   └── RegulationsLookupTool.java # Function calling
│   └── Application.java               # Entry point
├── frontend/
│   ├── index.html                     # Quick Analyzer UI
│   ├── app.js                         # Quick Analyzer logic
│   ├── marathon-dashboard.html        # Marathon Agent Dashboard ⭐
│   └── marathon-app.js                # Dashboard frontend logic ⭐
├── pom.xml
└── README.md
```

---

## 🏆 Hackathon Alignment

| Criterion | How We Address It |
|-----------|-------------------|
| **Technical Execution (40%)** | Full Java backend with Vertex AI SDK, Function Calling, async processing, Marathon Agent architecture |
| **Innovation (30%)** | Not a simple analyzer - autonomous multi-day patrols with self-correction and thought transparency |
| **Potential Impact (20%)** | Protects $42B wildlife tourism industry and UNESCO sites worldwide |
| **Presentation (10%)** | Clear architecture diagrams, API docs, and demo video |

### Why This Is NOT a "Simple Vision Analyzer"

✅ **Multi-step autonomous execution** - Missions run for hours without supervision  
✅ **Self-correcting** - Agent improves its own accuracy over time  
✅ **Thought transparency** - Every decision is explainable  
✅ **Context continuity** - Remembers patterns across hundreds of images  
✅ **Escalation logic** - Knows when to ask for human help  

---

## 👥 Team

Built with ❤️ by the Gemini Ethos Team for the Google DeepMind Gemini 3 Global Hackathon.

---

<p align="center">
  <b>🌍 Protecting our planet's natural heritage, one patrol at a time 🌿</b>
</p>
