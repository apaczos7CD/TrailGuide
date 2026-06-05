package pl.trailguide.backend.trip;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationPointRepository extends JpaRepository<LocationPoint, Long> {
	List<LocationPoint> findByTripOrderByTimestampAsc(Trip trip);

	long countByTrip(Trip trip);
}
