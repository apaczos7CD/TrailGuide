package pl.trailguide.backend.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendNotificationRequest(
		@NotBlank @Size(max = 120) String title,
		@NotBlank @Size(max = 500) String body) {
}
