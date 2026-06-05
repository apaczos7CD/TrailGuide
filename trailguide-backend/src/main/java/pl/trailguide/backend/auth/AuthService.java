package pl.trailguide.backend.auth;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pl.trailguide.backend.security.JwtService;
import pl.trailguide.backend.user.AppUser;
import pl.trailguide.backend.user.UserRepository;
import pl.trailguide.backend.user.UserRole;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new IllegalArgumentException("Email is already used");
		}
		if (userRepository.existsByUsername(request.username())) {
			throw new IllegalArgumentException("Username is already used");
		}

		AppUser user = new AppUser(
				request.username(),
				request.email(),
				passwordEncoder.encode(request.password()),
				UserRole.USER);

		AppUser savedUser = userRepository.save(user);
		return new AuthResponse(jwtService.createToken(savedUser), savedUser.getRole().name());
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		AppUser user = userRepository.findByEmail(request.email())
				.orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new BadCredentialsException("Invalid email or password");
		}

		return new AuthResponse(jwtService.createToken(user), user.getRole().name());
	}
}
