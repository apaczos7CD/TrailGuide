package pl.trailguide.backend.admin;

import java.time.Instant;

import pl.trailguide.backend.user.AppUser;

public record AdminUserResponse(
		Long id,
		String username,
		String email,
		String role,
		Instant createdAt) {

	static AdminUserResponse from(AppUser user) {
		return new AdminUserResponse(
				user.getId(),
				user.getUsername(),
				user.getEmail(),
				user.getRole().name(),
				user.getCreatedAt());
	}
}
