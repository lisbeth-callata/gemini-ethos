package com.gemini.ethos.model;

import java.time.Instant;

/**
 * Represents a video/image frame received from the streaming endpoint.
 */
public record StreamingFrame(
    String sessionId,
    byte[] imageData,
    String mimeType,
    Instant capturedAt,
    GeoLocation location,
    FrameMetadata metadata
) {
    
    public record GeoLocation(
        double latitude,
        double longitude,
        String locationName,
        String parkName
    ) {}
    
    public record FrameMetadata(
        int frameNumber,
        int width,
        int height,
        String deviceId,
        String cameraType
    ) {}
    
    /**
     * Validates that the frame contains required data.
     */
    public boolean isValid() {
        return sessionId != null && !sessionId.isBlank()
            && imageData != null && imageData.length > 0
            && mimeType != null && !mimeType.isBlank();
    }
    
    /**
     * Returns the size of the image data in bytes.
     */
    public int getDataSize() {
        return imageData != null ? imageData.length : 0;
    }
}
