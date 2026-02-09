package com.gemini.ethos.tools;

import com.gemini.ethos.model.AnalysisResult.RegulationInfo;
import com.google.cloud.vertexai.api.FunctionDeclaration;
import com.google.cloud.vertexai.api.Schema;
import com.google.cloud.vertexai.api.Tool;
import com.google.cloud.vertexai.api.Type;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Function Calling tool for looking up local regulations of national parks.
 * Implements the lookupLocalRegulations function that Gemini can invoke.
 */
public class RegulationsLookupTool {
    
    private static final Logger logger = LoggerFactory.getLogger(RegulationsLookupTool.class);
    private static final Gson gson = new Gson();
    
    // Simulated regulations database (in production, this would connect to a real API/database)
    private static final Map<String, RegulationInfo> REGULATIONS_DB = new ConcurrentHashMap<>();
    
    static {
        // Initialize with sample data for major national parks
        initializeRegulationsDatabase();
    }
    
    /**
     * Creates the Tool definition for Gemini Function Calling.
     */
    public static Tool createTool() {
        FunctionDeclaration lookupFunction = FunctionDeclaration.newBuilder()
            .setName("lookupLocalRegulations")
            .setDescription(
                "Looks up local regulations, laws, and rules for a specific national park or protected area. " +
                "Use this function when you need to verify site-specific rules, " +
                "access restrictions, penalties for violations, or environmental and cultural protection regulations."
            )
            .setParameters(
                Schema.newBuilder()
                    .setType(Type.OBJECT)
                    .putProperties("parkName", Schema.newBuilder()
                        .setType(Type.STRING)
                        .setDescription("Name of the national park or protected area (e.g., 'Galápagos', 'Machu Picchu', 'Torres del Paine')")
                        .build())
                    .putProperties("region", Schema.newBuilder()
                        .setType(Type.STRING)
                        .setDescription("Country or region where the park is located (e.g., 'Ecuador', 'Peru', 'Chile')")
                        .build())
                    .putProperties("queryType", Schema.newBuilder()
                        .setType(Type.STRING)
                        .setDescription("Query type: 'wildlife' (fauna), 'flora' (vegetation), 'cultural' (cultural sites), 'general' (general rules), 'penalties' (sanctions)")
                        .build())
                    .addRequired("parkName")
                    .addRequired("region")
                    .build()
            )
            .build();
        
        return Tool.newBuilder()
            .addFunctionDeclarations(lookupFunction)
            .build();
    }
    
    /**
     * Executes the lookupLocalRegulations function asynchronously.
     * 
     * @param arguments JSON arguments from Gemini's function call
     * @return CompletableFuture with the regulation information
     */
    public static CompletableFuture<RegulationInfo> execute(JsonObject arguments) {
        return CompletableFuture.supplyAsync(() -> {
            String parkName = arguments.has("parkName") ? 
                arguments.get("parkName").getAsString() : "";
            String region = arguments.has("region") ? 
                arguments.get("region").getAsString() : "";
            String queryType = arguments.has("queryType") ? 
                arguments.get("queryType").getAsString() : "general";
            
            logger.info("Looking up regulations for park: {} in region: {}, type: {}", 
                parkName, region, queryType);
            
            // Normalize the key for lookup
            String key = normalizeKey(parkName, region);
            
            // Look up in our database
            RegulationInfo info = REGULATIONS_DB.get(key);
            
            if (info == null) {
                // Return generic regulations if specific park not found
                info = getGenericRegulations(parkName, region);
            }
            
            logger.info("Found regulations for {}: {} rules", parkName, 
                info.applicableRules().size());
            
            return info;
        });
    }
    
    /**
     * Converts RegulationInfo to JSON string for Gemini response.
     */
    public static String toJsonResponse(RegulationInfo info) {
        return gson.toJson(info);
    }
    
    private static String normalizeKey(String parkName, String region) {
        return (parkName.toLowerCase().trim() + "_" + region.toLowerCase().trim())
            .replaceAll("\\s+", "_");
    }
    
    private static void initializeRegulationsDatabase() {
        // Galápagos - Ecuador
        REGULATIONS_DB.put("galápagos_ecuador", new RegulationInfo(
            "Galápagos National Park",
            "Ecuador",
            List.of(
                "Maintain a minimum distance of 2 meters from all wildlife",
                "Touching, feeding, or disturbing animals is prohibited",
                "Walk only on marked trails",
                "Extraction of any natural material (rocks, sand, shells) is prohibited",
                "Flash photography near animals is prohibited",
                "Visits must be accompanied by a certified naturalist guide",
                "Smoking is prohibited in all park areas",
                "Mandatory luggage inspection to prevent invasive species"
            ),
            Map.of(
                "Touching wildlife", "$400-$2,000 USD",
                "Material extraction", "$500-$5,000 USD + legal prosecution",
                "Entry without guide", "$200 USD",
                "Feeding animals", "$300-$1,000 USD"
            ),
            "Ministry of Environment of Ecuador / Galápagos National Park Directorate"
        ));
        
        // Machu Picchu - Perú
        REGULATIONS_DB.put("machu_picchu_perú", new RegulationInfo(
            "Historic Sanctuary of Machu Picchu",
            "Peru",
            List.of(
                "Maximum visit time: 4 hours",
                "Bringing food inside is prohibited",
                "Touching archaeological structures is prohibited",
                "Jumping on or climbing Inca walls is not allowed",
                "Metal-tipped walking sticks are prohibited (use rubber tips)",
                "Yoga, meditation, or rituals without authorization are prohibited",
                "Drone flights are prohibited",
                "Follow established routes and signage",
                "Shouting or making loud noises is prohibited"
            ),
            Map.of(
                "Damage to structures", "Criminal prosecution + substantial fine",
                "Unauthorized drones", "Confiscation + $1,500 USD",
                "Exceeding visit time", "$50 USD",
                "Unauthorized rituals", "$200 USD + expulsion"
            ),
            "Ministry of Culture of Peru / SERNANP"
        ));
        
        // Torres del Paine - Chile
        REGULATIONS_DB.put("torres_del_paine_chile", new RegulationInfo(
            "Torres del Paine National Park",
            "Chile",
            List.of(
                "Fires outside designated areas are prohibited",
                "Registration with CONAF is mandatory before trekking",
                "Carry all trash out with you (Leave No Trace)",
                "Camping outside authorized campsites is prohibited",
                "Maintain a minimum distance of 100 meters from wildlife",
                "Feeding guanacos, pumas, or any animals is prohibited",
                "Camp stoves only in designated areas with CONAF certification",
                "Cutting vegetation or collecting flowers/plants is prohibited"
            ),
            Map.of(
                "Caused wildfire", "Criminal prosecution + massive fines",
                "Unauthorized camping", "$500-$2,000 USD",
                "Feeding wildlife", "$300 USD",
                "Failure to register with CONAF", "$100 USD"
            ),
            "CONAF Chile"
        ));
        
        // Amazonas - Perú/Brasil
        REGULATIONS_DB.put("amazonas_perú", new RegulationInfo(
            "Pacaya Samiria National Reserve",
            "Peru",
            List.of(
                "Entry only with an authorized tour operator",
                "Fishing without a special license is prohibited",
                "Hunting or capturing any species is prohibited",
                "Respect indigenous communities and their territories",
                "Do not photograph people without their consent",
                "Purchasing crafts made from animal parts is prohibited",
                "Use biodegradable insect repellent",
                "Do not dump waste into rivers"
            ),
            Map.of(
                "Illegal hunting", "Serious criminal prosecution",
                "Fishing without license", "$500-$3,000 USD",
                "Wildlife trafficking", "Confiscation + legal prosecution",
                "Unauthorized entry", "$400 USD"
            ),
            "SERNANP Peru / Native Communities"
        ));
        
        // Iguazú - Argentina
        REGULATIONS_DB.put("iguazú_argentina", new RegulationInfo(
            "Iguazú National Park",
            "Argentina",
            List.of(
                "Do not feed coatis or any other animals",
                "Keep belongings secure (coatis may steal them)",
                "Respect opening and closing hours",
                "Do not throw objects into the waterfalls",
                "Follow only enabled trails",
                "Swimming in the waterfalls is prohibited",
                "Drone use without special authorization is prohibited",
                "Respect safety signage"
            ),
            Map.of(
                "Feeding coatis", "$100-$300 USD",
                "Leaving trails", "Warning / Expulsion",
                "Unauthorized drones", "$500 USD + confiscation"
            ),
            "Argentina National Parks Administration"
        ));
    }
    
    private static RegulationInfo getGenericRegulations(String parkName, String region) {
        return new RegulationInfo(
            parkName,
            region,
            List.of(
                "Respect wildlife - maintain a safe distance",
                "Leave no trace - carry out everything you bring in",
                "Follow marked trails and signage",
                "Respect local communities and their culture",
                "Do not extract plants, rocks, or natural materials",
                "Practice responsible photography without disturbing the environment",
                "Consult specific regulations with local park rangers"
            ),
            Map.of(
                "General violations", "Vary depending on severity and local legislation"
            ),
            "Local environmental authority - Consult on site"
        );
    }
}
