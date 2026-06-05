package pl.trailguide.backend.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private AppUser user;

	@Column(name = "first_name", length = 80)
	private String firstName;

	private Integer height;

	private Integer weight;

	@Enumerated(EnumType.STRING)
	@Column(name = "hiking_level", length = 20)
	private HikingLevel hikingLevel;

	protected UserProfile() {
	}

	public UserProfile(AppUser user) {
		this.user = user;
	}

	public Long getId() {
		return id;
	}

	public AppUser getUser() {
		return user;
	}

	public String getFirstName() {
		return firstName;
	}

	public Integer getHeight() {
		return height;
	}

	public Integer getWeight() {
		return weight;
	}

	public HikingLevel getHikingLevel() {
		return hikingLevel;
	}

	public void update(String firstName, Integer height, Integer weight, HikingLevel hikingLevel) {
		this.firstName = firstName;
		this.height = height;
		this.weight = weight;
		this.hikingLevel = hikingLevel;
	}
}
