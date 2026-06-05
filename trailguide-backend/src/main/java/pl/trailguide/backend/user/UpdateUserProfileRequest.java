package pl.trailguide.backend.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
		@Size(max = 80) String firstName,
		@Min(50) @Max(250) Integer height,
		@Min(20) @Max(300) Integer weight,
		HikingLevel hikingLevel) {
}
