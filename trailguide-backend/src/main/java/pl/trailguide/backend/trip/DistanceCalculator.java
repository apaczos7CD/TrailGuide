package pl.trailguide.backend.trip;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class DistanceCalculator {

	private static final double EARTH_RADIUS_METERS = 6_371_000.0;

	public BigDecimal calculateMeters(List<LocationPoint> points) {
		if (points.size() < 2) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}

		double totalMeters = 0.0;
		for (int i = 1; i < points.size(); i++) {
			LocationPoint previous = points.get(i - 1);
			LocationPoint current = points.get(i);
			totalMeters += distanceBetween(previous, current);
		}

		return BigDecimal.valueOf(totalMeters).setScale(2, RoundingMode.HALF_UP);
	}

	private double distanceBetween(LocationPoint previous, LocationPoint current) {
		double previousLatitude = Math.toRadians(previous.getLatitude().doubleValue());
		double currentLatitude = Math.toRadians(current.getLatitude().doubleValue());
		double latitudeDelta = Math.toRadians(current.getLatitude().doubleValue() - previous.getLatitude().doubleValue());
		double longitudeDelta = Math.toRadians(current.getLongitude().doubleValue() - previous.getLongitude().doubleValue());

		double a = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
				+ Math.cos(previousLatitude) * Math.cos(currentLatitude)
						* Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return EARTH_RADIUS_METERS * c;
	}
}
