package pl.trailguide.app.api;

public class UserMeResponse {
    private Long id;
    private String username;
    private String email;
    private String role;
    private UserProfileResponse profile;

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public UserProfileResponse getProfile() {
        return profile;
    }
}
