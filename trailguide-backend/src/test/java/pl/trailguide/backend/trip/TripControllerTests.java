package pl.trailguide.backend.trip;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TripControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void rejectsTripsWithoutJwt() throws Exception {
		mockMvc.perform(get("/api/trips"))
				.andExpect(status().isForbidden());
	}

	@Test
	void startsTripForCurrentUser() throws Exception {
		String token = registerAndGetToken("turysta123", "turysta@example.com");

		mockMvc.perform(post("/api/trips/start")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "title": "Tatry morning walk",
						  "description": "Easy route near the valley"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.title").value("Tatry morning walk"))
				.andExpect(jsonPath("$.endTime").doesNotExist())
				.andExpect(jsonPath("$.locationPointCount").value(0));
	}

	@Test
	void addsLocationPointAndReturnsTripDetails() throws Exception {
		String token = registerAndGetToken("adam", "adam@example.com");
		Long tripId = startTripAndGetId(token);

		mockMvc.perform(post("/api/trips/{id}/location-points", tripId)
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "latitude": 49.2992,
						  "longitude": 19.9496,
						  "altitude": 960.0,
						  "accuracy": 6.5,
						  "timestamp": "2026-05-24T12:30:00Z"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.latitude").value(49.2992))
				.andExpect(jsonPath("$.longitude").value(19.9496));

		mockMvc.perform(get("/api/trips/{id}", tripId)
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.locationPointCount").value(1))
				.andExpect(jsonPath("$.locationPoints", hasSize(1)))
				.andExpect(jsonPath("$.locationPoints[0].accuracy").value(6.5));
	}

	@Test
	void finishesTripAndListsIt() throws Exception {
		String token = registerAndGetToken("ewa", "ewa@example.com");
		Long tripId = startTripAndGetId(token);
		addLocationPoint(token, tripId, 49.2992, 19.9496, "2026-05-24T12:30:00Z");
		addLocationPoint(token, tripId, 49.3000, 19.9500, "2026-05-24T12:35:00Z");

		mockMvc.perform(post("/api/trips/{id}/finish", tripId)
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.endTime").exists())
				.andExpect(jsonPath("$.distanceMeters").value(93.56))
				.andExpect(jsonPath("$.locationPointCount").value(2));

		mockMvc.perform(get("/api/trips")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(tripId));
	}

	@Test
	void rejectsLocationPointAfterTripIsFinished() throws Exception {
		String token = registerAndGetToken("jan", "jan@example.com");
		Long tripId = startTripAndGetId(token);

		mockMvc.perform(post("/api/trips/{id}/finish", tripId)
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/trips/{id}/location-points", tripId)
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "latitude": 49.2992,
						  "longitude": 19.9496,
						  "timestamp": "2026-05-24T12:30:00Z"
						}
						"""))
				.andExpect(status().isConflict());
	}

	@Test
	void doesNotReturnAnotherUsersTripDetails() throws Exception {
		String ownerToken = registerAndGetToken("owner", "owner@example.com");
		String otherToken = registerAndGetToken("other", "other@example.com");
		Long ownerTripId = startTripAndGetId(ownerToken);

		mockMvc.perform(get("/api/trips/{id}", ownerTripId)
				.header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isNotFound());
	}

	@Test
	void doesNotListAnotherUsersTrips() throws Exception {
		String ownerToken = registerAndGetToken("anna", "anna@example.com");
		String otherToken = registerAndGetToken("marek", "marek@example.com");
		startTripAndGetId(ownerToken);

		mockMvc.perform(get("/api/trips")
				.header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	private Long startTripAndGetId(String token) throws Exception {
		String response = mockMvc.perform(post("/api/trips/start")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "title": "Test trip"
						}
						"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();

		return Long.parseLong(extractJsonValue(response, "id"));
	}

	private void addLocationPoint(String token, Long tripId, double latitude, double longitude, String timestamp) throws Exception {
		mockMvc.perform(post("/api/trips/{id}/location-points", tripId)
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(String.format("""
						{
						  "latitude": %s,
						  "longitude": %s,
						  "timestamp": "%s"
						}
						""", latitude, longitude, timestamp)))
				.andExpect(status().isCreated());
	}

	private String registerAndGetToken(String username, String email) throws Exception {
		String response = mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(String.format("""
						{
						  "username": "%s",
						  "email": "%s",
						  "password": "StrongPassword123!"
						}
						""", username, email)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.token", not(blankOrNullString())))
				.andReturn()
				.getResponse()
				.getContentAsString();

		return extractJsonValue(response, "token");
	}

	private String extractJsonValue(String response, String field) {
		String marker = "\"" + field + "\":";
		int start = response.indexOf(marker);
		if (start < 0) {
			throw new IllegalStateException("Missing field " + field + " in response: " + response);
		}
		start += marker.length();
		if (response.charAt(start) == '"') {
			start++;
			int end = response.indexOf('"', start);
			return response.substring(start, end);
		}
		int end = response.indexOf(',', start);
		if (end < 0) {
			end = response.indexOf('}', start);
		}
		return response.substring(start, end);
	}
}
