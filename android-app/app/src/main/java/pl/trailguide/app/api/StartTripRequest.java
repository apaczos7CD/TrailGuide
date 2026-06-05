package pl.trailguide.app.api;

public class StartTripRequest {
    private final String title;
    private final String description;

    public StartTripRequest(String title, String description) {
        this.title = title;
        this.description = description;
    }
}
