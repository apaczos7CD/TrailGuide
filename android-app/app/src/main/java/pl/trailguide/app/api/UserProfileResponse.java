package pl.trailguide.app.api;

public class UserProfileResponse {
    private String firstName;
    private String city;
    private Integer height;
    private Integer weight;
    private String hikingLevel;

    public String getFirstName() {
        return firstName;
    }

    public String getCity() {
        return city;
    }

    public Integer getHeight() {
        return height;
    }

    public Integer getWeight() {
        return weight;
    }

    public String getHikingLevel() {
        return hikingLevel;
    }
}
