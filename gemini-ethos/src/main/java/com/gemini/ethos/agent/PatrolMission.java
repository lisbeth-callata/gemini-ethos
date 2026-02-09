package com.gemini.ethos.agent;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PatrolMission - Represents a long-running autonomous patrol mission.
 * 
 * This is a key component for the "Marathon Agent" track in the Gemini 3 Hackathon.
 * Missions can span hours or days, with the agent maintaining continuity
 * and self-correcting across multi-step tool calls.
 * 
 * @author Gemini Ethos Team
 * @version 3.0.0
 */
public class PatrolMission {
    
    public enum MissionStatus {
        SCHEDULED,      // Mission is scheduled but not started
        ACTIVE,         // Currently running
        PAUSED,         // Temporarily paused (night, weather, etc.)
        COMPLETED,      // Successfully completed
        ABORTED,        // Stopped due to critical issue
        ESCALATED       // Requires human intervention
    }
    
    public enum MissionType {
        REAL_TIME_PATROL,       // Live camera/drone feed monitoring
        BATCH_ANALYSIS,         // Analyze batch of images from a day
        PREDICTIVE_PATROL,      // Predict high-risk times/locations
        INCIDENT_RESPONSE,      // Respond to reported incident
        EDUCATIONAL_OUTREACH    // Generate educational content
    }
    
    public record ThoughtSignature(
        String thoughtId,
        Instant timestamp,
        String thinkingLevel,      // "low", "medium", "high" - Gemini 3 feature
        String reasoning,
        List<String> observations,
        List<String> hypotheses,
        List<String> uncertainties,
        double confidence,
        String nextAction
    ) {}
    
    public record IncidentReport(
        String incidentId,
        Instant detectedAt,
        String location,
        String riskLevel,
        String description,
        List<String> evidenceUrls,
        List<String> recommendedActions,
        boolean requiresEscalation,
        String escalationReason
    ) {}
    
    public record MissionCheckpoint(
        String checkpointId,
        Instant timestamp,
        int imagesProcessed,
        int incidentsDetected,
        int warningsIssued,
        double averageRiskScore,
        String agentState,
        ThoughtSignature currentThought
    ) {}
    
    // Mission identity
    private final String missionId;
    private final MissionType type;
    private final String parkId;
    private final String description;
    
    // Mission timing
    private final Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private Duration expectedDuration;
    
    // Mission state
    private MissionStatus status;
    private int totalImagesAnalyzed;
    private int totalIncidentsDetected;
    private int totalWarningsIssued;
    private int totalEducationalInterventions;
    
    // Thought continuity - Key for Marathon Agent
    private final List<ThoughtSignature> thoughtChain;
    private final List<IncidentReport> incidents;
    private final List<MissionCheckpoint> checkpoints;
    private final Map<String, Object> contextMemory;
    
    // Self-correction tracking
    private int selfCorrections;
    private final List<String> correctionLog;

    // Analyzed image thumbnails for dashboard display
    public record AnalyzedImage(
        String imageId,
        Instant analyzedAt,
        String mimeType,
        String thumbnailBase64,  // first 200KB of image for preview
        String riskLevel,
        String location
    ) {}
    private final List<AnalyzedImage> analyzedImages;

    public PatrolMission(String parkId, MissionType type, String description, Duration expectedDuration) {
        this.missionId = "MISSION-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.parkId = parkId;
        this.type = type;
        this.description = description;
        this.expectedDuration = expectedDuration;
        this.createdAt = Instant.now();
        this.status = MissionStatus.SCHEDULED;
        
        this.thoughtChain = Collections.synchronizedList(new ArrayList<>());
        this.incidents = Collections.synchronizedList(new ArrayList<>());
        this.checkpoints = Collections.synchronizedList(new ArrayList<>());
        this.contextMemory = new ConcurrentHashMap<>();
        this.correctionLog = Collections.synchronizedList(new ArrayList<>());
        this.analyzedImages = Collections.synchronizedList(new ArrayList<>());
    }
    
    // === Mission Lifecycle ===
    
    public void start() {
        this.startedAt = Instant.now();
        this.status = MissionStatus.ACTIVE;
        addThought("Mission initialized", "high", 
            List.of("Starting patrol for " + parkId),
            List.of("Will monitor for tourist behavior violations"),
            List.of("Unknown traffic volume today"),
            0.95, "Begin image analysis loop");
    }
    
    public void pause(String reason) {
        this.status = MissionStatus.PAUSED;
        addThought("Mission paused", "medium",
            List.of("Pause triggered: " + reason),
            List.of(),
            List.of(),
            1.0, "Wait for resume signal");
    }
    
    public void resume() {
        this.status = MissionStatus.ACTIVE;
        addThought("Mission resumed", "medium",
            List.of("Resuming from checkpoint"),
            List.of("Continue monitoring from last state"),
            List.of(),
            0.9, "Resume image analysis");
    }
    
    public void complete(String summary) {
        this.completedAt = Instant.now();
        this.status = MissionStatus.COMPLETED;
        addThought("Mission completed", "high",
            List.of(summary),
            List.of(),
            List.of(),
            1.0, "Generate final report");
        createCheckpoint("FINAL");
    }
    
    public void escalate(String reason) {
        this.status = MissionStatus.ESCALATED;
        addThought("Mission escalated to human supervisor", "high",
            List.of("Critical situation detected"),
            List.of("Requires human judgment"),
            List.of("AI confidence insufficient for autonomous decision"),
            0.5, "Await human intervention");
    }
    
    // === Thought Chain Management (Thought Signatures) ===
    
    public ThoughtSignature addThought(String reasoning, String thinkingLevel,
            List<String> observations, List<String> hypotheses, 
            List<String> uncertainties, double confidence, String nextAction) {
        
        ThoughtSignature thought = new ThoughtSignature(
            "THOUGHT-" + (thoughtChain.size() + 1),
            Instant.now(),
            thinkingLevel,
            reasoning,
            observations,
            hypotheses,
            uncertainties,
            confidence,
            nextAction
        );
        thoughtChain.add(thought);
        return thought;
    }
    
    public ThoughtSignature getLastThought() {
        return thoughtChain.isEmpty() ? null : thoughtChain.get(thoughtChain.size() - 1);
    }
    
    public List<ThoughtSignature> getRecentThoughts(int count) {
        int size = thoughtChain.size();
        return thoughtChain.subList(Math.max(0, size - count), size);
    }
    
    // === Incident Management ===
    
    public IncidentReport recordIncident(String location, String riskLevel, 
            String description, List<String> evidenceUrls, 
            List<String> recommendedActions, boolean requiresEscalation,
            String escalationReason) {
        
        IncidentReport incident = new IncidentReport(
            "INC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            Instant.now(),
            location,
            riskLevel,
            description,
            evidenceUrls,
            recommendedActions,
            requiresEscalation,
            escalationReason
        );
        incidents.add(incident);
        totalIncidentsDetected++;
        
        if (requiresEscalation) {
            escalate(escalationReason);
        }
        
        return incident;
    }
    
    // === Self-Correction (Key Gemini 3 Feature) ===
    
    public void recordSelfCorrection(String originalAssessment, String correctedAssessment, String reason) {
        selfCorrections++;
        String log = String.format("[%s] SELF-CORRECTION #%d: '%s' -> '%s' | Reason: %s",
            Instant.now(), selfCorrections, originalAssessment, correctedAssessment, reason);
        correctionLog.add(log);
        
        addThought("Self-correction applied", "high",
            List.of("Previous assessment was inaccurate"),
            List.of("Updated analysis based on new evidence"),
            List.of("May require re-evaluation of similar past cases"),
            0.85, "Apply correction to future analyses");
    }
    
    // === Checkpoint Management ===
    
    public MissionCheckpoint createCheckpoint(String label) {
        ThoughtSignature currentThought = getLastThought();
        double avgRisk = incidents.stream()
            .mapToDouble(i -> riskLevelToScore(i.riskLevel()))
            .average()
            .orElse(0.0);
        
        MissionCheckpoint checkpoint = new MissionCheckpoint(
            "CP-" + label + "-" + checkpoints.size(),
            Instant.now(),
            totalImagesAnalyzed,
            totalIncidentsDetected,
            totalWarningsIssued,
            avgRisk,
            status.name(),
            currentThought
        );
        checkpoints.add(checkpoint);
        return checkpoint;
    }
    
    private double riskLevelToScore(String riskLevel) {
        return switch (riskLevel.toUpperCase()) {
            case "CRITICAL" -> 1.0;
            case "HIGH" -> 0.75;
            case "MEDIUM" -> 0.5;
            case "LOW" -> 0.25;
            default -> 0.0;
        };
    }
    
    // === Context Memory (For Long-Running Continuity) ===
    
    public void rememberContext(String key, Object value) {
        contextMemory.put(key, value);
    }
    
    public <T> T recallContext(String key, Class<T> type) {
        Object value = contextMemory.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }
    
    public void incrementImagesAnalyzed() {
        totalImagesAnalyzed++;
    }

    /**
     * Store an analyzed image for dashboard display.
     */
    public void addAnalyzedImage(String mimeType, String imageBase64, String riskLevel, String location) {
        analyzedImages.add(new AnalyzedImage(
            "IMG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            Instant.now(),
            mimeType,
            imageBase64,
            riskLevel,
            location
        ));
    }
    
    public void incrementWarnings() {
        totalWarningsIssued++;
    }
    
    public void incrementEducationalInterventions() {
        totalEducationalInterventions++;
    }
    
    // === Getters ===
    
    public String getMissionId() { return missionId; }
    public MissionType getType() { return type; }
    public String getParkId() { return parkId; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Duration getExpectedDuration() { return expectedDuration; }
    public MissionStatus getStatus() { return status; }
    public int getTotalImagesAnalyzed() { return totalImagesAnalyzed; }
    public int getTotalIncidentsDetected() { return totalIncidentsDetected; }
    public int getTotalWarningsIssued() { return totalWarningsIssued; }
    public int getTotalEducationalInterventions() { return totalEducationalInterventions; }
    public List<ThoughtSignature> getThoughtChain() { return new ArrayList<>(thoughtChain); }
    public List<IncidentReport> getIncidents() { return new ArrayList<>(incidents); }
    public List<MissionCheckpoint> getCheckpoints() { return new ArrayList<>(checkpoints); }
    public int getSelfCorrections() { return selfCorrections; }
    public List<String> getCorrectionLog() { return new ArrayList<>(correctionLog); }
    public List<AnalyzedImage> getAnalyzedImages() { return new ArrayList<>(analyzedImages); }
    
    public Duration getElapsedTime() {
        if (startedAt == null) return Duration.ZERO;
        Instant end = completedAt != null ? completedAt : Instant.now();
        return Duration.between(startedAt, end);
    }
    
    // === Mission Summary ===
    
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("missionId", missionId);
        summary.put("type", type.name());
        summary.put("parkId", parkId);
        summary.put("status", status.name());
        summary.put("elapsedTime", getElapsedTime().toString());
        summary.put("imagesAnalyzed", totalImagesAnalyzed);
        summary.put("incidentsDetected", totalIncidentsDetected);
        summary.put("warningsIssued", totalWarningsIssued);
        summary.put("educationalInterventions", totalEducationalInterventions);
        summary.put("selfCorrections", selfCorrections);
        summary.put("thoughtChainLength", thoughtChain.size());
        summary.put("checkpointsCreated", checkpoints.size());
        return summary;
    }
}
