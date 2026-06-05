package pl.trailguide.backend.user;

public record UserProfileResponse(
		String firstName,
		Integer height,
		Integer weight,
		String hikingLevel) {

	static UserProfileResponse from(UserProfile profile) {
		String level = profile.getHikingLevel() == null ? null : profile.getHikingLevel().name();
		return new UserProfileResponse(
				profile.getFirstName(),
				profile.getHeight(),
				profile.getWeight(),
				level);
	}
}
