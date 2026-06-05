package pl.trailguide.backend.user;

public record UserMeResponse(
		Long id,
		String username,
		String email,
		String role,
		UserProfileResponse profile) {

	static UserMeResponse from(AppUser user, UserProfile profile) {
		return new UserMeResponse(
				user.getId(),
				user.getUsername(),
				user.getEmail(),
				user.getRole().name(),
				profile == null ? null : UserProfileResponse.from(profile));
	}
}
