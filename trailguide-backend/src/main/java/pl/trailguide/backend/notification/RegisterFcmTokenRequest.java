package pl.trailguide.backend.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterFcmTokenRequest(
		@NotBlank @Size(max = 512) String token) {
}
