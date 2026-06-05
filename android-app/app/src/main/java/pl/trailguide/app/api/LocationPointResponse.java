package pl.trailguide.app.api;

import java.math.BigDecimal;

public class LocationPointResponse {
    private Long id;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal altitude;
    private BigDecimal accuracy;
    private String timestamp;

    public Long getId() {
        return id;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public BigDecimal getAltitude() {
        return altitude;
    }

    public BigDecimal getAccuracy() {
        return accuracy;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
