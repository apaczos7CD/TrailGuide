package pl.trailguide.backend.trip;

import java.math.BigDecimal;
import java.time.Instant;

public record LocationPointResponse(
		Long id,
		BigDecimal latitude,
		BigDecimal longitude,
		BigDecimal altitude,
		BigDecimal accuracy,
		Instant timestamp) {

	static LocationPointResponse from(LocationPoint point) {
		return new LocationPointResponse(
				point.getId(),
				point.getLatitude(),
				point.getLongitude(),
				point.getAltitude(),
				point.getAccuracy(),
				point.getTimestamp());
	}
}
