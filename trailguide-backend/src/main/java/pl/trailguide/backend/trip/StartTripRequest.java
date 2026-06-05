package pl.trailguide.backend.trip;

import jakarta.validation.constraints.Size;

public record StartTripRequest(
		@Size(max = 120) String title,
		@Size(max = 500) String description) {
}
