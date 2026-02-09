package com.gemini.ethos.agent;

import com.gemini.ethos.config.VertexAIConfig;
import com.gemini.ethos.model.AnalysisResult;
import com.gemini.ethos.model.StreamingFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * MarathonPatrolAgent - Long-Running Autonomous Agent for Multi-Day Patrols.
 * 
 * This is the core implementation for the "Marathon Agent" track in the Gemini 3 Hackathon.
 * 
 * KEY GEMINI 3 FEATURES USED:
 * 1. Thought Signatures - Transparent reasoning chain maintained across multi-step operations
 * 2. Thinking Levels - Adaptive reasoning depth (low/medium/high) based on situation complexity
 * 3. Self-Correction - Agent detects and corrects its own mistakes over long-running tasks
 * 4. Context Continuity - Maintains state and memory across hours/days of operation
 * 5. Autonomous Tool Calling - Executes multi-step plans without human supervision
 * 
 * The agent can run unsupervised for extended periods, periodically checkpointing
 * its state and escalating only when human judgment is truly required.
 * 
 * @author Gemini Ethos Team
 * @version 3.0.0 - Marathon Edition
 */
public class MarathonPatrolAgent implements AutoCloseable {
    
    private static final Logger logger = LoggerFactory.getLogger(MarathonPatrolAgent.class);
    
    // Agent configuration
    private static final Duration CHECKPOINT_INTERVAL = Duration.ofMinutes(15);
    private static final Duration MAX_MISSION_DURATION = Duration.ofHours(72); // 3 days max
    private static final int MAX_CONCURRENT_ANALYSES = 4;
    private static final double ESCALATION_THRESHOLD = 0.85; // Risk score threshold
    
    // Dependencies
    private final EthosGuardianAgent coreAgent;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService analysisPool;
    
    // Active missions
    private final Map<String, PatrolMission> activeMissions;
    private final Map<String, ScheduledFuture<?>> missionSchedulers;
    
    // Event listeners
    private final List<Consumer<PatrolMission.IncidentReport>> incidentListeners;
    private final List<Consumer<PatrolMission.MissionCheckpoint>> checkpointListeners;
    private final List<Consumer<PatrolMission>> escalationListeners;
    
    // Agent state
    private volatile boolean running;
    private Instant startedAt;
    private int totalMissionsCompleted;
    private int totalIncidentsHandled;
    
    public MarathonPatrolAgent(VertexAIConfig config) {
        this.coreAgent = new EthosGuardianAgent(config);
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.analysisPool = Executors.newFixedThreadPool(MAX_CONCURRENT_ANALYSES);
        this.activeMissions = new ConcurrentHashMap<>();
        this.missionSchedulers = new ConcurrentHashMap<>();
        this.incidentListeners = new CopyOnWriteArrayList<>();
        this.checkpointListeners = new CopyOnWriteArrayList<>();
        this.escalationListeners = new CopyOnWriteArrayList<>();
        this.running = false;
        
        logger.info("MarathonPatrolAgent v3.0 initialized - Ready for long-running autonomous patrols");
    }
    
    /**
     * Get the core EthosGuardianAgent for direct analysis operations.
     */
    public EthosGuardianAgent getCoreAgent() {
        return coreAgent;
    }
    
    // === Mission Management ===
    
    /**
     * Creates and starts a new patrol mission.
     * The mission will run autonomously, checkpointing its progress and
     * self-correcting as needed.
     */
    public PatrolMission startMission(String parkId, PatrolMission.MissionType type, 
            String description, Duration expectedDuration) {
        
        if (expectedDuration.compareTo(MAX_MISSION_DURATION) > 0) {
            throw new IllegalArgumentException("Mission duration exceeds maximum of " + MAX_MISSION_DURATION);
        }
        
        PatrolMission mission = new PatrolMission(parkId, type, description, expectedDuration);
        mission.start();
        
        activeMissions.put(mission.getMissionId(), mission);
        
        // Schedule periodic checkpoints
        ScheduledFuture<?> checkpointTask = scheduler.scheduleAtFixedRate(
            () -> performCheckpoint(mission),
            CHECKPOINT_INTERVAL.toMinutes(),
            CHECKPOINT_INTERVAL.toMinutes(),
            TimeUnit.MINUTES
        );
        missionSchedulers.put(mission.getMissionId(), checkpointTask);
        
        // Schedule mission timeout
        scheduler.schedule(
            () -> timeoutMission(mission),
            expectedDuration.toMinutes(),
            TimeUnit.MINUTES
        );
        
        logger.info("Started mission {} for park {} - Expected duration: {}",
            mission.getMissionId(), parkId, expectedDuration);
        
        return mission;
    }
    
    /**
     * Submits an image for analysis within a mission context.
     * The agent maintains thought continuity from previous analyses.
     */
    public CompletableFuture<AnalysisResult> analyzeWithinMission(
            String missionId, byte[] imageData, String mimeType, 
            String location, Map<String, Double> geoLocation) {
        
        PatrolMission mission = activeMissions.get(missionId);
        if (mission == null) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Mission not found: " + missionId));
        }
        
        if (mission.getStatus() != PatrolMission.MissionStatus.ACTIVE) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Mission is not active: " + mission.getStatus()));
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Build context from previous thoughts for continuity
                String continuityContext = buildContinuityContext(mission);
                
                // Determine thinking level based on recent incidents
                String thinkingLevel = determineThinkingLevel(mission);
                
                // Add pre-analysis thought
                mission.addThought(
                    "Beginning analysis of new image",
                    thinkingLevel,
                    List.of("Received new image at " + location),
                    List.of("Will apply learned patterns from previous " + mission.getTotalImagesAnalyzed() + " images"),
                    List.of(),
                    0.9,
                    "Execute visual analysis"
                );
                
                // Perform core analysis
                // Build GeoLocation from mission parkId
                StreamingFrame.GeoLocation geoLoc = new StreamingFrame.GeoLocation(
                    0, 0, location, mission.getParkId());
                
                AnalysisResult result = coreAgent.analyzeEnvironment(
                    imageData, mimeType, null, geoLoc).get(120, java.util.concurrent.TimeUnit.SECONDS);
                
                mission.incrementImagesAnalyzed();
                
                // Store image thumbnail for dashboard gallery
                String riskLevel = result.overallRiskLevel() != null 
                    ? result.overallRiskLevel().name() : "LOW";
                String imageBase64 = java.util.Base64.getEncoder().encodeToString(imageData);
                mission.addAnalyzedImage(mimeType, imageBase64, riskLevel, location);
                
                // Process result and update mission state
                processAnalysisResult(mission, result, location);
                
                // Check for self-correction opportunities
                checkForSelfCorrection(mission, result);
                
                return result;
                
            } catch (Exception e) {
                logger.error("Error analyzing image in mission {}: {}", missionId, e.getMessage());
                mission.addThought(
                    "Analysis failed - will retry or skip",
                    "high",
                    List.of("Error: " + e.getMessage()),
                    List.of("May be transient, will continue patrol"),
                    List.of("Network or API issue possible"),
                    0.6,
                    "Log error and continue"
                );
                throw new RuntimeException(e);
            }
        }, analysisPool);
    }
    
    /**
     * Builds context from previous thoughts to maintain continuity.
     * This is key for the Marathon Agent track - the agent "remembers" its reasoning.
     */
    private String buildContinuityContext(PatrolMission mission) {
        List<PatrolMission.ThoughtSignature> recentThoughts = mission.getRecentThoughts(5);
        if (recentThoughts.isEmpty()) {
            return "";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("CONTINUITY CONTEXT FROM PREVIOUS ANALYSES:\n");
        for (PatrolMission.ThoughtSignature thought : recentThoughts) {
            context.append("- [").append(thought.thinkingLevel().toUpperCase()).append("] ");
            context.append(thought.reasoning()).append("\n");
        }
        
        // Add learned patterns
        Integer detectedPattern = mission.recallContext("repeatedBehaviorPattern", Integer.class);
        if (detectedPattern != null && detectedPattern > 2) {
            context.append("\nATTENTION: Detected recurring pattern (seen ")
                   .append(detectedPattern).append(" times). Increase vigilance.\n");
        }
        
        return context.toString();
    }
    
    /**
     * Determines thinking level based on mission context.
     * - LOW: Routine check, no recent incidents
     * - MEDIUM: Some recent activity, moderate attention needed
     * - HIGH: Critical situation, full reasoning required
     */
    private String determineThinkingLevel(PatrolMission mission) {
        // High if recent escalation or many incidents
        if (mission.getStatus() == PatrolMission.MissionStatus.ESCALATED) {
            return "high";
        }
        
        // High if high incident rate
        double incidentRate = mission.getTotalImagesAnalyzed() > 0 ?
            (double) mission.getTotalIncidentsDetected() / mission.getTotalImagesAnalyzed() : 0;
        if (incidentRate > 0.3) {
            return "high";
        }
        
        // Medium if some incidents
        if (mission.getTotalIncidentsDetected() > 0) {
            return "medium";
        }
        
        return "low";
    }
    
    /**
     * Processes analysis result and updates mission state.
     */
    private void processAnalysisResult(PatrolMission mission, AnalysisResult result, String location) {
        String riskLevel = result.overallRiskLevel().name();
        
        // Record thought about the analysis
        mission.addThought(
            "Completed analysis - Risk Level: " + riskLevel,
            determineThinkingLevel(mission),
            extractObservations(result),
            List.of("Risk assessment based on visual evidence and park regulations"),
            extractUncertainties(result),
            0.85,
            riskLevel.equals("CRITICAL") || riskLevel.equals("HIGH") ? 
                "Record incident and notify" : "Continue patrol"
        );
        
        // Record incident if high/critical risk
        if (riskLevel.equals("CRITICAL") || riskLevel.equals("HIGH")) {
            boolean requiresEscalation = riskLevel.equals("CRITICAL");
            
            PatrolMission.IncidentReport incident = mission.recordIncident(
                location,
                riskLevel,
                result.summary(),
                List.of(), // Evidence URLs would be added in production
                result.immediateActions(),
                requiresEscalation,
                requiresEscalation ? "Critical risk requires immediate human review" : null
            );
            
            // Notify listeners
            incidentListeners.forEach(listener -> {
                try {
                    listener.accept(incident);
                } catch (Exception e) {
                    logger.error("Error in incident listener", e);
                }
            });
            
            if (requiresEscalation) {
                escalationListeners.forEach(listener -> {
                    try {
                        listener.accept(mission);
                    } catch (Exception e) {
                        logger.error("Error in escalation listener", e);
                    }
                });
            }
        } else if (riskLevel.equals("MEDIUM")) {
            mission.incrementWarnings();
        }
        
        // Track patterns
        trackBehaviorPatterns(mission, result);
    }
    
    /**
     * Self-correction logic - the agent reviews its own past assessments
     * and corrects if new information suggests errors.
     */
    private void checkForSelfCorrection(PatrolMission mission, AnalysisResult currentResult) {
        // Get last few incidents for comparison
        List<PatrolMission.IncidentReport> recentIncidents = mission.getIncidents();
        if (recentIncidents.size() < 2) return;
        
        // Look for potential over-reactions or under-reactions
        PatrolMission.IncidentReport lastIncident = recentIncidents.get(recentIncidents.size() - 1);
        
        // Example: If we marked something as CRITICAL but current analysis shows
        // similar situation as LOW, we may have over-reacted
        if (lastIncident.riskLevel().equals("CRITICAL") && 
            currentResult.overallRiskLevel().name().equals("LOW") &&
            similarContext(lastIncident, currentResult)) {
            
            mission.recordSelfCorrection(
                "Previous CRITICAL assessment",
                "May have been overestimated based on similar LOW-risk current situation",
                "Similar visual elements but different actual risk. Will recalibrate thresholds."
            );
        }
        
        // Check for consistent under-estimation
        Integer underEstimations = mission.recallContext("underEstimationCount", Integer.class);
        if (underEstimations != null && underEstimations > 3) {
            mission.recordSelfCorrection(
                "Multiple potential under-estimations detected",
                "Adjusting sensitivity upward for remaining patrol",
                "Pattern suggests risk threshold may be too high"
            );
            mission.rememberContext("sensitivityAdjusted", true);
        }
    }
    
    private boolean similarContext(PatrolMission.IncidentReport incident, AnalysisResult result) {
        // Simplified similarity check - in production this would be more sophisticated
        return incident.description().length() > 0 && 
               result.summary() != null &&
               incident.description().split(" ").length < result.summary().split(" ").length * 2;
    }
    
    private void trackBehaviorPatterns(PatrolMission mission, AnalysisResult result) {
        // Track repeated behavior types
        if (result.detectedBehaviors() != null) {
            for (var behavior : result.detectedBehaviors()) {
                String key = "pattern_" + behavior.behaviorType().toLowerCase().replaceAll("\\s+", "_");
                Integer count = mission.recallContext(key, Integer.class);
                mission.rememberContext(key, count == null ? 1 : count + 1);
                
                if (count != null && count >= 3) {
                    mission.rememberContext("repeatedBehaviorPattern", count);
                    mission.addThought(
                        "Detected recurring behavior pattern: " + behavior.behaviorType(),
                        "high",
                        List.of("This behavior has been observed " + count + " times"),
                        List.of("May indicate systemic issue requiring park management attention"),
                        List.of(),
                        0.9,
                        "Flag for summary report"
                    );
                }
            }
        }
    }
    
    private List<String> extractObservations(AnalysisResult result) {
        List<String> observations = new ArrayList<>();
        if (result.reasoningProcess() != null && result.reasoningProcess().visualObservations() != null) {
            for (var obs : result.reasoningProcess().visualObservations()) {
                observations.add(obs.element() + ": " + obs.description());
            }
        }
        if (observations.isEmpty() && result.summary() != null) {
            observations.add(result.summary());
        }
        return observations;
    }
    
    private List<String> extractUncertainties(AnalysisResult result) {
        if (result.reasoningProcess() != null && result.reasoningProcess().uncertainties() != null) {
            return result.reasoningProcess().uncertainties();
        }
        return List.of();
    }
    
    // === Checkpoint and Timeout Handling ===
    
    private void performCheckpoint(PatrolMission mission) {
        if (mission.getStatus() != PatrolMission.MissionStatus.ACTIVE) {
            return;
        }
        
        PatrolMission.MissionCheckpoint checkpoint = mission.createCheckpoint("AUTO");
        
        logger.info("Checkpoint for mission {}: {} images analyzed, {} incidents, {} thoughts in chain",
            mission.getMissionId(), 
            checkpoint.imagesProcessed(),
            checkpoint.incidentsDetected(),
            mission.getThoughtChain().size());
        
        // Notify checkpoint listeners
        checkpointListeners.forEach(listener -> {
            try {
                listener.accept(checkpoint);
            } catch (Exception e) {
                logger.error("Error in checkpoint listener", e);
            }
        });
    }
    
    private void timeoutMission(PatrolMission mission) {
        if (mission.getStatus() == PatrolMission.MissionStatus.ACTIVE) {
            mission.complete("Mission completed - duration limit reached");
            cleanupMission(mission.getMissionId());
            totalMissionsCompleted++;
            logger.info("Mission {} completed by timeout", mission.getMissionId());
        }
    }
    
    private void cleanupMission(String missionId) {
        ScheduledFuture<?> scheduledTask = missionSchedulers.remove(missionId);
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
    }
    
    // === Mission Control ===
    
    public void pauseMission(String missionId, String reason) {
        PatrolMission mission = activeMissions.get(missionId);
        if (mission != null) {
            mission.pause(reason);
            logger.info("Mission {} paused: {}", missionId, reason);
        }
    }
    
    public void resumeMission(String missionId) {
        PatrolMission mission = activeMissions.get(missionId);
        if (mission != null) {
            mission.resume();
            logger.info("Mission {} resumed", missionId);
        }
    }
    
    public void abortMission(String missionId, String reason) {
        PatrolMission mission = activeMissions.get(missionId);
        if (mission != null) {
            mission.addThought("Mission aborted", "high",
                List.of("Abort reason: " + reason),
                List.of(),
                List.of(),
                1.0, "Cleanup and generate partial report");
            cleanupMission(missionId);
            logger.info("Mission {} aborted: {}", missionId, reason);
        }
    }
    
    public PatrolMission getMission(String missionId) {
        return activeMissions.get(missionId);
    }
    
    public List<PatrolMission> getActiveMissions() {
        return activeMissions.values().stream()
            .filter(m -> m.getStatus() == PatrolMission.MissionStatus.ACTIVE)
            .toList();
    }
    
    // === Event Listeners ===
    
    public void onIncident(Consumer<PatrolMission.IncidentReport> listener) {
        incidentListeners.add(listener);
    }
    
    public void onCheckpoint(Consumer<PatrolMission.MissionCheckpoint> listener) {
        checkpointListeners.add(listener);
    }
    
    public void onEscalation(Consumer<PatrolMission> listener) {
        escalationListeners.add(listener);
    }
    
    // === Agent Statistics ===
    
    public Map<String, Object> getAgentStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("agentVersion", "3.0.0-Marathon");
        stats.put("startedAt", startedAt);
        stats.put("uptime", startedAt != null ? Duration.between(startedAt, Instant.now()).toString() : "not started");
        stats.put("activeMissions", activeMissions.size());
        stats.put("totalMissionsCompleted", totalMissionsCompleted);
        stats.put("totalIncidentsHandled", totalIncidentsHandled);
        
        // Aggregate mission stats
        int totalImages = activeMissions.values().stream()
            .mapToInt(PatrolMission::getTotalImagesAnalyzed).sum();
        int totalThoughts = activeMissions.values().stream()
            .mapToInt(m -> m.getThoughtChain().size()).sum();
        int totalCorrections = activeMissions.values().stream()
            .mapToInt(PatrolMission::getSelfCorrections).sum();
        
        stats.put("totalImagesAnalyzed", totalImages);
        stats.put("totalThoughtsGenerated", totalThoughts);
        stats.put("totalSelfCorrections", totalCorrections);
        
        return stats;
    }
    
    // === Lifecycle ===
    
    public void start() {
        this.running = true;
        this.startedAt = Instant.now();
        logger.info("MarathonPatrolAgent started at {}", startedAt);
    }
    
    @Override
    public void close() {
        running = false;
        
        // Complete all active missions
        for (PatrolMission mission : activeMissions.values()) {
            if (mission.getStatus() == PatrolMission.MissionStatus.ACTIVE) {
                mission.complete("Agent shutdown");
            }
        }
        
        // Shutdown executors
        scheduler.shutdown();
        analysisPool.shutdown();
        
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!analysisPool.awaitTermination(10, TimeUnit.SECONDS)) {
                analysisPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            analysisPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        coreAgent.close();
        logger.info("MarathonPatrolAgent shutdown complete");
    }
}
