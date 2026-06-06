package pl.trailguide.backend.notification;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
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
class NotificationControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void userRegistersFcmTokenAfterLogin() throws Exception {
		String token = registerAndGetToken("turysta123", "turysta@example.com");

		mockMvc.perform(post("/api/fcm-token")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "token": "test-fcm-token-1"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.token").value("test-fcm-token-1"));
	}

	@Test
	void adminSendsNotificationToRegisteredTokens() throws Exception {
		String userToken = registerAndGetToken("adam", "adam@example.com");
		registerFcmToken(userToken, "test-fcm-token-2");
		String adminToken = loginAndGetToken("admin@example.com", "AdminPassword123!");

		mockMvc.perform(post("/api/notifications")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "title": "Uwaga na trasie",
						  "body": "Szlak jest dzisiaj czesciowo zamkniety."
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.title").value("Uwaga na trasie"))
				.andExpect(jsonPath("$.recipientCount").value(1));
	}

	@Test
	void normalUserCannotSendNotification() throws Exception {
		String userToken = registerAndGetToken("ewa", "ewa@example.com");

		mockMvc.perform(post("/api/notifications")
				.header("Authorization", "Bearer " + userToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "title": "Uwaga",
						  "body": "Test"
						}
						"""))
				.andExpect(status().isForbidden());
	}

	private void registerFcmToken(String token, String fcmToken) throws Exception {
		mockMvc.perform(post("/api/fcm-token")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(String.format("""
						{
						  "token": "%s"
						}
						""", fcmToken)))
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

	private String loginAndGetToken(String email, String password) throws Exception {
		String response = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(String.format("""
						{
						  "email": "%s",
						  "password": "%s"
						}
						""", email, password)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token", not(blankOrNullString())))
				.andReturn()
				.getResponse()
				.getContentAsString();

		return extractJsonValue(response, "token");
	}

	private String extractJsonValue(String response, String field) {
		String marker = "\"" + field + "\":\"";
		int start = response.indexOf(marker);
		if (start < 0) {
			throw new IllegalStateException("Missing field " + field + " in response: " + response);
		}
		start += marker.length();
		int end = response.indexOf('"', start);
		return response.substring(start, end);
	}
}
