package pl.trailguide.app.api;

public class FcmTokenResponse {
    private Long id;
    private String token;
    private String createdAt;

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
