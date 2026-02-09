package com.gemini.ethos.api;

import com.gemini.ethos.model.StreamingFrame;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Base64;

/**
 * JSON utilities for API serialization/deserialization.
 */
public class JsonUtils {
    
    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(Instant.class, new InstantAdapter())
        .registerTypeAdapter(byte[].class, new ByteArrayAdapter())
        .setPrettyPrinting()
        .create();
    
    public static String toJson(Object object) {
        return GSON.toJson(object);
    }
    
    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }
    
    public static StreamingFrame parseStreamingFrame(String json) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        
        String sessionId = getStringOrNull(obj, "sessionId");
        byte[] imageData = obj.has("imageData") 
            ? Base64.getDecoder().decode(obj.get("imageData").getAsString())
            : new byte[0];
        String mimeType = getStringOrDefault(obj, "mimeType", "image/jpeg");
        Instant capturedAt = obj.has("capturedAt") 
            ? Instant.parse(obj.get("capturedAt").getAsString())
            : Instant.now();
        
        StreamingFrame.GeoLocation location = null;
        if (obj.has("location") && !obj.get("location").isJsonNull()) {
            JsonObject loc = obj.getAsJsonObject("location");
            location = new StreamingFrame.GeoLocation(
                loc.has("latitude") ? loc.get("latitude").getAsDouble() : 0,
                loc.has("longitude") ? loc.get("longitude").getAsDouble() : 0,
                getStringOrNull(loc, "locationName"),
                getStringOrNull(loc, "parkName")
            );
        }
        
        StreamingFrame.FrameMetadata metadata = null;
        if (obj.has("metadata") && !obj.get("metadata").isJsonNull()) {
            JsonObject meta = obj.getAsJsonObject("metadata");
            metadata = new StreamingFrame.FrameMetadata(
                meta.has("frameNumber") ? meta.get("frameNumber").getAsInt() : 0,
                meta.has("width") ? meta.get("width").getAsInt() : 0,
                meta.has("height") ? meta.get("height").getAsInt() : 0,
                getStringOrNull(meta, "deviceId"),
                getStringOrNull(meta, "cameraType")
            );
        }
        
        return new StreamingFrame(sessionId, imageData, mimeType, capturedAt, location, metadata);
    }
    
    private static String getStringOrNull(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() 
            ? obj.get(key).getAsString() 
            : null;
    }
    
    private static String getStringOrDefault(JsonObject obj, String key, String defaultVal) {
        String val = getStringOrNull(obj, key);
        return val != null ? val : defaultVal;
    }
    
    /**
     * Adapter for Instant serialization.
     */
    private static class InstantAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
        @Override
        public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
        
        @Override
        public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            return Instant.parse(json.getAsString());
        }
    }
    
    /**
     * Adapter for byte array serialization (Base64).
     */
    private static class ByteArrayAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
        @Override
        public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(Base64.getEncoder().encodeToString(src));
        }
        
        @Override
        public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            return Base64.getDecoder().decode(json.getAsString());
        }
    }
}
