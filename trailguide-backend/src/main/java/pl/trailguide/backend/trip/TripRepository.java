package pl.trailguide.backend.trip;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pl.trailguide.backend.user.AppUser;

public interface TripRepository extends JpaRepository<Trip, Long> {
	List<Trip> findByUserOrderByStartTimeDesc(AppUser user);

	Optional<Trip> findByIdAndUser(Long id, AppUser user);
}
