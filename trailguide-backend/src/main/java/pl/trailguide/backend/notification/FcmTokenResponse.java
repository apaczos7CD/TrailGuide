package pl.trailguide.backend.notification;

import java.time.Instant;

public record FcmTokenResponse(
		Long id,
		String token,
		Instant createdAt) {

	static FcmTokenResponse from(FcmToken token) {
		return new FcmTokenResponse(
				token.getId(),
				token.getToken(),
				token.getCreatedAt());
	}
}
