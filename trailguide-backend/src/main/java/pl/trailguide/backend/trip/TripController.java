package pl.trailguide.backend.trip;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pl.trailguide.backend.common.ResourceNotFoundException;
import pl.trailguide.backend.user.AppUser;
import pl.trailguide.backend.user.UserRepository;

@RestController
@RequestMapping("/api/trips")
public class TripController {

	private final UserRepository userRepository;
	private final TripRepository tripRepository;
	private final LocationPointRepository locationPointRepository;
	private final DistanceCalculator distanceCalculator;

	public TripController(
			UserRepository userRepository,
			TripRepository tripRepository,
			LocationPointRepository locationPointRepository,
			DistanceCalculator distanceCalculator) {
		this.userRepository = userRepository;
		this.tripRepository = tripRepository;
		this.locationPointRepository = locationPointRepository;
		this.distanceCalculator = distanceCalculator;
	}

	@PostMapping("/start")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	public TripResponse startTrip(Principal principal, @Valid @RequestBody StartTripRequest request) {
		AppUser user = currentUser(principal);
		String title = request.title() == null || request.title().isBlank() ? "Untitled trip" : request.title();
		Trip trip = tripRepository.save(new Trip(user, title, Instant.now(), request.description()));
		return TripResponse.summary(trip, 0);
	}

	@PostMapping("/{id}/location-points")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	public LocationPointResponse addLocationPoint(
			Principal principal,
			@PathVariable Long id,
			@Valid @RequestBody AddLocationPointRequest request) {
		Trip trip = currentUserTrip(principal, id);
		if (trip.isFinished()) {
			throw new IllegalArgumentException("Cannot add location points to a finished trip");
		}

		LocationPoint point = new LocationPoint(
				trip,
				request.latitude(),
				request.longitude(),
				request.altitude(),
				request.accuracy(),
				request.timestamp());

		return LocationPointResponse.from(locationPointRepository.save(point));
	}

	@PostMapping("/{id}/finish")
	@Transactional
	public TripResponse finishTrip(
			Principal principal,
			@PathVariable Long id,
			@Valid @RequestBody FinishTripRequest request) {
		Trip trip = currentUserTrip(principal, id);
		if (trip.isFinished()) {
			throw new IllegalArgumentException("Trip is already finished");
		}
		List<LocationPoint> points = locationPointRepository.findByTripOrderByTimestampAsc(trip);
		BigDecimal distance = distanceCalculator.calculateMeters(points);
		trip.finish(Instant.now(), distance);
		Trip savedTrip = tripRepository.save(trip);
		return TripResponse.summary(savedTrip, points.size());
	}

	@GetMapping
	@Transactional(readOnly = true)
	public List<TripResponse> trips(Principal principal) {
		AppUser user = currentUser(principal);
		return tripRepository.findByUserOrderByStartTimeDesc(user).stream()
				.map(trip -> TripResponse.summary(trip, locationPointRepository.countByTrip(trip)))
				.toList();
	}

	@GetMapping("/{id}")
	@Transactional(readOnly = true)
	public TripResponse tripDetails(Principal principal, @PathVariable Long id) {
		Trip trip = currentUserTrip(principal, id);
		List<LocationPoint> points = locationPointRepository.findByTripOrderByTimestampAsc(trip);
		return TripResponse.details(trip, points);
	}

	private Trip currentUserTrip(Principal principal, Long tripId) {
		AppUser user = currentUser(principal);
		return tripRepository.findByIdAndUser(tripId, user)
				.orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
	}

	private AppUser currentUser(Principal principal) {
		return userRepository.findByEmail(principal.getName())
				.orElseThrow(() -> new IllegalArgumentException("User not found"));
	}
}
