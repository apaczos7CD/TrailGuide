package pl.trailguide.backend.notification;

import java.security.Principal;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pl.trailguide.backend.user.AppUser;
import pl.trailguide.backend.user.UserRepository;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

	private final UserRepository userRepository;
	private final FcmTokenRepository fcmTokenRepository;
	private final PushNotificationRepository pushNotificationRepository;
	private final FirebasePushSender firebasePushSender;

	public NotificationController(
			UserRepository userRepository,
			FcmTokenRepository fcmTokenRepository,
			PushNotificationRepository pushNotificationRepository,
			FirebasePushSender firebasePushSender) {
		this.userRepository = userRepository;
		this.fcmTokenRepository = fcmTokenRepository;
		this.pushNotificationRepository = pushNotificationRepository;
		this.firebasePushSender = firebasePushSender;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	public NotificationResponse sendNotification(
			Principal principal,
			@Valid @RequestBody SendNotificationRequest request) {
		AppUser admin = currentUser(principal);
		List<String> tokens = fcmTokenRepository.findAll().stream()
				.map(FcmToken::getToken)
				.toList();

		firebasePushSender.sendToTokens(request.title(), request.body(), tokens);

		PushNotification notification = pushNotificationRepository.save(new PushNotification(
				admin,
				request.title(),
				request.body(),
				tokens.size()));
		return NotificationResponse.from(notification);
	}

	private AppUser currentUser(Principal principal) {
		return userRepository.findByEmail(principal.getName())
				.orElseThrow(() -> new IllegalArgumentException("User not found"));
	}
}
