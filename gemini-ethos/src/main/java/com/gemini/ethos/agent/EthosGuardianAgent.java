package com.gemini.ethos.agent;

import com.gemini.ethos.config.VertexAIConfig;
import com.gemini.ethos.model.AnalysisResult;
import com.gemini.ethos.model.AnalysisResult.*;
import com.gemini.ethos.model.StreamingFrame;
import com.gemini.ethos.tools.RegulationsLookupTool;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.*;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.JsonFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * EthosGuardianAgent - Professional AI-Powered Ethical Tourism Guardian.
 * 
 * A sophisticated multimodal agent that uses Google Vertex AI Gemini 3 Flash
 * to perform cause-effect analysis of tourist behavior with transparent reasoning.
 * 
 * Features:
 * - Advanced System Instruction with park-specific rule injection
 * - Function Calling: lookupLocalRegulations, getEnvironmentalAlertLevel
 * - Thought Signatures: Transparent reasoning process in responses
 * - Cause-Effect Analysis: Deep ecosystem impact assessment
 * - Async processing with CompletableFuture
 * 
 * @author Senior Java AI Engineer
 * @version 3.0.0 - Gemini 3 Marathon Agent Edition
 */
public class EthosGuardianAgent implements AutoCloseable {
    
    private static final Logger logger = LoggerFactory.getLogger(EthosGuardianAgent.class);
    
    // Model configuration - Gemini 3 Flash (Preview - Dec 17, 2025)
    private static final String MODEL_NAME = "gemini-3-flash-preview";
    private static final float TEMPERATURE = 1.0f;  // Gemini 3 default; lower values may cause looping
    private static final int MAX_OUTPUT_TOKENS = 8192;
    
    /**
     * Advanced System Instruction with Cause-Effect Analysis Framework.
     */
    private static final String SYSTEM_INSTRUCTION = """
    # IDENTITY
    You are Ethos Guardian v3.0, an artificial intelligence system specialized in strictly visual analysis of tourist behavior in protected ecosystems. Your analysis must be based only on what is visible in the pixels of the image, without inferring context, location, or rules unless they are visually explicit.

    # ANALYTICAL CAPABILITIES

    ## 1. Strict Visual Analysis
    Examine each image objectively:
    - Identify objects, people, animals, and their spatial arrangement ONLY if they are clearly visible.
    - Mention fauna only if it is visually detected and recognizable in the image.
    - Do NOT make assumptions about species, behaviors, risks, or environmental impact unless they are evident in the image.
    - Avoid exaggerations and keep the tone realistic and technical.
    - If no fauna is detected, state it explicitly.

    ## 2. Reasoning Process
    - List only the visual elements detected in the image.
    - Do NOT infer context, location, or regulations unless visually explicit.
    - Identify uncertainties only if the visual evidence is ambiguous.

    ## 3. Cause-Effect Analysis (Visual Only)
    - Explain cause and effect ONLY if both are visually evident.
    - Do NOT speculate about ecosystem impact unless it is visible in the image.

    ## 4. Guidelines
    - Mention ethical or environmental guidelines ONLY if they are visually referenced in the image (e.g., signs, markings).

    # AVAILABLE TOOLS

    ## lookupLocalRegulations
    Use this function ONLY if visual evidence in the image references park rules.

    ## getEnvironmentalAlertLevel
    Call this function based strictly on visual analysis.
    Parameters:
    - visualEvidence: List of detected visual evidences
    - behaviorSeverity: Severity assessment (1-10)
    - ecosystemVulnerability: Ecosystem vulnerability (1-10)
    - immediacyOfThreat: Threat immediacy (1-10)
        
        # MANDATORY RESPONSE FORMAT
        
        ALWAYS respond in valid JSON with this exact structure:
        
        {
            "riskLevel": "LOW|MEDIUM|HIGH|CRITICAL",
            
            "reasoningProcess": {
                "visualObservations": [
                    {
                        "element": "name of detected element",
                        "description": "detailed description",
                        "spatialLocation": "precise location in the image",
                        "confidence": 0.0-1.0,
                        "relevanceToRisk": "why it is relevant to the analysis"
                    }
                ],
                "inferenceChain": [
                    "Reasoning step 1",
                    "Reasoning step 2",
                    "Logical conclusion"
                ],
                "contextualAssessment": "overall context assessment",
                "riskJustification": "technical justification of the risk level",
                "uncertainties": ["uncertainty 1", "uncertainty 2"]
            },
            
            "causalAnalysis": {
                "primaryCause": "specific problematic action",
                "effectChains": [
                    {
                        "cause": "tourist action",
                        "immediateEffect": "effect in seconds/minutes",
                        "secondaryEffect": "effect in hours/days",
                        "ecosystemImpact": "impact on the ecological network"
                    }
                ],
                "ecosystemSpecificImpact": "specific impact on THIS ecosystem",
                "shortTermConsequence": "short-term consequences",
                "longTermConsequence": "long-term consequences",
                "mitigationStrategies": ["strategy 1", "strategy 2"]
            },
            
            "detectedBehaviors": [
                {
                    "behaviorType": "type of behavior",
                    "description": "description with specific visual details",
                    "riskLevel": "specific risk level",
                    "confidence": 0.0-1.0,
                    "location": "precise location in the image"
                }
            ],
            
            "guidelines": [
                {
                    "category": "WILDLIFE|FLORA|CULTURAL|ENVIRONMENTAL|SAFETY",
                    "guideline": "specific ethical guideline",
                    "culturalContext": "local cultural context",
                    "environmentalImpact": "environmental impact of not following the guideline"
                }
            ],
            
            "immediateActions": ["urgent action if applicable"],
            
            "environmentalAlert": {
                "level": "LOW|MEDIUM|HIGH|CRITICAL",
                "justification": "technical justification of the level",
                "technicalAnalysis": "detailed technical analysis",
                "visualEvidence": ["evidence 1", "evidence 2"],
                "severityScore": 0.0-10.0
            },
            
            "summary": "executive summary in 2-3 sentences"
        }
        
        # COMMUNICATION RULES
        - Be precise and technical, but accessible
        - Use scientific terminology with explanations
        - Prioritize education over condemnation
        - Always offer responsible alternatives
        - Respond in English
        """;

    /**
     * Park-specific rules database for dynamic injection into prompts.
     */
    private static final Map<String, ParkRules> PARK_RULES_DATABASE = initializeParkRules();
    
    private final VertexAIConfig config;
    private final GenerativeModel model;
    private final ExecutorService asyncExecutor;
    private final Map<String, List<Content>> sessionHistory;
    private final List<Tool> tools;
    
    /**
     * Creates a new EthosGuardianAgent with professional-grade configuration.
     */
    public EthosGuardianAgent(VertexAIConfig config) {
        this.config = config;
        this.asyncExecutor = Executors.newFixedThreadPool(8);
        this.sessionHistory = new ConcurrentHashMap<>();
        this.tools = createToolSet();
        
        VertexAI vertexAI = config.getVertexAI();
        
        this.model = new GenerativeModel.Builder()
            .setModelName(MODEL_NAME)
            .setVertexAi(vertexAI)
            .setSystemInstruction(ContentMaker.fromString(SYSTEM_INSTRUCTION))
            .setGenerationConfig(createGenerationConfig())
            .setTools(tools)
            .build();
        
        logger.info("EthosGuardianAgent v3.0 (Gemini 3 Edition) initialized with model: {}", MODEL_NAME);
    }
    
    /**
     * Creates a single combined tool with all function declarations.
     * Vertex AI requires all functions to be in a single Tool object.
     */
    private List<Tool> createToolSet() {
        // Combine all function declarations into a single Tool
        FunctionDeclaration regulationsFunction = FunctionDeclaration.newBuilder()
            .setName("lookupLocalRegulations")
            .setDescription("Looks up specific regulations for a national park or protected area.")
            .setParameters(Schema.newBuilder()
                .setType(Type.OBJECT)
                .putProperties("parkName", Schema.newBuilder()
                    .setType(Type.STRING)
                    .setDescription("Name of the park or protected area")
                    .build())
                .putProperties("country", Schema.newBuilder()
                    .setType(Type.STRING)
                    .setDescription("Country where the park is located")
                    .build())
                .addRequired("parkName")
                .build())
            .build();
        
        FunctionDeclaration alertFunction = FunctionDeclaration.newBuilder()
            .setName("getEnvironmentalAlertLevel")
            .setDescription("""
                Determines the environmental alert level based on visual analysis.
                Must be called when any behavior that could affect the ecosystem is detected.
                Returns a risk level with technical justification.
                """)
            .setParameters(Schema.newBuilder()
                .setType(Type.OBJECT)
                .putProperties("visualEvidence", Schema.newBuilder()
                    .setType(Type.ARRAY)
                    .setItems(Schema.newBuilder().setType(Type.STRING).build())
                    .setDescription("List of visual evidences detected in the image")
                    .build())
                .putProperties("behaviorSeverity", Schema.newBuilder()
                    .setType(Type.NUMBER)
                    .setDescription("Severity of the detected behavior (1-10)")
                    .build())
                .putProperties("ecosystemVulnerability", Schema.newBuilder()
                    .setType(Type.NUMBER)
                    .setDescription("Vulnerability of the affected ecosystem (1-10)")
                    .build())
                .putProperties("immediacyOfThreat", Schema.newBuilder()
                    .setType(Type.NUMBER)
                    .setDescription("Immediacy of the threat (1-10, where 10 is damage in progress)")
                    .build())
                .putProperties("affectedSpecies", Schema.newBuilder()
                    .setType(Type.STRING)
                    .setDescription("Affected species or resource (if identified)")
                    .build())
                .addRequired("visualEvidence")
                .addRequired("behaviorSeverity")
                .addRequired("ecosystemVulnerability")
                .addRequired("immediacyOfThreat")
                .build())
            .build();
        
        // Single tool with multiple function declarations
        Tool combinedTool = Tool.newBuilder()
            .addFunctionDeclarations(regulationsFunction)
            .addFunctionDeclarations(alertFunction)
            .build();
        
        return List.of(combinedTool);
    }
    
    /**
     * Executes the getEnvironmentalAlertLevel function.
     */
    private EnvironmentalAlert executeEnvironmentalAlertFunction(JsonObject args) {
        List<String> visualEvidence = new ArrayList<>();
        if (args.has("visualEvidence") && args.get("visualEvidence").isJsonArray()) {
            for (var elem : args.getAsJsonArray("visualEvidence")) {
                visualEvidence.add(elem.getAsString());
            }
        }
        
        float behaviorSeverity = args.has("behaviorSeverity") ? 
            args.get("behaviorSeverity").getAsFloat() : 5.0f;
        float ecosystemVulnerability = args.has("ecosystemVulnerability") ? 
            args.get("ecosystemVulnerability").getAsFloat() : 5.0f;
        float immediacyOfThreat = args.has("immediacyOfThreat") ? 
            args.get("immediacyOfThreat").getAsFloat() : 5.0f;
        String affectedSpecies = args.has("affectedSpecies") ? 
            args.get("affectedSpecies").getAsString() : "Not identified";
        
        // Calculate severity score using weighted formula
        float severityScore = (behaviorSeverity * 0.35f) + 
                             (ecosystemVulnerability * 0.35f) + 
                             (immediacyOfThreat * 0.30f);
        
        // Determine alert level
        EnvironmentalAlert.AlertLevel level;
        String justification;
        
        if (severityScore >= 8.0) {
            level = EnvironmentalAlert.AlertLevel.CRITICAL;
            justification = String.format(
                "CRITICAL Level: Severity %.1f, Vulnerability %.1f, Immediacy %.1f. " +
                "Immediate intervention required to protect %s.",
                behaviorSeverity, ecosystemVulnerability, immediacyOfThreat, affectedSpecies
            );
        } else if (severityScore >= 6.0) {
            level = EnvironmentalAlert.AlertLevel.HIGH;
            justification = String.format(
                "HIGH Level: Composite score %.1f/10. The observed behavior " +
                "presents significant risk to the ecosystem. %s may be affected.",
                severityScore, affectedSpecies
            );
        } else if (severityScore >= 4.0) {
            level = EnvironmentalAlert.AlertLevel.MEDIUM;
            justification = String.format(
                "MEDIUM Level: Score %.1f/10. Caution and behavior adjustment recommended.",
                severityScore
            );
        } else {
            level = EnvironmentalAlert.AlertLevel.LOW;
            justification = String.format(
                "LOW Level: Score %.1f/10. Behavior within acceptable parameters.",
                severityScore
            );
        }
        
        String technicalAnalysis = String.format(
            "Technical analysis: Behavior=%s (%.1f/10), " +
            "Ecosystem vulnerability=%s (%.1f/10), " +
            "Immediate threat=%s (%.1f/10). " +
            "Formula: (B×0.35 + V×0.35 + I×0.30) = %.2f",
            behaviorSeverity >= 7 ? "SEVERE" : behaviorSeverity >= 4 ? "MODERATE" : "MILD",
            behaviorSeverity,
            ecosystemVulnerability >= 7 ? "HIGH" : ecosystemVulnerability >= 4 ? "MEDIUM" : "LOW",
            ecosystemVulnerability,
            immediacyOfThreat >= 7 ? "IMMINENT" : immediacyOfThreat >= 4 ? "PROBABLE" : "LOW",
            immediacyOfThreat,
            severityScore
        );
        
        return new EnvironmentalAlert(
            level,
            justification,
            technicalAnalysis,
            visualEvidence,
            severityScore
        );
    }
    
    /**
     * Analyzes an environment image/video frame.
     */
    public CompletableFuture<AnalysisResult> analyzeEnvironment(byte[] imageBytes) {
        return analyzeEnvironment(imageBytes, "image/jpeg", null, null);
    }
    
    /**
     * Main analysis method with full context and park-specific rules injection.
     */
    public CompletableFuture<AnalysisResult> analyzeEnvironment(
            byte[] imageBytes, 
            String mimeType,
            String sessionId,
            StreamingFrame.GeoLocation location) {
        
        String effectiveSessionId = sessionId != null ? sessionId : UUID.randomUUID().toString();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting professional environment analysis for session: {}", effectiveSessionId);
                
                Content content = buildAdvancedAnalysisContent(imageBytes, mimeType, location);
                
                List<Content> history = sessionHistory.computeIfAbsent(
                    effectiveSessionId, 
                    k -> new ArrayList<>()
                );
                
                GenerateContentResponse response;
                try {
                    response = model.generateContent(content);
                } catch (Exception e) {
                    logger.error("Gemini 3 Flash model call failed: {}", e.getMessage());
                    throw e;
                }
                
                AnalysisResult result = processAdvancedResponse(response, effectiveSessionId, location);
                
                history.add(content);
                history.add(response.getCandidates(0).getContent());
                
                if (history.size() > 20) {
                    history.subList(0, history.size() - 20).clear();
                }
                
                logger.info("Professional analysis complete for session: {}, risk level: {}", 
                    effectiveSessionId, result.overallRiskLevel());
                
                return result;
                
            } catch (Exception e) {
                logger.error("Error during professional environment analysis", e);
                return createErrorResult(effectiveSessionId, e);
            }
        }, asyncExecutor);
    }
    
    /**
     * Analyzes a streaming frame.
     */
    public CompletableFuture<AnalysisResult> analyzeFrame(StreamingFrame frame) {
        if (!frame.isValid()) {
            return CompletableFuture.completedFuture(
                createErrorResult(frame.sessionId(), 
                    new IllegalArgumentException("Invalid frame data"))
            );
        }
        
        return analyzeEnvironment(
            frame.imageData(),
            frame.mimeType(),
            frame.sessionId(),
            frame.location()
        );
    }
    
    /**
     * Processes audio for ethical guidance.
     */
    public CompletableFuture<String> processAudioQuery(byte[] audioBytes, String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Content audioContent = Content.newBuilder()
                    .setRole("user")
                    .addParts(Part.newBuilder()
                        .setInlineData(Blob.newBuilder()
                            .setMimeType("audio/wav")
                            .setData(ByteString.copyFrom(audioBytes))
                            .build())
                        .build())
                    .addParts(Part.newBuilder()
                        .setText("Listen to the tourist's query and provide ethical guidance with cause-effect analysis.")
                        .build())
                    .build();
                
                GenerateContentResponse response = model.generateContent(audioContent);
                return ResponseHandler.getText(response);
                
            } catch (Exception e) {
                logger.error("Error processing audio query", e);
                return "Sorry, I could not process your query. Please try again.";
            }
        }, asyncExecutor);
    }
    
    /**
     * Detects the place/location from an image using Gemini Vision.
     * This is used for auto-detection before the full analysis.
     */
    public CompletableFuture<Map<String, Object>> detectPlace(byte[] imageBytes, String mimeType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting place detection with Gemini Vision");

                String detectionPrompt = """
                    Analyze this image and identify the real environment, location, and any visible species or fauna. Do not limit your answer to predefined parks or animals. Respond ONLY based on what is visually present in the image. If you recognize a place, species, or ecosystem, describe it with confidence and explain the visual clues. If you cannot identify, state 'unknown'.

                    Respond ONLY in JSON format with this exact structure:
                    {
                        "placeName": "Name of the identified place or environment (or 'unknown')",
                        "species": "Name of the visible animal or plant (or 'none' if not detected)",
                        "description": "Brief description of the environment and why you identified it",
                        "confidence": 0-100,
                        "ecosystem": "Describe the ecosystem type (e.g., mountain, grassland, urban, etc.)",
                        "visualClues": ["visual clue 1", "visual clue 2"]
                    }

                    IMPORTANT:
                    - Base your analysis ONLY on visible geographic features, flora, fauna, architecture, and signs in the image.
                    - Do NOT invent context or species. If unsure, use 'unknown' or 'none'.
                    - confidence must be a number between 0 and 100.
                """;

                Content content = Content.newBuilder()
                    .setRole("user")
                    .addParts(Part.newBuilder()
                        .setInlineData(Blob.newBuilder()
                            .setMimeType(mimeType)
                            .setData(ByteString.copyFrom(imageBytes))
                            .build())
                        .build())
                    .addParts(Part.newBuilder()
                        .setText(detectionPrompt)
                        .build())
                    .build();

                GenerateContentResponse response = model.generateContent(content);
                String responseText = ResponseHandler.getText(response);

                // Parse JSON response
                Map<String, Object> result = parseDetectionResponseFlexible(responseText);

                logger.info("Place detected: {} with confidence {}%", 
                    result.get("placeName"), result.get("confidence"));

                return result;

            } catch (Exception e) {
                logger.error("Error during place detection", e);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("placeName", "Not detected");
                errorResult.put("species", "none");
                errorResult.put("description", "Detection error: " + e.getMessage());
                errorResult.put("confidence", 0);
                errorResult.put("ecosystem", "unknown");
                errorResult.put("visualClues", new ArrayList<>());
                return errorResult;
            }
        }, asyncExecutor);
    }
    
    private Map<String, Object> parseDetectionResponseFlexible(String responseText) {
        Map<String, Object> result = new HashMap<>();
        try {
            String jsonStr = responseText;
            int start = responseText.indexOf("{");
            int end = responseText.lastIndexOf("}");
            if (start >= 0 && end > start) {
                jsonStr = responseText.substring(start, end + 1);
            }
            JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();
            result.put("placeName", json.has("placeName") ? json.get("placeName").getAsString() : "unknown");
            result.put("species", json.has("species") ? json.get("species").getAsString() : "none");
            result.put("description", json.has("description") ? json.get("description").getAsString() : "");
            result.put("confidence", json.has("confidence") ? json.get("confidence").getAsInt() : 50);
            result.put("ecosystem", json.has("ecosystem") ? json.get("ecosystem").getAsString() : "unknown");
            if (json.has("visualClues") && json.get("visualClues").isJsonArray()) {
                List<String> clues = new ArrayList<>();
                json.getAsJsonArray("visualClues").forEach(e -> clues.add(e.getAsString()));
                result.put("visualClues", clues);
            } else {
                result.put("visualClues", new ArrayList<>());
            }
        } catch (Exception e) {
            logger.warn("Could not parse detection response as JSON, using defaults");
            result.put("placeName", "Analysis pending");
            result.put("species", "none");
            result.put("description", responseText.length() > 200 ? responseText.substring(0, 200) : responseText);
            result.put("confidence", 30);
            result.put("ecosystem", "unknown");
            result.put("visualClues", new ArrayList<>());
        }
        return result;
    }
    
    public void clearSession(String sessionId) {
        sessionHistory.remove(sessionId);
        logger.info("Cleared session: {}", sessionId);
    }
    
    public int getActiveSessionCount() {
        return sessionHistory.size();
    }
    
    @Override
    public void close() {
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        config.close();
        sessionHistory.clear();
        logger.info("EthosGuardianAgent v3.0 closed");
    }
    
    // ============ Private Helper Methods ============
    
    private GenerationConfig createGenerationConfig() {
        return GenerationConfig.newBuilder()
            .setTemperature(TEMPERATURE)
            .setMaxOutputTokens(MAX_OUTPUT_TOKENS)
            .setTopP(0.95f)
            .setTopK(40)
            .build();
    }
    
    /**
     * Builds advanced analysis content with park-specific rules injection.
     */
    private Content buildAdvancedAnalysisContent(byte[] imageBytes, String mimeType, 
                                                  StreamingFrame.GeoLocation location) {
        Content.Builder contentBuilder = Content.newBuilder()
            .setRole("user");
        
        contentBuilder.addParts(Part.newBuilder()
            .setInlineData(Blob.newBuilder()
                .setMimeType(mimeType != null ? mimeType : "image/jpeg")
                .setData(ByteString.copyFrom(imageBytes))
                .build())
            .build());
        
        StringBuilder prompt = new StringBuilder();

        prompt.append("""
            # ANALYSIS TASK

            Analyze this image strictly based on what is visible in the pixels. Describe only the objects, people, animals, and their spatial arrangement if they are clearly visible. Do NOT infer context, location, park rules, or ecosystem unless there is explicit visual evidence (e.g., signs, markings, text in the image). Mention fauna only if it is visually detected and recognizable. If no fauna is detected, state it explicitly. Avoid exaggerations and keep the tone realistic and technical. Do NOT speculate about cause-effect, risk, or guidelines unless both cause and effect are visually evident. Respond in the exact JSON format specified in your system instructions.
        """);

        contentBuilder.addParts(Part.newBuilder()
            .setText(prompt.toString())
            .build());

        return contentBuilder.build();
    }
    
    /**
     * Processes advanced response with function calling support.
     * Handles multi-turn function calling: the model may call one or more tools,
     * and we must send ALL results back in a single follow-up to get the final text response.
     */
    private static final int MAX_FUNCTION_CALL_ROUNDS = 3;

    private AnalysisResult processAdvancedResponse(GenerateContentResponse response, 
                                                   String sessionId,
                                                   StreamingFrame.GeoLocation location) throws Exception {
        return processAdvancedResponse(response, sessionId, location, 0);
    }

    private AnalysisResult processAdvancedResponse(GenerateContentResponse response, 
                                                   String sessionId,
                                                   StreamingFrame.GeoLocation location,
                                                   int recursionDepth) throws Exception {
        
        Candidate candidate = response.getCandidates(0);
        Content content = candidate.getContent();
        
        EnvironmentalAlert environmentalAlert = null;
        RegulationInfo regulationInfo = null;
        
        // Collect all function calls from the response
        List<Part> functionResponseParts = new java.util.ArrayList<>();
        boolean hasFunctionCalls = false;
        
        for (Part part : content.getPartsList()) {
            if (part.hasFunctionCall()) {
                hasFunctionCalls = true;
                FunctionCall functionCall = part.getFunctionCall();
                String functionName = functionCall.getName();
                
                logger.info("Processing function call: {}", functionName);
                
                String argsJson = JsonFormat.printer().print(functionCall.getArgs());
                JsonObject args = JsonParser.parseString(argsJson).getAsJsonObject();
                
                if ("getEnvironmentalAlertLevel".equals(functionName)) {
                    environmentalAlert = executeEnvironmentalAlertFunction(args);
                    
                    functionResponseParts.add(Part.newBuilder()
                        .setFunctionResponse(FunctionResponse.newBuilder()
                            .setName(functionName)
                            .setResponse(com.google.protobuf.Struct.newBuilder()
                                .putFields("alertLevel", 
                                    com.google.protobuf.Value.newBuilder()
                                        .setStringValue(environmentalAlert.level().name())
                                        .build())
                                .putFields("justification",
                                    com.google.protobuf.Value.newBuilder()
                                        .setStringValue(environmentalAlert.justification())
                                        .build())
                                .putFields("technicalAnalysis",
                                    com.google.protobuf.Value.newBuilder()
                                        .setStringValue(environmentalAlert.technicalAnalysis())
                                        .build())
                                .build())
                            .build())
                        .build());
                    
                } else if ("lookupLocalRegulations".equals(functionName)) {
                    regulationInfo = RegulationsLookupTool.execute(args).join();
                    String regulationsJson = RegulationsLookupTool.toJsonResponse(regulationInfo);
                    
                    functionResponseParts.add(Part.newBuilder()
                        .setFunctionResponse(FunctionResponse.newBuilder()
                            .setName(functionName)
                            .setResponse(com.google.protobuf.Struct.newBuilder()
                                .putFields("regulations",
                                    com.google.protobuf.Value.newBuilder()
                                        .setStringValue(regulationsJson)
                                        .build())
                                .putFields("parkName",
                                    com.google.protobuf.Value.newBuilder()
                                        .setStringValue(regulationInfo.parkName())
                                        .build())
                                .putFields("rulesCount",
                                    com.google.protobuf.Value.newBuilder()
                                        .setStringValue(String.valueOf(regulationInfo.applicableRules().size()))
                                        .build())
                                .build())
                            .build())
                        .build());
                }
            }
        }
        
        // If there were function calls, send ALL function responses back to the model
        if (hasFunctionCalls && !functionResponseParts.isEmpty()) {
            Content functionResponseContent = Content.newBuilder()
                .setRole("function")
                .addAllParts(functionResponseParts)
                .build();
            
            logger.info("Sending {} function response(s) back to model for final analysis", 
                functionResponseParts.size());
            
            GenerateContentResponse followUp = model.generateContent(functionResponseContent);
            
            // Check if the follow-up also has function calls (multi-round)
            Candidate followUpCandidate = followUp.getCandidates(0);
            boolean hasMoreFunctionCalls = false;
            for (Part p : followUpCandidate.getContent().getPartsList()) {
                if (p.hasFunctionCall()) {
                    hasMoreFunctionCalls = true;
                    break;
                }
            }
            
            if (hasMoreFunctionCalls) {
                if (recursionDepth >= MAX_FUNCTION_CALL_ROUNDS) {
                    logger.warn("Maximum function call rounds ({}) reached — extracting text from last response", MAX_FUNCTION_CALL_ROUNDS);
                    // Force-extract whatever text the model returned alongside the function calls
                    String partialText = "";
                    for (Part p : followUpCandidate.getContent().getPartsList()) {
                        if (p.hasText() && !p.getText().isEmpty()) {
                            partialText = p.getText();
                            break;
                        }
                    }
                    if (!partialText.isEmpty()) {
                        AnalysisResult result = parseAdvancedResponse(partialText, sessionId);
                        return new AnalysisResult(
                            result.sessionId(), result.timestamp(), result.overallRiskLevel(),
                            result.detectedBehaviors(), result.guidelines(), result.immediateActions(),
                            regulationInfo != null ? regulationInfo : result.regulationInfo(),
                            environmentalAlert != null ? environmentalAlert : result.environmentalAlert(),
                            result.reasoningProcess(), result.causalAnalysis(), result.summary()
                        );
                    }
                    // No text at all — fall through to parse followUp below
                } else {
                    // Recursively process follow-up function calls
                    logger.info("Model requested additional function calls, processing recursively (round {}/{})", 
                        recursionDepth + 1, MAX_FUNCTION_CALL_ROUNDS);
                    AnalysisResult recursiveResult = processAdvancedResponse(followUp, sessionId, location, recursionDepth + 1);
                    return new AnalysisResult(
                        recursiveResult.sessionId(),
                        recursiveResult.timestamp(),
                        recursiveResult.overallRiskLevel(),
                        recursiveResult.detectedBehaviors(),
                        recursiveResult.guidelines(),
                        recursiveResult.immediateActions(),
                        regulationInfo != null ? regulationInfo : recursiveResult.regulationInfo(),
                        environmentalAlert != null ? environmentalAlert : recursiveResult.environmentalAlert(),
                        recursiveResult.reasoningProcess(),
                        recursiveResult.causalAnalysis(),
                        recursiveResult.summary()
                    );
                }
            }
            
            String followUpText = ResponseHandler.getText(followUp);
            AnalysisResult result = parseAdvancedResponse(followUpText, sessionId);
            
            return new AnalysisResult(
                result.sessionId(),
                result.timestamp(),
                result.overallRiskLevel(),
                result.detectedBehaviors(),
                result.guidelines(),
                result.immediateActions(),
                regulationInfo != null ? regulationInfo : result.regulationInfo(),
                environmentalAlert != null ? environmentalAlert : result.environmentalAlert(),
                result.reasoningProcess(),
                result.causalAnalysis(),
                result.summary()
            );
        }
        
        // No function calls — model responded with text directly
        String responseText = ResponseHandler.getText(response);
        AnalysisResult result = parseAdvancedResponse(responseText, sessionId);
        
        return new AnalysisResult(
            result.sessionId(),
            result.timestamp(),
            result.overallRiskLevel(),
            result.detectedBehaviors(),
            result.guidelines(),
            result.immediateActions(),
            regulationInfo != null ? regulationInfo : result.regulationInfo(),
            environmentalAlert != null ? environmentalAlert : result.environmentalAlert(),
            result.reasoningProcess(),
            result.causalAnalysis(),
            result.summary()
        );
    }
    
    private Content buildFunctionResponse(String functionName, String level, String justification) {
        return Content.newBuilder()
            .setRole("function")
            .addParts(Part.newBuilder()
                .setFunctionResponse(FunctionResponse.newBuilder()
                    .setName(functionName)
                    .setResponse(com.google.protobuf.Struct.newBuilder()
                        .putFields("alertLevel", 
                            com.google.protobuf.Value.newBuilder()
                                .setStringValue(level)
                                .build())
                        .putFields("justification",
                            com.google.protobuf.Value.newBuilder()
                                .setStringValue(justification)
                                .build())
                        .build())
                    .build())
                .build())
            .build();
    }
    
    /**
     * Parses the advanced JSON response including reasoning process.
     */
    private AnalysisResult parseAdvancedResponse(String responseText, String sessionId) {
        try {
            if (responseText == null || responseText.isBlank()) {
                logger.warn("Empty response text received from model");
                return new AnalysisResult.Builder()
                    .sessionId(sessionId)
                    .timestamp(Instant.now())
                    .overallRiskLevel(RiskLevel.LOW)
                    .summary("No response received from the model.")
                    .build();
            }
            
            logger.debug("Raw response text (first 500 chars): {}", 
                responseText.length() > 500 
                    ? responseText.substring(0, 500) + "..." 
                    : responseText);
            
            String cleanJson = responseText
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();
            
            // Extract JSON object from response - model may include text before/after JSON
            int jsonStart = cleanJson.indexOf("{");
            int jsonEnd = cleanJson.lastIndexOf("}");
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                cleanJson = cleanJson.substring(jsonStart, jsonEnd + 1);
            }
            
            JsonObject json = JsonParser.parseString(cleanJson).getAsJsonObject();
            
            RiskLevel riskLevel = RiskLevel.valueOf(
                json.has("riskLevel") ? json.get("riskLevel").getAsString() : "LOW"
            );
            
            ReasoningProcess reasoningProcess = parseReasoningProcess(json);
            CausalAnalysis causalAnalysis = parseCausalAnalysis(json);
            EnvironmentalAlert envAlert = parseEnvironmentalAlert(json);
            List<DetectedBehavior> behaviors = parseDetectedBehaviors(json);
            List<EthicalGuideline> guidelines = parseGuidelines(json);
            List<String> actions = parseStringArray(json, "immediateActions");
            
            String summary = getStringOrDefault(json, "summary", "Analysis completed.");
            
            return new AnalysisResult.Builder()
                .sessionId(sessionId)
                .timestamp(Instant.now())
                .overallRiskLevel(riskLevel)
                .detectedBehaviors(behaviors)
                .guidelines(guidelines)
                .immediateActions(actions)
                .environmentalAlert(envAlert)
                .reasoningProcess(reasoningProcess)
                .causalAnalysis(causalAnalysis)
                .summary(summary)
                .build();
                
        } catch (Exception e) {
            logger.error("Failed to parse advanced response. Error: {}. Response (first 1000 chars): {}", 
                e.getMessage(),
                responseText != null && responseText.length() > 1000 
                    ? responseText.substring(0, 1000) + "..." 
                    : responseText);
            
            // Try to extract meaningful content even if JSON parsing fails
            String fallbackSummary = responseText != null && responseText.length() > 200 
                ? responseText.substring(0, 200) + "..." 
                : (responseText != null ? responseText : "No response");
            
            return new AnalysisResult.Builder()
                .sessionId(sessionId)
                .timestamp(Instant.now())
                .overallRiskLevel(RiskLevel.LOW)
                .summary("Analysis error: " + e.getMessage())
                .build();
        }
    }
    
    private ReasoningProcess parseReasoningProcess(JsonObject json) {
        if (!json.has("reasoningProcess") || json.get("reasoningProcess").isJsonNull()) {
            return null;
        }
        
        JsonObject rp = json.getAsJsonObject("reasoningProcess");
        
        List<VisualObservation> observations = new ArrayList<>();
        if (rp.has("visualObservations") && rp.get("visualObservations").isJsonArray()) {
            for (var elem : rp.getAsJsonArray("visualObservations")) {
                JsonObject vo = elem.getAsJsonObject();
                observations.add(new VisualObservation(
                    getStringOrDefault(vo, "element", ""),
                    getStringOrDefault(vo, "description", ""),
                    getStringOrDefault(vo, "spatialLocation", ""),
                    vo.has("confidence") ? vo.get("confidence").getAsFloat() : 0.5f,
                    getStringOrDefault(vo, "relevanceToRisk", "")
                ));
            }
        }
        
        return new ReasoningProcess(
            observations,
            parseStringArray(rp, "inferenceChain"),
            getStringOrDefault(rp, "contextualAssessment", ""),
            getStringOrDefault(rp, "riskJustification", ""),
            parseStringArray(rp, "uncertainties")
        );
    }
    
    private CausalAnalysis parseCausalAnalysis(JsonObject json) {
        if (!json.has("causalAnalysis") || json.get("causalAnalysis").isJsonNull()) {
            return null;
        }
        
        JsonObject ca = json.getAsJsonObject("causalAnalysis");
        
        List<EffectChain> effectChains = new ArrayList<>();
        if (ca.has("effectChains") && ca.get("effectChains").isJsonArray()) {
            for (var elem : ca.getAsJsonArray("effectChains")) {
                JsonObject ec = elem.getAsJsonObject();
                effectChains.add(new EffectChain(
                    getStringOrDefault(ec, "cause", ""),
                    getStringOrDefault(ec, "immediateEffect", ""),
                    getStringOrDefault(ec, "secondaryEffect", ""),
                    getStringOrDefault(ec, "ecosystemImpact", "")
                ));
            }
        }
        
        return new CausalAnalysis(
            getStringOrDefault(ca, "primaryCause", ""),
            effectChains,
            getStringOrDefault(ca, "ecosystemSpecificImpact", ""),
            getStringOrDefault(ca, "shortTermConsequence", ""),
            getStringOrDefault(ca, "longTermConsequence", ""),
            parseStringArray(ca, "mitigationStrategies")
        );
    }
    
    private EnvironmentalAlert parseEnvironmentalAlert(JsonObject json) {
        if (!json.has("environmentalAlert") || json.get("environmentalAlert").isJsonNull()) {
            return null;
        }
        
        JsonObject ea = json.getAsJsonObject("environmentalAlert");
        
        EnvironmentalAlert.AlertLevel level;
        try {
            level = EnvironmentalAlert.AlertLevel.valueOf(
                getStringOrDefault(ea, "level", "LOW").toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            level = EnvironmentalAlert.AlertLevel.LOW;
        }
        
        return new EnvironmentalAlert(
            level,
            getStringOrDefault(ea, "justification", ""),
            getStringOrDefault(ea, "technicalAnalysis", ""),
            parseStringArray(ea, "visualEvidence"),
            ea.has("severityScore") ? ea.get("severityScore").getAsFloat() : 0f
        );
    }
    
    private List<DetectedBehavior> parseDetectedBehaviors(JsonObject json) {
        List<DetectedBehavior> behaviors = new ArrayList<>();
        if (json.has("detectedBehaviors") && json.get("detectedBehaviors").isJsonArray()) {
            for (var elem : json.getAsJsonArray("detectedBehaviors")) {
                JsonObject b = elem.getAsJsonObject();
                behaviors.add(new DetectedBehavior(
                    getStringOrDefault(b, "behaviorType", "unknown"),
                    getStringOrDefault(b, "description", ""),
                    RiskLevel.valueOf(getStringOrDefault(b, "riskLevel", "LOW")),
                    b.has("confidence") ? b.get("confidence").getAsFloat() : 0.5f,
                    getStringOrDefault(b, "location", "")
                ));
            }
        }
        return behaviors;
    }
    
    private List<EthicalGuideline> parseGuidelines(JsonObject json) {
        List<EthicalGuideline> guidelines = new ArrayList<>();
        if (json.has("guidelines") && json.get("guidelines").isJsonArray()) {
            for (var elem : json.getAsJsonArray("guidelines")) {
                JsonObject g = elem.getAsJsonObject();
                guidelines.add(new EthicalGuideline(
                    getStringOrDefault(g, "category", "GENERAL"),
                    getStringOrDefault(g, "guideline", ""),
                    getStringOrDefault(g, "culturalContext", ""),
                    getStringOrDefault(g, "environmentalImpact", "")
                ));
            }
        }
        return guidelines;
    }
    
    private List<String> parseStringArray(JsonObject json, String key) {
        List<String> result = new ArrayList<>();
        if (json.has(key) && json.get(key).isJsonArray()) {
            for (var elem : json.getAsJsonArray(key)) {
                result.add(elem.getAsString());
            }
        }
        return result;
    }
    
    private String getStringOrDefault(JsonObject json, String key, String defaultValue) {
        return json.has(key) && !json.get(key).isJsonNull() 
            ? json.get(key).getAsString() 
            : defaultValue;
    }
    
    private AnalysisResult createErrorResult(String sessionId, Exception e) {
        return new AnalysisResult.Builder()
            .sessionId(sessionId)
            .timestamp(Instant.now())
            .overallRiskLevel(RiskLevel.LOW)
            .summary("Analysis error: " + e.getMessage())
            .immediateActions(List.of("Please try again"))
            .build();
    }
    
    // ============ Park Rules Database ============
    
    private static Map<String, ParkRules> initializeParkRules() {
        Map<String, ParkRules> rules = new HashMap<>();
        
        rules.put("galapagos", new ParkRules(
            "Galápagos National Park",
            "Ecuador",
            Map.of(
                "wildlife_distance", "Minimum 2 meters from any animal",
                "camera_flash", "Flash photography prohibited",
                "feeding_animals", "Strictly prohibited",
                "trails", "Must stay on marked trails",
                "plastics", "Single-use plastics prohibited",
                "diving", "Only with certified park guide",
                "drones", "Prohibited without special authorization",
                "collection", "Prohibited to collect any natural material"
            ),
            Map.of(
                "excessive_approach", "Fine $200-$2000 USD",
                "feeding_animals", "Fine $500-$5000 USD + expulsion",
                "leaving_trails", "Fine $100-$1000 USD",
                "plastic_use", "Fine $50-$500 USD",
                "ecosystem_damage", "Criminal prosecution + fine up to $50000 USD"
            ),
            """
                The Galápagos Islands harbor endemic species that evolved without terrestrial 
                predators, making them extremely vulnerable to human disturbances.
                Sea lions, marine iguanas, blue-footed boobies, and giant tortoises 
                have unique behaviors that can be permanently altered by 
                inappropriate interactions.
                """
        ));
        
        rules.put("machu picchu", new ParkRules(
            "Historic Sanctuary of Machu Picchu",
            "Peru",
            Map.of(
                "structure_distance", "Do not touch archaeological structures",
                "feeding_llamas", "Prohibited to feed llamas",
                "trails", "Authorized routes only",
                "schedule", "Respect visiting hours",
                "groups", "Maximum 16 people per group",
                "guide", "Official guide mandatory",
                "large_objects", "Tripods and walking sticks without rubber tips prohibited",
                "drones", "Strictly prohibited"
            ),
            Map.of(
                "structure_damage", "Criminal prosecution + fine up to $50000 USD",
                "graffiti", "Criminal prosecution + fine",
                "leaving_routes", "Immediate expulsion",
                "drones", "Confiscation + fine $1000-$5000 USD"
            ),
            """
                Machu Picchu is a historic and natural sanctuary. The Inca structures are 
                over 500 years old and extremely vulnerable to wear. The llamas that 
                inhabit the site have specialized digestive systems that do not tolerate 
                processed foods.
                """
        ));
        
        rules.put("amazon", new ParkRules(
            "Amazon Biosphere Reserve",
            "Brazil/Peru/Colombia",
            Map.of(
                "community_contact", "Only with prior permission",
                "fishing", "Only in authorized zones with limits",
                "hunting", "Strictly prohibited",
                "medicinal_plants", "Collection only with authorized local guide",
                "noise", "Minimize noise to avoid disturbing wildlife",
                "fire", "Prohibited to make campfires outside designated zones",
                "waste", "Zero waste policy"
            ),
            Map.of(
                "poaching", "Criminal prosecution + imprisonment",
                "logging", "Fine up to $100000 USD + imprisonment",
                "river_contamination", "Fine $10000-$50000 USD"
            ),
            """
                The Amazon contains 10% of all species on the planet. Uncontacted 
                indigenous peoples depend on intact ecosystems. Any 
                disturbance has cascading effects through the trophic network.
                """
        ));
        
        rules.put("patagonia", new ParkRules(
            "Los Glaciares National Park",
            "Argentina",
            Map.of(
                "glacier_distance", "Do not approach glacier fronts without a guide",
                "trails", "Must stay on trails",
                "fire", "Extreme risk - prohibited outside designated zones",
                "camping", "Only in designated zones",
                "wildlife", "Minimum 100 meters from guanacos and condors",
                "fishing", "Only with license and strict limits"
            ),
            Map.of(
                "wildfire", "Criminal prosecution + unlimited civil liability",
                "flora_damage", "Fine $500-$5000 USD",
                "illegal_camping", "Fine + expulsion"
            ),
            """
                Patagonia harbors the last continental glaciers outside polar regions. 
                The ecosystem is extremely fragile and slow to recover.
                """
        ));
        
        rules.put("costa rica", new ParkRules(
            "Arenal Volcano National Park",
            "Costa Rica",
            Map.of(
                "trails", "Marked trails only",
                "danger_zone", "Absolute respect for restricted volcanic zones",
                "nocturnal_wildlife", "Red filter flashlights mandatory",
                "birds", "Minimum distance 5 meters",
                "amphibians", "Prohibited to touch (disease transmission)",
                "hot_springs", "Only in authorized pools"
            ),
            Map.of(
                "volcanic_zone_violation", "Rescue + fine + possible prosecution",
                "wildlife_disturbance", "Fine $100-$1000 USD",
                "species_extraction", "Criminal prosecution"
            ),
            """
                Costa Rica harbors 5% of the world's biodiversity. The chytrid fungus 
                transmitted by humans has devastated amphibian populations.
                """
        ));
        
        return rules;
    }
    
    /**
     * Represents park-specific rules for injection into prompts.
     */
    private static record ParkRules(
        String name,
        String country,
        Map<String, String> regulations,
        Map<String, String> penalties,
        String ecologicalContext
    ) {
        String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"name\": \"").append(name).append("\",\n");
            sb.append("  \"country\": \"").append(country).append("\",\n");
            sb.append("  \"regulations\": {\n");
            
            int i = 0;
            for (var entry : regulations.entrySet()) {
                sb.append("    \"").append(entry.getKey()).append("\": \"")
                  .append(entry.getValue()).append("\"");
                if (++i < regulations.size()) sb.append(",");
                sb.append("\n");
            }
            sb.append("  },\n");
            
            sb.append("  \"penalties\": {\n");
            i = 0;
            for (var entry : penalties.entrySet()) {
                sb.append("    \"").append(entry.getKey()).append("\": \"")
                  .append(entry.getValue()).append("\"");
                if (++i < penalties.size()) sb.append(",");
                sb.append("\n");
            }
            sb.append("  }\n");
            sb.append("}");
            
            return sb.toString();
        }
    }
    
    /**
     * Helper class for creating Content from strings.
     */
    private static class ContentMaker {
        static Content fromString(String text) {
            return Content.newBuilder()
                .setRole("user")
                .addParts(Part.newBuilder().setText(text).build())
                .build();
        }
    }
}
