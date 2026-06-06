package pl.trailguide.backend.notification;

import java.time.Instant;

public record NotificationResponse(
		Long id,
		String title,
		String body,
		Instant sentAt,
		int recipientCount) {

	static NotificationResponse from(PushNotification notification) {
		return new NotificationResponse(
				notification.getId(),
				notification.getTitle(),
				notification.getBody(),
				notification.getSentAt(),
				notification.getRecipientCount());
	}
}
