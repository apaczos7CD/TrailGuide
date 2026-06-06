package pl.trailguide.backend.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import pl.trailguide.backend.user.AppUser;
import pl.trailguide.backend.user.UserRepository;
import pl.trailguide.backend.user.UserRole;

@Component
public class DefaultAdminSeeder implements CommandLineRunner {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final String username;
	private final String email;
	private final String password;

	public DefaultAdminSeeder(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			@Value("${trailguide.admin.username}") String username,
			@Value("${trailguide.admin.email}") String email,
			@Value("${trailguide.admin.password}") String password) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.username = username;
		this.email = email;
		this.password = password;
	}

	@Override
	@Transactional
	public void run(String... args) {
		String passwordHash = passwordEncoder.encode(password);
		userRepository.findByEmail(email).ifPresentOrElse(existingAdmin -> {
			existingAdmin.updateCredentials(passwordHash, UserRole.ADMIN);
			userRepository.save(existingAdmin);
		}, () -> {
			AppUser admin = new AppUser(
					username,
					email,
					passwordHash,
					UserRole.ADMIN);
			userRepository.save(admin);
		});
	}
}
