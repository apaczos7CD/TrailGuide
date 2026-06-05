package pl.trailguide.backend.trip;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TripResponse(
		Long id,
		String title,
		Instant startTime,
		Instant endTime,
		BigDecimal distanceMeters,
		String description,
		long locationPointCount,
		List<LocationPointResponse> locationPoints) {

	static TripResponse summary(Trip trip, long locationPointCount) {
		return new TripResponse(
				trip.getId(),
				trip.getTitle(),
				trip.getStartTime(),
				trip.getEndTime(),
				trip.getDistanceMeters(),
				trip.getDescription(),
				locationPointCount,
				List.of());
	}

	static TripResponse details(Trip trip, List<LocationPoint> locationPoints) {
		return new TripResponse(
				trip.getId(),
				trip.getTitle(),
				trip.getStartTime(),
				trip.getEndTime(),
				trip.getDistanceMeters(),
				trip.getDescription(),
				locationPoints.size(),
				locationPoints.stream().map(LocationPointResponse::from).toList());
	}
}
