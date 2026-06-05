package pl.trailguide.app.api;

import java.math.BigDecimal;

public class AddLocationPointRequest {
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final BigDecimal altitude;
    private final BigDecimal accuracy;
    private final String timestamp;

    public AddLocationPointRequest(
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal altitude,
            BigDecimal accuracy,
            String timestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.accuracy = accuracy;
        this.timestamp = timestamp;
    }
}
