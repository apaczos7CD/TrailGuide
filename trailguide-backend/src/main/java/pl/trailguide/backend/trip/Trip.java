package pl.trailguide.backend.trip;

import java.math.BigDecimal;
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
@Table(name = "trips")
public class Trip {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private AppUser user;

	@Column(nullable = false, length = 120)
	private String title;

	@Column(name = "start_time", nullable = false)
	private Instant startTime;

	@Column(name = "end_time")
	private Instant endTime;

	@Column(name = "distance_meters", precision = 12, scale = 2)
	private BigDecimal distanceMeters;

	@Column(length = 500)
	private String description;

	protected Trip() {
	}

	public Trip(AppUser user, String title, Instant startTime, String description) {
		this.user = user;
		this.title = title;
		this.startTime = startTime;
		this.description = description;
	}

	public Long getId() {
		return id;
	}

	public AppUser getUser() {
		return user;
	}

	public String getTitle() {
		return title;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public Instant getEndTime() {
		return endTime;
	}

	public BigDecimal getDistanceMeters() {
		return distanceMeters;
	}

	public String getDescription() {
		return description;
	}

	public boolean isFinished() {
		return endTime != null;
	}

	public void finish(Instant endTime, BigDecimal distanceMeters) {
		this.endTime = endTime;
		this.distanceMeters = distanceMeters;
	}
}
