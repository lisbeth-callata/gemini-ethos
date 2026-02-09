package com.gemini.ethos;

import com.gemini.ethos.agent.EthosGuardianAgent;
import com.gemini.ethos.api.EthosApiServer;
import com.gemini.ethos.api.MarathonApiServer;
import com.gemini.ethos.config.VertexAIConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main Application entry point for Gemini Ethos.
 * 
 * Gemini Ethos is an AI-powered responsible tourism guardian that uses
 * Google Vertex AI Gemini 3 to analyze video/images and provide proactive
 * ethical guidance to tourists visiting natural and cultural heritage sites.
 * 
 * ═══════════════════════════════════════════════════════════════
 * GEMINI 3 HACKATHON - MARATHON AGENT TRACK
 * ═══════════════════════════════════════════════════════════════
 * 
 * This application showcases Gemini 3's advanced capabilities:
 * - Thought Signatures: Transparent reasoning chain
 * - Thinking Levels: Adaptive reasoning depth (low/medium/high)
 * - Self-Correction: Agent detects and corrects its own mistakes
 * - Context Continuity: State maintained across hours/days
 * - Autonomous Tool Calling: Multi-step plans without supervision
 * 
 * Modes:
 * - STANDARD: Classic single-image analysis (default)
 * - MARATHON: Long-running autonomous patrol agent
 * 
 * Environment Variables Required:
 * - GOOGLE_CLOUD_PROJECT: Your Google Cloud project ID
 * - GOOGLE_CLOUD_LOCATION: (Optional) GCP region, defaults to us-central1
 * - ETHOS_SERVER_PORT: (Optional) Server port, defaults to 8080
 * - ETHOS_MODE: (Optional) "standard" or "marathon", defaults to "marathon"
 * 
 * Usage:
 *   java -jar gemini-ethos.jar
 *   java -jar gemini-ethos.jar --marathon   # Force marathon mode
 *   java -jar gemini-ethos.jar --standard   # Force standard mode
 * 
 * @author Gemini Ethos Team
 * @version 3.0.0 - Gemini 3 Hackathon Edition
 */
public class Application {
    
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    
    private static final int DEFAULT_PORT = 8080;
    private static final String ENV_SERVER_PORT = "ETHOS_SERVER_PORT";
    private static final String ENV_MODE = "ETHOS_MODE";
    
    private VertexAIConfig vertexConfig;
    private EthosGuardianAgent agent;
    private EthosApiServer standardServer;
    private MarathonApiServer marathonServer;
    private boolean marathonMode;
    
    public static void main(String[] args) {
        Application app = new Application();
        
        // Parse command line args
        boolean forceMarathon = false;
        boolean forceStandard = false;
        for (String arg : args) {
            if ("--marathon".equalsIgnoreCase(arg)) forceMarathon = true;
            if ("--standard".equalsIgnoreCase(arg)) forceStandard = true;
        }
        
        app.run(forceMarathon, forceStandard);
    }
    
    /**
     * Initializes and starts the application.
     */
    public void run(boolean forceMarathon, boolean forceStandard) {
        printBanner();
        
        try {
            // Determine mode
            marathonMode = determineMode(forceMarathon, forceStandard);
            
            // Initialize components
            initialize();
            
            // Register shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
            
            // Start the appropriate server
            int port = getServerPort();
            
            if (marathonMode) {
                marathonServer = new MarathonApiServer(vertexConfig, port);
                marathonServer.start();
                
                logger.info("═".repeat(65));
                logger.info("🚀 GEMINI 3 HACKATHON - MARATHON AGENT MODE");
                logger.info("═".repeat(65));
                logger.info("Gemini Ethos Marathon Agent is ready for long-running patrols!");
                logger.info("API v3 available at: http://localhost:{}/api/v3", port);
                logger.info("Health check: http://localhost:{}/health", port);
                logger.info("");
                logger.info("GEMINI 3 FEATURES ACTIVE:");
                logger.info("  ✓ Thought Signatures - Transparent reasoning chain");
                logger.info("  ✓ Thinking Levels - Adaptive depth (low/medium/high)");
                logger.info("  ✓ Self-Correction - Auto-detects and corrects mistakes");
                logger.info("  ✓ Context Continuity - State across hours/days");
                logger.info("  ✓ Autonomous Execution - Multi-step without supervision");
                logger.info("═".repeat(65));
            } else {
                standardServer = new EthosApiServer(agent, port);
                standardServer.start();
                
                logger.info("=".repeat(60));
                logger.info("Gemini Ethos is ready to protect our planet's heritage!");
                logger.info("API available at: http://localhost:{}/api/v1", port);
                logger.info("Health check: http://localhost:{}/health", port);
                logger.info("=".repeat(60));
            }
            
            // Keep the server running - wait indefinitely
            // The shutdown hook will handle cleanup on Ctrl+C
            logger.info("Press Ctrl+C to stop the server...");
            Thread.currentThread().join();
            
        } catch (InterruptedException e) {
            logger.info("Server interrupted, shutting down...");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Failed to start Gemini Ethos", e);
            System.exit(1);
        }
    }
    
    private boolean determineMode(boolean forceMarathon, boolean forceStandard) {
        if (forceMarathon) return true;
        if (forceStandard) return false;
        
        String modeEnv = System.getenv(ENV_MODE);
        if (modeEnv != null) {
            return "marathon".equalsIgnoreCase(modeEnv.trim());
        }
        
        // Default to marathon mode for the hackathon
        return true;
    }
    
    private void initialize() {
        logger.info("Initializing Gemini Ethos...");
        
        // Validate environment
        validateEnvironment();
        
        // Initialize Vertex AI configuration
        vertexConfig = new VertexAIConfig();
        logger.info("Vertex AI configured for project: {}", vertexConfig.getProjectId());
        
        // Initialize the Guardian Agent
        agent = new EthosGuardianAgent(vertexConfig);
        logger.info("EthosGuardianAgent initialized successfully");
    }
    
    private void validateEnvironment() {
        String projectId = System.getenv("GOOGLE_CLOUD_PROJECT");
        if (projectId == null || projectId.isBlank()) {
            logger.error("GOOGLE_CLOUD_PROJECT environment variable is not set");
            logger.error("Please set it to your Google Cloud project ID:");
            logger.error("  Windows: set GOOGLE_CLOUD_PROJECT=your-project-id");
            logger.error("  Linux/Mac: export GOOGLE_CLOUD_PROJECT=your-project-id");
            throw new IllegalStateException("Missing required environment variable: GOOGLE_CLOUD_PROJECT");
        }
        
        // Check for Google Cloud credentials
        String credentials = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (credentials == null || credentials.isBlank()) {
            logger.warn("GOOGLE_APPLICATION_CREDENTIALS is not set.");
            logger.warn("Using Application Default Credentials (ADC).");
            logger.warn("Run 'gcloud auth application-default login' if not authenticated.");
        }
    }
    
    private int getServerPort() {
        String portEnv = System.getenv(ENV_SERVER_PORT);
        if (portEnv != null && !portEnv.isBlank()) {
            try {
                return Integer.parseInt(portEnv.trim());
            } catch (NumberFormatException e) {
                logger.warn("Invalid port '{}', using default: {}", portEnv, DEFAULT_PORT);
            }
        }
        return DEFAULT_PORT;
    }
    
    private void shutdown() {
        logger.info("Shutting down Gemini Ethos...");
        
        try {
            if (marathonServer != null) {
                marathonServer.stop();
            }
            if (standardServer != null) {
                standardServer.stop();
            }
            if (agent != null) {
                agent.close();
            }
            logger.info("Gemini Ethos shutdown complete. Goodbye!");
        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        }
    }
    
    private void printBanner() {
        String banner = """
            
            ╔═══════════════════════════════════════════════════════════════╗
            ║                                                               ║
            ║   ██████╗ ███████╗███╗   ███╗██╗███╗   ██╗██╗  ██████╗        ║
            ║  ██╔════╝ ██╔════╝████╗ ████║██║████╗  ██║██║ ╚════██╗       ║
            ║  ██║  ███╗█████╗  ██╔████╔██║██║██╔██╗ ██║██║  █████╔╝       ║
            ║  ██║   ██║██╔══╝  ██║╚██╔╝██║██║██║╚██╗██║██║  ╚═══██╗       ║
            ║  ╚██████╔╝███████╗██║ ╚═╝ ██║██║██║ ╚████║██║ ██████╔╝       ║
            ║   ╚═════╝ ╚══════╝╚═╝     ╚═╝╚═╝╚═╝  ╚═══╝╚═╝ ╚═════╝        ║
            ║                                                               ║
            ║   ███████╗████████╗██╗  ██╗ ██████╗ ███████╗                   ║
            ║   ██╔════╝╚══██╔══╝██║  ██║██╔═══██╗██╔════╝                   ║
            ║   █████╗     ██║   ███████║██║   ██║███████╗                   ║
            ║   ██╔══╝     ██║   ██╔══██║██║   ██║╚════██║                   ║
            ║   ███████╗   ██║   ██║  ██║╚██████╔╝███████║                   ║
            ║   ╚══════╝   ╚═╝   ╚═╝  ╚═╝ ╚═════╝ ╚══════╝                   ║
            ║                                                               ║
            ║   🌿 MARATHON AGENT - Gemini 3 Hackathon Edition 🌍           ║
            ║   🏆 Long-Running Autonomous AI Patrol System                 ║
            ║                                                               ║
            ╚═══════════════════════════════════════════════════════════════╝
            """;
        System.out.println(banner);
    }
}

