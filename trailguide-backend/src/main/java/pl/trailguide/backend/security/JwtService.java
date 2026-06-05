package pl.trailguide.backend.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import pl.trailguide.backend.user.AppUser;

@Service
public class JwtService {

	private final String secret;
	private final long expirationMinutes;

	public JwtService(
			@Value("${trailguide.jwt.secret}") String secret,
			@Value("${trailguide.jwt.expiration-minutes}") long expirationMinutes) {
		this.secret = secret;
		this.expirationMinutes = expirationMinutes;
	}

	public String createToken(AppUser user) {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);
		String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
		String payload = String.format(
				"{\"sub\":\"%s\",\"role\":\"%s\",\"iat\":%d,\"exp\":%d}",
				jsonEscape(user.getEmail()),
				user.getRole().name(),
				now.getEpochSecond(),
				expiresAt.getEpochSecond());

		String unsignedToken = base64Url(header.getBytes(StandardCharsets.UTF_8)) + "."
				+ base64Url(payload.getBytes(StandardCharsets.UTF_8));
		return unsignedToken + "." + sign(unsignedToken);
	}

	public String extractEmail(String token) {
		validate(token);
		String payload = decodePayload(token);
		return extractStringClaim(payload, "sub");
	}

	public boolean isValid(String token) {
		try {
			validate(token);
			return true;
		}
		catch (RuntimeException ex) {
			return false;
		}
	}

	private void validate(String token) {
		String[] parts = token.split("\\.");
		if (parts.length != 3) {
			throw new IllegalArgumentException("Invalid JWT format");
		}

		String unsignedToken = parts[0] + "." + parts[1];
		if (!constantTimeEquals(sign(unsignedToken), parts[2])) {
			throw new IllegalArgumentException("Invalid JWT signature");
		}

		long expiresAt = extractLongClaim(decodePayload(token), "exp");
		if (Instant.now().getEpochSecond() >= expiresAt) {
			throw new IllegalArgumentException("JWT expired");
		}
	}

	private String decodePayload(String token) {
		String[] parts = token.split("\\.");
		return new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
	}

	private String sign(String value) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return base64Url(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Cannot sign JWT", ex);
		}
	}

	private String base64Url(byte[] bytes) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String extractStringClaim(String payload, String claim) {
		String marker = "\"" + claim + "\":\"";
		int start = payload.indexOf(marker);
		if (start < 0) {
			throw new IllegalArgumentException("Missing JWT claim: " + claim);
		}
		start += marker.length();
		int end = payload.indexOf('"', start);
		return payload.substring(start, end);
	}

	private long extractLongClaim(String payload, String claim) {
		String marker = "\"" + claim + "\":";
		int start = payload.indexOf(marker);
		if (start < 0) {
			throw new IllegalArgumentException("Missing JWT claim: " + claim);
		}
		start += marker.length();
		int end = payload.indexOf(',', start);
		if (end < 0) {
			end = payload.indexOf('}', start);
		}
		return Long.parseLong(payload.substring(start, end));
	}

	private boolean constantTimeEquals(String left, String right) {
		return left.length() == right.length() && MessageDigestUtils.constantTimeEquals(left, right);
	}

	private String jsonEscape(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
