package com.gemini.ethos.api;

import com.gemini.ethos.agent.MarathonPatrolAgent;
import com.gemini.ethos.agent.PatrolMission;
import com.gemini.ethos.config.VertexAIConfig;
import com.gemini.ethos.model.AnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static spark.Spark.*;

/**
 * Marathon API Server - Extended API for Long-Running Autonomous Patrols.
 * 
 * This server provides the REST API for the Marathon Agent functionality,
 * which is a key differentiator for the Gemini 3 Hackathon.
 * 
 * HACKATHON ALIGNMENT:
 * - "Marathon Agent" track: Autonomous systems for tasks spanning hours/days
 * - Uses Thought Signatures for reasoning transparency
 * - Self-correction capabilities
 * - Multi-step tool calls without human supervision
 * 
 * @author Gemini Ethos Team
 * @version 3.0.0
 */
public class MarathonApiServer {
    
    private static final Logger logger = LoggerFactory.getLogger(MarathonApiServer.class);
    private static final int ANALYSIS_TIMEOUT_SECONDS = 150;
    
    private final MarathonPatrolAgent marathonAgent;
    private final int port;
    
    // WebSocket-like event storage (for polling in demo)
    private final Map<String, List<Map<String, Object>>> eventQueues;
    
    public MarathonApiServer(VertexAIConfig config, int port) {
        this.marathonAgent = new MarathonPatrolAgent(config);
        this.port = port;
        this.eventQueues = new ConcurrentHashMap<>();
        
        // Set up event listeners
        setupEventListeners();
    }
    
    private void setupEventListeners() {
        // Queue incidents for client polling
        marathonAgent.onIncident(incident -> {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("type", "INCIDENT");
            event.put("timestamp", incident.detectedAt().toString());
            event.put("incidentId", incident.incidentId());
            event.put("riskLevel", incident.riskLevel());
            event.put("description", incident.description());
            event.put("requiresEscalation", incident.requiresEscalation());
            
            queueEventForAll(event);
        });
        
        // Queue checkpoints for client polling
        marathonAgent.onCheckpoint(checkpoint -> {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("type", "CHECKPOINT");
            event.put("timestamp", checkpoint.timestamp().toString());
            event.put("checkpointId", checkpoint.checkpointId());
            event.put("imagesProcessed", checkpoint.imagesProcessed());
            event.put("incidentsDetected", checkpoint.incidentsDetected());
            event.put("averageRiskScore", checkpoint.averageRiskScore());
            
            queueEventForAll(event);
        });
        
        // Queue escalations for client polling
        marathonAgent.onEscalation(mission -> {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("type", "ESCALATION");
            event.put("timestamp", java.time.Instant.now().toString());
            event.put("missionId", mission.getMissionId());
            event.put("parkId", mission.getParkId());
            event.put("status", mission.getStatus().name());
            event.put("lastThought", mission.getLastThought());
            
            queueEventForAll(event);
        });
    }
    
    private void queueEventForAll(Map<String, Object> event) {
        // Broadcast to all connected clients (simplified for demo)
        for (List<Map<String, Object>> queue : eventQueues.values()) {
            queue.add(event);
            // Keep only last 100 events
            while (queue.size() > 100) {
                queue.remove(0);
            }
        }
    }
    
    public void start() {
        port(port);
        threadPool(200, 8, 60000);
        
        // Static files must be configured BEFORE routes
        // Search for frontend in multiple possible locations
        String userDir = System.getProperty("user.dir");
        String[] possiblePaths = {
            userDir + "/frontend",
            userDir + "/gemini-ethos/frontend",
            new java.io.File(MarathonApiServer.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath()).getParentFile()
                .getParentFile().getAbsolutePath() + "/frontend"
        };
        String frontendPath = null;
        for (String path : possiblePaths) {
            java.io.File dir = new java.io.File(path);
            if (dir.exists() && dir.isDirectory()) {
                frontendPath = dir.getAbsolutePath();
                break;
            }
        }
        if (frontendPath != null) {
            staticFiles.externalLocation(frontendPath);
            logger.info("Serving frontend from: {}", frontendPath);
        } else {
            logger.warn("Frontend directory not found. Searched: {}", String.join(", ", possiblePaths));
        }
        
        enableCors();
        configureRoutes();
        
        exception(Exception.class, (e, req, res) -> {
            logger.error("Unhandled exception", e);
            res.status(500);
            res.type("application/json");
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            res.body(JsonUtils.toJson(Map.of(
                "error", "Internal server error",
                "message", msg
            )));
        });
        
        awaitInitialization();
        marathonAgent.start();
        
        logger.info("Marathon API Server started on port {}", port);
    }
    
    public void stop() {
        marathonAgent.close();
        spark.Spark.stop();
        awaitStop();
        logger.info("Marathon API Server stopped");
    }
    
    private void enableCors() {
        options("/*", (req, res) -> {
            String accessControlRequestHeaders = req.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                res.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }
            String accessControlRequestMethod = req.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                res.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }
            return "OK";
        });
        
        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
            // Only set JSON for API routes
            if (req.pathInfo().startsWith("/api") || req.pathInfo().equals("/health")) {
                res.type("application/json");
            }
        });
    }
    
    private void configureRoutes() {
        
        // Serve marathon dashboard as default
        get("/", (req, res) -> {
            res.redirect("/marathon-dashboard.html");
            return null;
        });
        
        // ============ Marathon Agent Info ============
        get("/health", (req, res) -> JsonUtils.toJson(Map.of(
            "status", "healthy",
            "service", "gemini-ethos-marathon",
            "version", "3.0.0",
            "geminiModel", "gemini-3-flash-preview",
            "hackathonTrack", "Marathon Agent",
            "agentStats", marathonAgent.getAgentStats()
        )));
        
        get("/api/v3", (req, res) -> JsonUtils.toJson(Map.of(
            "name", "Gemini Ethos Marathon API",
            "version", "3.0.0",
            "description", "Long-running autonomous AI patrol agent powered by Gemini 3",
            "gemini3Features", List.of(
                "Thought Signatures - Transparent reasoning chain",
                "Thinking Levels - Adaptive reasoning depth",
                "Self-Correction - Agent detects and corrects mistakes",
                "Context Continuity - State maintained across hours/days",
                "Autonomous Tool Calling - Multi-step plans without supervision"
            ),
            "endpoints", Map.of(
                "POST /api/v3/mission/start", "Start a new patrol mission",
                "GET /api/v3/mission/:id", "Get mission status and details",
                "POST /api/v3/mission/:id/analyze", "Analyze image within mission context",
                "POST /api/v3/mission/:id/pause", "Pause a mission",
                "POST /api/v3/mission/:id/resume", "Resume a paused mission",
                "DELETE /api/v3/mission/:id", "Abort a mission",
                "GET /api/v3/mission/:id/thoughts", "Get mission thought chain",
                "GET /api/v3/mission/:id/incidents", "Get mission incidents",
                "GET /api/v3/missions", "List all active missions",
                "GET /api/v3/events", "Poll for real-time events"
            )
        )));
        
        // ============ Mission Management ============
        
        // Start a new mission
        post("/api/v3/mission/start", this::handleStartMission);
        
        // Get mission details
        get("/api/v3/mission/:id", this::handleGetMission);
        
        // Analyze image within mission
        post("/api/v3/mission/:id/analyze", this::handleMissionAnalysis);
        
        // Pause mission
        post("/api/v3/mission/:id/pause", this::handlePauseMission);
        
        // Resume mission
        post("/api/v3/mission/:id/resume", this::handleResumeMission);
        
        // Abort mission
        delete("/api/v3/mission/:id", this::handleAbortMission);
        
        // Get thought chain
        get("/api/v3/mission/:id/thoughts", this::handleGetThoughts);
        
        // Get incidents
        get("/api/v3/mission/:id/incidents", this::handleGetIncidents);
        
        // Get analyzed images for mission gallery
        get("/api/v3/mission/:id/images", this::handleGetMissionImages);
        
        // List active missions
        get("/api/v3/missions", this::handleListMissions);
        
        // Get events (long-polling style)
        get("/api/v3/events", this::handleGetEvents);
        
        // ============ Agent Stats ============
        get("/api/v3/stats", (req, res) -> JsonUtils.toJson(marathonAgent.getAgentStats()));
        
        // ============ Direct Analysis (without mission) ============
        // For backwards compatibility with v1 API and quick analysis
        post("/api/v3/analyze", this::handleDirectAnalysis);
        post("/api/v1/analyze", this::handleDirectAnalysis);
        
        // Also support legacy endpoint
        post("/api/v1/environment/analyze", this::handleDirectAnalysis);
    }
    
    /**
     * Handle direct image analysis without a mission context.
     * This provides backwards compatibility and quick analysis capability.
     */
    private Object handleDirectAnalysis(Request req, Response res) {
        try {
            var json = com.google.gson.JsonParser.parseString(req.body()).getAsJsonObject();
            
            String imageBase64 = json.get("image").getAsString();
            String mimeType = json.has("mimeType") ? json.get("mimeType").getAsString() : "image/jpeg";
            
            // Optional location info
            String location = json.has("location") ? json.get("location").getAsString() : "Unknown location";
            String parkId = json.has("parkId") ? json.get("parkId").getAsString() : "general";
            double latitude = json.has("latitude") ? json.get("latitude").getAsDouble() : 0;
            double longitude = json.has("longitude") ? json.get("longitude").getAsDouble() : 0;
            
            byte[] imageData = java.util.Base64.getDecoder().decode(imageBase64);
            
            // Use the core agent for direct analysis (async call)
            var geoLocation = new com.gemini.ethos.model.StreamingFrame.GeoLocation(
                latitude, longitude, location, parkId
            );
            
            // Call the async method and wait for result
            AnalysisResult result = marathonAgent.getCoreAgent().analyzeEnvironment(
                imageData, mimeType, null, geoLocation
            ).get(90, java.util.concurrent.TimeUnit.SECONDS);
            
            // Add a thought for this direct analysis
            addThought(new PatrolMission.ThoughtSignature(
                java.util.UUID.randomUUID().toString(),
                java.time.Instant.now(),
                "medium",  // thinkingLevel as String
                "Direct image analysis performed",
                List.of(result.summary() != null ? result.summary() : "Analysis complete"),
                List.of(),  // hypotheses
                List.of(),  // uncertainties
                0.8,        // confidence
                "Provide recommendations"
            ));
            
            // Calculate risk score from risk level
            float riskScore = 0.0f;
            if (result.overallRiskLevel() != null) {
                String levelName = result.overallRiskLevel().name();
                if ("LOW".equals(levelName)) riskScore = 0.2f;
                else if ("MEDIUM".equals(levelName)) riskScore = 0.5f;
                else if ("HIGH".equals(levelName)) riskScore = 0.75f;
                else if ("CRITICAL".equals(levelName)) riskScore = 1.0f;
            }
            
            // Return enhanced response with Gemini 3 Flash info
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("geminiModel", "gemini-3-flash-preview");
            response.put("analysisType", "direct");
            response.put("riskLevel", result.overallRiskLevel() != null ? result.overallRiskLevel().name() : "LOW");
            response.put("riskScore", riskScore);
            response.put("summary", result.summary());
            response.put("description", result.overallRiskLevel() != null ? result.overallRiskLevel().getDescription() : "Analysis complete");
            response.put("behaviors", result.detectedBehaviors());
            response.put("recommendations", result.immediateActions());
            response.put("guidelines", result.guidelines());
            response.put("reasoningProcess", result.reasoningProcess());
            response.put("causalAnalysis", result.causalAnalysis());
            response.put("timestamp", java.time.Instant.now().toString());
            
            // Gemini 3 specific fields
            response.put("thinkingLevel", "MEDIUM");
            response.put("thoughtSignature", Map.of(
                "reasoning", "Environmental impact assessment using Gemini 3 vision capabilities",
                "confidence", riskScore > 0.5 ? "high" : "medium"
            ));
            
            return JsonUtils.toJson(response);
            
        } catch (Exception e) {
            logger.error("Error in direct analysis", e);
            res.status(500);
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return JsonUtils.toJson(Map.of(
                "success", false,
                "error", errorMsg
            ));
        }
    }
    
    private void addThought(PatrolMission.ThoughtSignature thought) {
        // Add to any active missions using the mission's own addThought method
        marathonAgent.getActiveMissions().forEach(mission -> {
            if (mission.getStatus() == PatrolMission.MissionStatus.ACTIVE) {
                mission.addThought(
                    thought.reasoning(),
                    thought.thinkingLevel(),
                    thought.observations(),
                    thought.hypotheses(),
                    thought.uncertainties(),
                    thought.confidence(),
                    thought.nextAction()
                );
                logger.info("💭 Thought added to mission {}: {}", mission.getMissionId(), thought.reasoning());
            }
        });
    }

    /**
     * Start a new patrol mission.
     * 
     * Request: {
     *   "parkId": "galapagos",
     *   "type": "REAL_TIME_PATROL",
     *   "description": "Morning patrol of main tourist area",
     *   "durationHours": 4
     * }
     */
    private Object handleStartMission(Request req, Response res) {
        try {
            var json = com.google.gson.JsonParser.parseString(req.body()).getAsJsonObject();
            
            String parkId = json.get("parkId").getAsString();
            String typeStr = json.has("type") ? json.get("type").getAsString() : "REAL_TIME_PATROL";
            String description = json.has("description") ? json.get("description").getAsString() : "Autonomous patrol";
            int durationHours = json.has("durationHours") ? json.get("durationHours").getAsInt() : 4;
            
            PatrolMission.MissionType type = PatrolMission.MissionType.valueOf(typeStr);
            Duration duration = Duration.ofHours(durationHours);
            
            PatrolMission mission = marathonAgent.startMission(parkId, type, description, duration);
            
            // Create event queue for this mission
            eventQueues.put(mission.getMissionId(), Collections.synchronizedList(new ArrayList<>()));
            
            return JsonUtils.toJson(Map.of(
                "success", true,
                "mission", mission.getSummary(),
                "message", "Mission started successfully. The agent will run autonomously for " + durationHours + " hours."
            ));
            
        } catch (Exception e) {
            logger.error("Error starting mission", e);
            res.status(400);
            return JsonUtils.toJson(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get mission status and details.
     */
    private Object handleGetMission(Request req, Response res) {
        String missionId = req.params(":id");
        PatrolMission mission = marathonAgent.getMission(missionId);
        
        if (mission == null) {
            res.status(404);
            return JsonUtils.toJson(Map.of("error", "Mission not found: " + missionId));
        }
        
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("mission", mission.getSummary());
        details.put("lastThought", mission.getLastThought());
        details.put("recentCheckpoints", mission.getCheckpoints().stream()
            .skip(Math.max(0, mission.getCheckpoints().size() - 5))
            .toList());
        details.put("selfCorrections", mission.getSelfCorrections());
        details.put("correctionLog", mission.getCorrectionLog());
        
        return JsonUtils.toJson(details);
    }
    
    /**
     * Analyze an image within mission context.
     * The agent maintains thought continuity from previous analyses.
     * 
     * Request: {
     *   "imageBase64": "base64...",
     *   "mimeType": "image/jpeg",
     *   "location": "Playa Las Bachas"
     * }
     */
    private Object handleMissionAnalysis(Request req, Response res) {
        String missionId = req.params(":id");
        
        try {
            var json = com.google.gson.JsonParser.parseString(req.body()).getAsJsonObject();
            
            // Accept both "imageBase64" and "image" field names for flexibility
            String imageBase64;
            if (json.has("imageBase64")) {
                imageBase64 = json.get("imageBase64").getAsString();
            } else if (json.has("image")) {
                imageBase64 = json.get("image").getAsString();
            } else {
                res.status(400);
                return JsonUtils.toJson(Map.of("error", "Missing image data. Provide 'image' or 'imageBase64' field."));
            }
            
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
            String mimeType = json.has("mimeType") ? json.get("mimeType").getAsString() : "image/jpeg";
            String location = json.has("location") ? json.get("location").getAsString() : "Unknown";
            
            Map<String, Double> geoLocation = null;
            if (json.has("geoLocation")) {
                var geoJson = json.getAsJsonObject("geoLocation");
                geoLocation = Map.of(
                    "latitude", geoJson.get("latitude").getAsDouble(),
                    "longitude", geoJson.get("longitude").getAsDouble()
                );
            }
            
            CompletableFuture<AnalysisResult> future = marathonAgent.analyzeWithinMission(
                missionId, imageBytes, mimeType, location, geoLocation);
            
            AnalysisResult result = future.get(ANALYSIS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            // Include current thought chain context
            PatrolMission mission = marathonAgent.getMission(missionId);
            
            // Calculate risk score from risk level
            float riskScore = 0.0f;
            String riskLevelName = "LOW";
            if (result.overallRiskLevel() != null) {
                riskLevelName = result.overallRiskLevel().name();
                if ("LOW".equals(riskLevelName)) riskScore = 0.2f;
                else if ("MEDIUM".equals(riskLevelName)) riskScore = 0.5f;
                else if ("HIGH".equals(riskLevelName)) riskScore = 0.75f;
                else if ("CRITICAL".equals(riskLevelName)) riskScore = 1.0f;
            }
            
            // Return flat response that the dashboard frontend expects
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("geminiModel", "gemini-3-flash-preview");
            response.put("riskLevel", riskLevelName);
            response.put("riskScore", riskScore);
            response.put("summary", result.summary());
            response.put("description", result.overallRiskLevel() != null ? result.overallRiskLevel().getDescription() : "Analysis complete");
            response.put("behaviors", result.detectedBehaviors());
            response.put("recommendations", result.immediateActions());
            response.put("guidelines", result.guidelines());
            response.put("reasoningProcess", result.reasoningProcess());
            response.put("causalAnalysis", result.causalAnalysis());
            response.put("thinkingLevel", mission.getLastThought() != null ? 
                    mission.getLastThought().thinkingLevel().toUpperCase() : "MEDIUM");
            response.put("timestamp", java.time.Instant.now().toString());
            
            // Mission context
            response.put("missionContext", Map.of(
                "imagesAnalyzedInMission", mission.getTotalImagesAnalyzed(),
                "incidentsInMission", mission.getTotalIncidentsDetected(),
                "thoughtChainLength", mission.getThoughtChain().size(),
                "selfCorrectionsApplied", mission.getSelfCorrections()
            ));
            
            return JsonUtils.toJson(response);
            
        } catch (Exception e) {
            logger.error("Error in mission analysis", e);
            res.status(500);
            String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return JsonUtils.toJson(Map.of("error", errMsg));
        }
    }
    
    private Object handlePauseMission(Request req, Response res) {
        String missionId = req.params(":id");
        
        try {
            var json = com.google.gson.JsonParser.parseString(req.body()).getAsJsonObject();
            String reason = json.has("reason") ? json.get("reason").getAsString() : "Manual pause";
            
            marathonAgent.pauseMission(missionId, reason);
            
            PatrolMission mission = marathonAgent.getMission(missionId);
            return JsonUtils.toJson(Map.of(
                "success", true,
                "mission", mission.getSummary()
            ));
            
        } catch (Exception e) {
            res.status(400);
            return JsonUtils.toJson(Map.of("error", e.getMessage()));
        }
    }
    
    private Object handleResumeMission(Request req, Response res) {
        String missionId = req.params(":id");
        
        marathonAgent.resumeMission(missionId);
        
        PatrolMission mission = marathonAgent.getMission(missionId);
        if (mission == null) {
            res.status(404);
            return JsonUtils.toJson(Map.of("error", "Mission not found"));
        }
        
        return JsonUtils.toJson(Map.of(
            "success", true,
            "mission", mission.getSummary()
        ));
    }
    
    private Object handleAbortMission(Request req, Response res) {
        String missionId = req.params(":id");
        
        String reason = req.queryParams("reason");
        if (reason == null) reason = "Manual abort";
        
        marathonAgent.abortMission(missionId, reason);
        
        eventQueues.remove(missionId);
        
        return JsonUtils.toJson(Map.of(
            "success", true,
            "message", "Mission aborted: " + missionId
        ));
    }
    
    /**
     * Get the thought chain for a mission.
     * This exposes the Thought Signatures feature - transparent reasoning.
     */
    private Object handleGetThoughts(Request req, Response res) {
        String missionId = req.params(":id");
        PatrolMission mission = marathonAgent.getMission(missionId);
        
        if (mission == null) {
            res.status(404);
            return JsonUtils.toJson(Map.of("error", "Mission not found"));
        }
        
        int limit = 50;
        try {
            String limitParam = req.queryParams("limit");
            if (limitParam != null) limit = Integer.parseInt(limitParam);
        } catch (NumberFormatException ignored) {}
        
        List<PatrolMission.ThoughtSignature> thoughts = mission.getThoughtChain();
        int start = Math.max(0, thoughts.size() - limit);
        
        return JsonUtils.toJson(Map.of(
            "missionId", missionId,
            "totalThoughts", thoughts.size(),
            "thoughtsReturned", thoughts.size() - start,
            "thoughts", thoughts.subList(start, thoughts.size())
        ));
    }
    
    private Object handleGetIncidents(Request req, Response res) {
        String missionId = req.params(":id");
        PatrolMission mission = marathonAgent.getMission(missionId);
        
        if (mission == null) {
            res.status(404);
            return JsonUtils.toJson(Map.of("error", "Mission not found"));
        }
        
        return JsonUtils.toJson(Map.of(
            "missionId", missionId,
            "totalIncidents", mission.getIncidents().size(),
            "incidents", mission.getIncidents()
        ));
    }

    /**
     * Get analyzed images for a mission (gallery with thumbnails).
     */
    private Object handleGetMissionImages(Request req, Response res) {
        String missionId = req.params(":id");
        PatrolMission mission = marathonAgent.getMission(missionId);
        
        if (mission == null) {
            res.status(404);
            return JsonUtils.toJson(Map.of("error", "Mission not found"));
        }
        
        List<Map<String, Object>> images = mission.getAnalyzedImages().stream()
            .map(img -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("imageId", img.imageId());
                m.put("analyzedAt", img.analyzedAt().toString());
                m.put("mimeType", img.mimeType());
                m.put("thumbnailBase64", img.thumbnailBase64());
                m.put("riskLevel", img.riskLevel());
                m.put("location", img.location());
                return m;
            }).toList();
        
        return JsonUtils.toJson(Map.of(
            "missionId", missionId,
            "totalImages", images.size(),
            "images", images
        ));
    }
    
    private Object handleListMissions(Request req, Response res) {
        List<PatrolMission> missions = marathonAgent.getActiveMissions();
        
        return JsonUtils.toJson(Map.of(
            "activeMissions", missions.size(),
            "missions", missions.stream().map(PatrolMission::getSummary).toList()
        ));
    }
    
    /**
     * Long-polling style event retrieval.
     * Returns new events since last poll for real-time updates.
     */
    private Object handleGetEvents(Request req, Response res) {
        String clientId = req.queryParams("clientId");
        if (clientId == null) {
            clientId = UUID.randomUUID().toString();
        }
        
        // Get or create queue for this client
        List<Map<String, Object>> queue = eventQueues.computeIfAbsent(
            clientId, k -> Collections.synchronizedList(new ArrayList<>()));
        
        // Return and clear events
        List<Map<String, Object>> events = new ArrayList<>(queue);
        queue.clear();
        
        return JsonUtils.toJson(Map.of(
            "clientId", clientId,
            "eventCount", events.size(),
            "events", events
        ));
    }
    
    public MarathonPatrolAgent getAgent() {
        return marathonAgent;
    }
}
