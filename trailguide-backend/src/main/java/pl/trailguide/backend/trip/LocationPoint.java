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

@Entity
@Table(name = "location_points")
public class LocationPoint {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "trip_id", nullable = false)
	private Trip trip;

	@Column(nullable = false, precision = 9, scale = 6)
	private BigDecimal latitude;

	@Column(nullable = false, precision = 9, scale = 6)
	private BigDecimal longitude;

	@Column(precision = 8, scale = 2)
	private BigDecimal altitude;

	@Column(precision = 8, scale = 2)
	private BigDecimal accuracy;

	@Column(nullable = false)
	private Instant timestamp;

	protected LocationPoint() {
	}

	public LocationPoint(
			Trip trip,
			BigDecimal latitude,
			BigDecimal longitude,
			BigDecimal altitude,
			BigDecimal accuracy,
			Instant timestamp) {
		this.trip = trip;
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
		this.accuracy = accuracy;
		this.timestamp = timestamp;
	}

	public Long getId() {
		return id;
	}

	public Trip getTrip() {
		return trip;
	}

	public BigDecimal getLatitude() {
		return latitude;
	}

	public BigDecimal getLongitude() {
		return longitude;
	}

	public BigDecimal getAltitude() {
		return altitude;
	}

	public BigDecimal getAccuracy() {
		return accuracy;
	}

	public Instant getTimestamp() {
		return timestamp;
	}
}
