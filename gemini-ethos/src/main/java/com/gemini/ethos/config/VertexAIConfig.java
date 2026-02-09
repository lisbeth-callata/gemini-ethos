package com.gemini.ethos.config;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration class for Google Cloud Vertex AI connection.
 * Uses environment variables for secure credential management.
 * 
 * Gemini 3 Flash Preview requires the GLOBAL endpoint with REST transport.
 * The global endpoint (aiplatform.googleapis.com) is the only location
 * where gemini-3-flash-preview is deployed.
 */
public class VertexAIConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(VertexAIConfig.class);
    
    // Environment variable names
    private static final String ENV_PROJECT_ID = "GOOGLE_CLOUD_PROJECT";
    private static final String ENV_LOCATION = "GOOGLE_CLOUD_LOCATION";
    
    // Gemini 3 models are ONLY available on the global endpoint
    private static final String DEFAULT_LOCATION = "global";
    
    // Global endpoint for Vertex AI (required for Gemini 3 models)
    private static final String GLOBAL_API_ENDPOINT = "aiplatform.googleapis.com";
    
    private final String projectId;
    private final String location;
    private VertexAI vertexAI;
    
    public VertexAIConfig() {
        this.projectId = getRequiredEnvVar(ENV_PROJECT_ID);
        this.location = getEnvVarOrDefault(ENV_LOCATION, DEFAULT_LOCATION);
        
        logger.info("Initializing Vertex AI with project: {} in location: {} (global endpoint)", projectId, location);
    }
    
    /**
     * Creates and returns a VertexAI client instance.
     * Uses lazy initialization for resource efficiency.
     * 
     * Configures the client with:
     * - Location: global (required for gemini-3-flash-preview)
     * - Transport: REST (global endpoint does not support gRPC)
     * - API Endpoint: aiplatform.googleapis.com (global, no regional prefix)
     */
    public synchronized VertexAI getVertexAI() {
        if (vertexAI == null) {
            try {
                vertexAI = new VertexAI.Builder()
                        .setProjectId(projectId)
                        .setLocation(location)
                        .setTransport(Transport.REST)
                        .setApiEndpoint(GLOBAL_API_ENDPOINT)
                        .build();
                logger.info("Vertex AI client initialized successfully (global endpoint, REST transport)");
            } catch (Exception e) {
                logger.error("Failed to initialize Vertex AI client", e);
                throw new RuntimeException("Cannot initialize Vertex AI: " + e.getMessage(), e);
            }
        }
        return vertexAI;
    }
    
    /**
     * Closes the Vertex AI client and releases resources.
     */
    public synchronized void close() {
        if (vertexAI != null) {
            try {
                vertexAI.close();
                logger.info("Vertex AI client closed successfully");
            } catch (Exception e) {
                logger.warn("Error closing Vertex AI client", e);
            }
            vertexAI = null;
        }
    }
    
    public String getProjectId() {
        return projectId;
    }
    
    public String getLocation() {
        return location;
    }
    
    private String getRequiredEnvVar(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                "Required environment variable '" + name + "' is not set. " +
                "Please set it before running the application."
            );
        }
        return value.trim();
    }
    
    private String getEnvVarOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value != null && !value.isBlank()) ? value.trim() : defaultValue;
    }
}
