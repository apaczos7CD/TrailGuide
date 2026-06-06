package pl.trailguide.app.api;

public class UpdateUserProfileRequest {
    private final String firstName;
    private final String city;
    private final Integer height;
    private final Integer weight;
    private final String hikingLevel;

    public UpdateUserProfileRequest(String firstName, String city, Integer height, Integer weight, String hikingLevel) {
        this.firstName = firstName;
        this.city = city;
        this.height = height;
        this.weight = weight;
        this.hikingLevel = hikingLevel;
    }
}
