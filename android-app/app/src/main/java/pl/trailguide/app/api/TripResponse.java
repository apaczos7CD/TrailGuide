package pl.trailguide.app.api;

import java.math.BigDecimal;
import java.util.List;

public class TripResponse {
    private Long id;
    private String title;
    private String startTime;
    private String endTime;
    private BigDecimal distanceMeters;
    private String description;
    private long locationPointCount;
    private List<LocationPointResponse> locationPoints;

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public BigDecimal getDistanceMeters() {
        return distanceMeters;
    }

    public String getDescription() {
        return description;
    }

    public long getLocationPointCount() {
        return locationPointCount;
    }

    public List<LocationPointResponse> getLocationPoints() {
        return locationPoints;
    }

    public boolean isFinished() {
        return endTime != null;
    }
}
