package pl.trailguide.backend.notification;

import java.security.Principal;

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
@RequestMapping("/api/fcm-token")
public class FcmTokenController {

	private final UserRepository userRepository;
	private final FcmTokenRepository fcmTokenRepository;

	public FcmTokenController(UserRepository userRepository, FcmTokenRepository fcmTokenRepository) {
		this.userRepository = userRepository;
		this.fcmTokenRepository = fcmTokenRepository;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	public FcmTokenResponse registerToken(Principal principal, @Valid @RequestBody RegisterFcmTokenRequest request) {
		AppUser user = currentUser(principal);
		FcmToken token = fcmTokenRepository.findByToken(request.token())
				.map(existingToken -> {
					existingToken.assignTo(user);
					return existingToken;
				})
				.orElseGet(() -> new FcmToken(user, request.token()));

		return FcmTokenResponse.from(fcmTokenRepository.save(token));
	}

	private AppUser currentUser(Principal principal) {
		return userRepository.findByEmail(principal.getName())
				.orElseThrow(() -> new IllegalArgumentException("User not found"));
	}
}
