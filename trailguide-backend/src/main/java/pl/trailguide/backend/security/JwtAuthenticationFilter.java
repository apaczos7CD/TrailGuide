package pl.trailguide.backend.security;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import pl.trailguide.backend.user.AppUser;
import pl.trailguide.backend.user.UserRepository;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserRepository userRepository;

	public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
		this.jwtService = jwtService;
		this.userRepository = userRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String authHeader = request.getHeader("Authorization");

		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			String token = authHeader.substring(7);
			if (jwtService.isValid(token)) {
				String email = jwtService.extractEmail(token);
				userRepository.findByEmail(email).ifPresent(this::authenticate);
			}
		}

		filterChain.doFilter(request, response);
	}

	private void authenticate(AppUser user) {
		SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				user.getEmail(),
				null,
				List.of(authority));
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}
}
