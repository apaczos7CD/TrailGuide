package pl.trailguide.backend.admin;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pl.trailguide.backend.user.UserRepository;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

	private final UserRepository userRepository;

	public AdminUserController(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@GetMapping
	@Transactional(readOnly = true)
	public List<AdminUserResponse> users() {
		return userRepository.findAll().stream()
				.map(AdminUserResponse::from)
				.toList();
	}
}
