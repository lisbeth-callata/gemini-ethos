package com.gemini.ethos.api;

import com.gemini.ethos.agent.EthosGuardianAgent;
import com.gemini.ethos.model.AnalysisResult;
import com.gemini.ethos.model.StreamingFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.io.InputStream;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static spark.Spark.*;

/**
 * REST API Server for Gemini Ethos using SparkJava.
 * Provides endpoints for video streaming analysis and ethical guidance.
 */
public class EthosApiServer {
    
    private static final Logger logger = LoggerFactory.getLogger(EthosApiServer.class);
    
    private static final String MULTIPART_CONFIG_LOCATION = System.getProperty("java.io.tmpdir");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long MAX_REQUEST_SIZE = 15 * 1024 * 1024; // 15MB
    private static final int ANALYSIS_TIMEOUT_SECONDS = 300;
    
    private final EthosGuardianAgent agent;
    private final int port;
    
    public EthosApiServer(EthosGuardianAgent agent, int port) {
        this.agent = agent;
        this.port = port;
    }
    
    /**
     * Starts the API server.
     */
    public void start() {
        // Configure Spark
        port(port);
        threadPool(200, 8, 60000);
        
        // Enable CORS
        enableCors();
        
        // Configure routes
        configureRoutes();
        
        // Global exception handler
        exception(Exception.class, (e, req, res) -> {
            logger.error("Unhandled exception", e);
            res.status(500);
            res.type("application/json");
            res.body(JsonUtils.toJson(Map.of(
                "error", "Internal server error",
                "message", e.getMessage()
            )));
        });
        
        // Wait for initialization
        awaitInitialization();
        
        logger.info("Gemini Ethos API Server started on port {}", port);
    }
    
    /**
     * Stops the API server.
     */
    public void stop() {
        spark.Spark.stop();
        awaitStop();
        logger.info("Gemini Ethos API Server stopped");
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
            res.type("application/json");
        });
    }
    
    private void configureRoutes() {
        
        // ============ Health Check ============
        get("/health", (req, res) -> JsonUtils.toJson(Map.of(
            "status", "healthy",
            "service", "gemini-ethos",
            "version", "1.0.0",
            "activeSessions", agent.getActiveSessionCount()
        )));
        
        // ============ API Info ============
        get("/api/v1", (req, res) -> JsonUtils.toJson(Map.of(
            "name", "Gemini Ethos API",
            "version", "1.0.0",
            "description", "AI-powered ethical tourism guardian",
            "endpoints", Map.of(
                "POST /api/v1/analyze/frame", "Analyze a single image frame",
                "POST /api/v1/analyze/stream", "Analyze streaming video frame with metadata",
                "POST /api/v1/analyze/audio", "Process audio query for ethical guidance",
                "DELETE /api/v1/session/:sessionId", "Clear session history"
            )
        )));
        
        // ============ Frame Analysis (Simple) ============
        post("/api/v1/analyze/frame", this::handleFrameAnalysis);
        
        // ============ Streaming Frame Analysis (Full metadata) ============
        post("/api/v1/analyze/stream", this::handleStreamAnalysis);
        
        // ============ Audio Query Processing ============
        post("/api/v1/analyze/audio", this::handleAudioAnalysis);
        
        // ============ Session Management ============
        delete("/api/v1/session/:sessionId", (req, res) -> {
            String sessionId = req.params(":sessionId");
            agent.clearSession(sessionId);
            return JsonUtils.toJson(Map.of(
                "success", true,
                "message", "Session cleared: " + sessionId
            ));
        });
        
        // ============ Multipart Image Upload ============
        post("/api/v1/analyze/upload", this::handleMultipartUpload);
        
        // ============ Place Detection with Vision ============
        post("/api/v1/detect/place", this::handlePlaceDetection);
    }
    
    /**
     * Handles simple frame analysis with Base64 encoded image.
     * 
     * Request body: { 
     *   "imageBase64": "base64...", 
     *   "mimeType": "image/jpeg",
     *   "location": "galapagos",
     *   "geoLocation": { "latitude": 0.0, "longitude": 0.0, "accuracy": 10 },
     *   "timestamp": "2024-01-01T00:00:00Z"
     * }
     */
    private Object handleFrameAnalysis(Request req, Response res) {
        try {
            var json = com.google.gson.JsonParser.parseString(req.body()).getAsJsonObject();
            
            // Support both old 'imageData' and new 'imageBase64' field names
            String imageBase64 = null;
            if (json.has("imageBase64")) {
                imageBase64 = json.get("imageBase64").getAsString();
            } else if (json.has("imageData")) {
                imageBase64 = json.get("imageData").getAsString();
            }
            
            if (imageBase64 == null) {
                res.status(400);
                return JsonUtils.toJson(Map.of("error", "Missing 'imageBase64' or 'imageData' field"));
            }
            
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
            String sessionId = json.has("sessionId") ? json.get("sessionId").getAsString() : null;
            String mimeType = json.has("mimeType") ? json.get("mimeType").getAsString() : "image/jpeg";
            String location = json.has("location") ? json.get("location").getAsString() : null;
            
            // Parse GPS coordinates if provided
            StreamingFrame.GeoLocation geoLocation = null;
            if (json.has("geoLocation")) {
                var geoJson = json.getAsJsonObject("geoLocation");
                double lat = geoJson.has("latitude") ? geoJson.get("latitude").getAsDouble() : 0;
                double lng = geoJson.has("longitude") ? geoJson.get("longitude").getAsDouble() : 0;
                String locationName = geoJson.has("name") ? geoJson.get("name").getAsString() : null;
                geoLocation = new StreamingFrame.GeoLocation(lat, lng, locationName, location);
            } else if (location != null && !location.isEmpty()) {
                // Create GeoLocation from park name only
                geoLocation = new StreamingFrame.GeoLocation(0, 0, location, location);
            }
            
            CompletableFuture<AnalysisResult> future = agent.analyzeEnvironment(
                imageBytes, mimeType, sessionId, geoLocation
            );
            
            AnalysisResult result = future.get(ANALYSIS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return JsonUtils.toJson(result);
            
        } catch (Exception e) {
            logger.error("Error in frame analysis", e);
            res.status(500);
            return JsonUtils.toJson(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Handles streaming frame analysis with full metadata.
     * 
     * Request body: StreamingFrame JSON
     */
    private Object handleStreamAnalysis(Request req, Response res) {
        try {
            StreamingFrame frame = JsonUtils.parseStreamingFrame(req.body());
            
            if (!frame.isValid()) {
                res.status(400);
                return JsonUtils.toJson(Map.of(
                    "error", "Invalid frame data",
                    "details", "sessionId and imageData are required"
                ));
            }
            
            CompletableFuture<AnalysisResult> future = agent.analyzeFrame(frame);
            AnalysisResult result = future.get(ANALYSIS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            return JsonUtils.toJson(result);
            
        } catch (Exception e) {
            logger.error("Error in stream analysis", e);
            res.status(500);
            return JsonUtils.toJson(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Handles audio query for ethical guidance.
     * 
     * Request body: { "audioData": "base64...", "sessionId": "optional" }
     */
    private Object handleAudioAnalysis(Request req, Response res) {
        try {
            var json = com.google.gson.JsonParser.parseString(req.body()).getAsJsonObject();
            
            if (!json.has("audioData")) {
                res.status(400);
                return JsonUtils.toJson(Map.of("error", "Missing 'audioData' field"));
            }
            
            byte[] audioBytes = Base64.getDecoder().decode(json.get("audioData").getAsString());
            String sessionId = json.has("sessionId") ? json.get("sessionId").getAsString() : "default";
            
            CompletableFuture<String> future = agent.processAudioQuery(audioBytes, sessionId);
            String response = future.get(ANALYSIS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            return JsonUtils.toJson(Map.of(
                "sessionId", sessionId,
                "response", response
            ));
            
        } catch (Exception e) {
            logger.error("Error in audio analysis", e);
            res.status(500);
            return JsonUtils.toJson(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Handles multipart file upload for image analysis.
     */
    private Object handleMultipartUpload(Request req, Response res) {
        try {
            req.attribute("org.eclipse.jetty.multipartConfig", 
                new MultipartConfigElement(MULTIPART_CONFIG_LOCATION, MAX_FILE_SIZE, MAX_REQUEST_SIZE, 1024));
            
            Part imagePart = req.raw().getPart("image");
            if (imagePart == null) {
                res.status(400);
                return JsonUtils.toJson(Map.of("error", "Missing 'image' file part"));
            }
            
            String sessionId = req.queryParams("sessionId");
            String parkName = req.queryParams("parkName");
            String locationName = req.queryParams("locationName");
            
            // Read image bytes
            byte[] imageBytes;
            try (InputStream is = imagePart.getInputStream()) {
                imageBytes = is.readAllBytes();
            }
            
            String mimeType = imagePart.getContentType();
            if (mimeType == null) {
                mimeType = "image/jpeg";
            }
            
            // Build location if provided
            StreamingFrame.GeoLocation location = null;
            if (parkName != null || locationName != null) {
                double lat = parseDoubleOrDefault(req.queryParams("latitude"), 0);
                double lng = parseDoubleOrDefault(req.queryParams("longitude"), 0);
                location = new StreamingFrame.GeoLocation(lat, lng, locationName, parkName);
            }
            
            CompletableFuture<AnalysisResult> future = agent.analyzeEnvironment(
                imageBytes, mimeType, sessionId, location
            );
            
            AnalysisResult result = future.get(ANALYSIS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return JsonUtils.toJson(result);
            
        } catch (Exception e) {
            logger.error("Error in multipart upload", e);
            res.status(500);
            return JsonUtils.toJson(Map.of("error", e.getMessage()));
        }
    }
    
    private double parseDoubleOrDefault(String value, double defaultVal) {
        if (value == null || value.isBlank()) {
            return defaultVal;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
    
    /**
     * Handles place detection using Gemini Vision.
     * Identifies the location from the image before full analysis.
     * 
     * Request body: { "imageBase64": "base64...", "mimeType": "image/jpeg" }
     */
    private Object handlePlaceDetection(Request req, Response res) {
        try {
            var json = com.google.gson.JsonParser.parseString(req.body()).getAsJsonObject();
            
            String imageBase64 = null;
            if (json.has("imageBase64")) {
                imageBase64 = json.get("imageBase64").getAsString();
            } else if (json.has("imageData")) {
                imageBase64 = json.get("imageData").getAsString();
            }
            
            if (imageBase64 == null) {
                res.status(400);
                return JsonUtils.toJson(Map.of("error", "Missing 'imageBase64' field"));
            }
            
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
            String mimeType = json.has("mimeType") ? json.get("mimeType").getAsString() : "image/jpeg";
            
            // Use agent to detect place
            CompletableFuture<Map<String, Object>> future = agent.detectPlace(imageBytes, mimeType);
            Map<String, Object> result = future.get(ANALYSIS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            return JsonUtils.toJson(result);
            
        } catch (Exception e) {
            logger.error("Error in place detection", e);
            res.status(500);
            return JsonUtils.toJson(Map.of("error", e.getMessage()));
        }
    }
}
