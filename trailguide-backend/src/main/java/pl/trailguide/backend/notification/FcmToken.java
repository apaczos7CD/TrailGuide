package pl.trailguide.backend.notification;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import pl.trailguide.backend.user.AppUser;

@Entity
@Table(name = "fcm_tokens")
public class FcmToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private AppUser user;

	@Column(nullable = false, unique = true, length = 512)
	private String token;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	protected FcmToken() {
	}

	public FcmToken(AppUser user, String token) {
		this.user = user;
		this.token = token;
	}

	public Long getId() {
		return id;
	}

	public AppUser getUser() {
		return user;
	}

	public String getToken() {
		return token;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void assignTo(AppUser user) {
		this.user = user;
	}
}
