package pl.trailguide.backend.user;

import java.security.Principal;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserRepository userRepository;
	private final UserProfileRepository userProfileRepository;

	public UserController(UserRepository userRepository, UserProfileRepository userProfileRepository) {
		this.userRepository = userRepository;
		this.userProfileRepository = userProfileRepository;
	}

	@GetMapping("/me")
	@Transactional(readOnly = true)
	public UserMeResponse me(Principal principal) {
		AppUser user = currentUser(principal);
		UserProfile profile = userProfileRepository.findByUser(user).orElse(null);
		return UserMeResponse.from(user, profile);
	}

	@PutMapping("/me")
	@Transactional
	public UserMeResponse updateMe(Principal principal, @Valid @RequestBody UpdateUserProfileRequest request) {
		AppUser user = currentUser(principal);
		UserProfile profile = userProfileRepository.findByUser(user)
				.orElseGet(() -> new UserProfile(user));
		profile.update(request.firstName(), request.height(), request.weight(), request.hikingLevel());
		UserProfile savedProfile = userProfileRepository.save(profile);
		return UserMeResponse.from(user, savedProfile);
	}

	private AppUser currentUser(Principal principal) {
		return userRepository.findByEmail(principal.getName())
				.orElseThrow(() -> new IllegalArgumentException("User not found"));
	}
}
