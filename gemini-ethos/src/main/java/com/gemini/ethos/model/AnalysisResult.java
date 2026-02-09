package com.gemini.ethos.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents the result of an ethical environment analysis.
 * 
 * This is a comprehensive POJO designed for easy JSON serialization
 * to the frontend via SparkJava.
 * 
 * @author Gemini Ethos Team
 * @version 3.0.0 - Gemini 3 Marathon Agent Edition
 */
public record AnalysisResult(
    String sessionId,
    Instant timestamp,
    RiskLevel overallRiskLevel,
    List<DetectedBehavior> detectedBehaviors,
    List<EthicalGuideline> guidelines,
    List<String> immediateActions,
    RegulationInfo regulationInfo,
    EnvironmentalAlert environmentalAlert,
    ReasoningProcess reasoningProcess,
    CausalAnalysis causalAnalysis,
    String summary
) {
    
    /**
     * Environmental Alert Level returned by getEnvironmentalAlertLevel function.
     */
    public record EnvironmentalAlert(
        AlertLevel level,
        String justification,
        String technicalAnalysis,
        List<String> visualEvidence,
        float severityScore
    ) {
        public enum AlertLevel {
            LOW("No immediate threat to the ecosystem"),
            MEDIUM("Potential reversible impact"),
            HIGH("Significant risk to the ecosystem"),
            CRITICAL("Imminent or ongoing damage");
            
            private final String description;
            
            AlertLevel(String description) {
                this.description = description;
            }
            
            public String getDescription() {
                return description;
            }
        }
    }
    
    /**
     * Thought Signature - The AI's reasoning process before final response.
     * Implements "Chain of Thought" transparency.
     */
    public record ReasoningProcess(
        List<VisualObservation> visualObservations,
        List<String> inferenceChain,
        String contextualAssessment,
        String riskJustification,
        List<String> uncertainties
    ) {}
    
    /**
     * Individual visual observation with precise location details.
     */
    public record VisualObservation(
        String element,
        String description,
        String spatialLocation,
        float confidence,
        String relevanceToRisk
    ) {}
    
    /**
     * Cause-Effect Analysis for detected behaviors.
     */
    public record CausalAnalysis(
        String primaryCause,
        List<EffectChain> effectChains,
        String ecosystemSpecificImpact,
        String shortTermConsequence,
        String longTermConsequence,
        List<String> mitigationStrategies
    ) {}
    
    /**
     * Chain of effects from a detected behavior.
     */
    public record EffectChain(
        String cause,
        String immediateEffect,
        String secondaryEffect,
        String ecosystemImpact
    ) {}
    
    public enum RiskLevel {
        LOW("Responsible behavior detected"),
        MEDIUM("Caution recommended"),
        HIGH("Intervention suggested"),
        CRITICAL("Immediate action required");
        
        private final String description;
        
        RiskLevel(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public record DetectedBehavior(
        String behaviorType,
        String description,
        RiskLevel riskLevel,
        float confidence,
        String location
    ) {}
    
    public record EthicalGuideline(
        String category,
        String guideline,
        String culturalContext,
        String environmentalImpact
    ) {}
    
    public record RegulationInfo(
        String parkName,
        String region,
        List<String> applicableRules,
        Map<String, String> penalties,
        String source
    ) {}
    
    /**
     * Builder for creating AnalysisResult instances.
     */
    public static class Builder {
        private String sessionId;
        private Instant timestamp = Instant.now();
        private RiskLevel overallRiskLevel = RiskLevel.LOW;
        private List<DetectedBehavior> detectedBehaviors = List.of();
        private List<EthicalGuideline> guidelines = List.of();
        private List<String> immediateActions = List.of();
        private RegulationInfo regulationInfo;
        private EnvironmentalAlert environmentalAlert;
        private ReasoningProcess reasoningProcess;
        private CausalAnalysis causalAnalysis;
        private String summary = "";
        
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder overallRiskLevel(RiskLevel level) {
            this.overallRiskLevel = level;
            return this;
        }
        
        public Builder detectedBehaviors(List<DetectedBehavior> behaviors) {
            this.detectedBehaviors = behaviors;
            return this;
        }
        
        public Builder guidelines(List<EthicalGuideline> guidelines) {
            this.guidelines = guidelines;
            return this;
        }
        
        public Builder immediateActions(List<String> actions) {
            this.immediateActions = actions;
            return this;
        }
        
        public Builder regulationInfo(RegulationInfo info) {
            this.regulationInfo = info;
            return this;
        }
        
        public Builder environmentalAlert(EnvironmentalAlert alert) {
            this.environmentalAlert = alert;
            return this;
        }
        
        public Builder reasoningProcess(ReasoningProcess process) {
            this.reasoningProcess = process;
            return this;
        }
        
        public Builder causalAnalysis(CausalAnalysis analysis) {
            this.causalAnalysis = analysis;
            return this;
        }
        
        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }
        
        public AnalysisResult build() {
            return new AnalysisResult(
                sessionId, timestamp, overallRiskLevel,
                detectedBehaviors, guidelines, immediateActions,
                regulationInfo, environmentalAlert, reasoningProcess,
                causalAnalysis, summary
            );
        }
    }
}
