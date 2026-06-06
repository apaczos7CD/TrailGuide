package pl.trailguide.backend.admin;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.hasItem;
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
class AdminUserControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void defaultAdminCanListUsers() throws Exception {
		registerUser();
		String adminToken = loginAndGetToken("admin@example.com", "AdminPassword123!");

		mockMvc.perform(get("/api/admin/users")
				.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].email", hasItem("admin@example.com")))
				.andExpect(jsonPath("$[*].email", hasItem("turysta@example.com")))
				.andExpect(jsonPath("$[*].role", hasItem("ADMIN")))
				.andExpect(jsonPath("$[*].passwordHash").doesNotExist());
	}

	@Test
	void normalUserCannotListUsers() throws Exception {
		registerUser();
		String userToken = loginAndGetToken("turysta@example.com", "StrongPassword123!");

		mockMvc.perform(get("/api/admin/users")
				.header("Authorization", "Bearer " + userToken))
				.andExpect(status().isForbidden());
	}

	private void registerUser() throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "username": "turysta123",
						  "email": "turysta@example.com",
						  "password": "StrongPassword123!"
						}
						"""))
				.andExpect(status().isCreated());
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
