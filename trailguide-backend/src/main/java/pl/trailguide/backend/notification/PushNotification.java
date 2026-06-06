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
@Table(name = "notifications")
public class PushNotification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by_user_id", nullable = false)
	private AppUser createdBy;

	@Column(nullable = false, length = 120)
	private String title;

	@Column(nullable = false, length = 500)
	private String body;

	@Column(name = "sent_at", nullable = false)
	private Instant sentAt = Instant.now();

	@Column(name = "recipient_count", nullable = false)
	private int recipientCount;

	protected PushNotification() {
	}

	public PushNotification(AppUser createdBy, String title, String body, int recipientCount) {
		this.createdBy = createdBy;
		this.title = title;
		this.body = body;
		this.recipientCount = recipientCount;
	}

	public Long getId() {
		return id;
	}

	public AppUser getCreatedBy() {
		return createdBy;
	}

	public String getTitle() {
		return title;
	}

	public String getBody() {
		return body;
	}

	public Instant getSentAt() {
		return sentAt;
	}

	public int getRecipientCount() {
		return recipientCount;
	}
}
