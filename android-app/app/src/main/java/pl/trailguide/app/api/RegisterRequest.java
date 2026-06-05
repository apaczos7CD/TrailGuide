package pl.trailguide.app.api;

public class RegisterRequest {
    private final String username;
    private final String email;
    private final String password;

    public RegisterRequest(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
}
