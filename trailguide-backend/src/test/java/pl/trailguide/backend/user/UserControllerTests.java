package pl.trailguide.backend.user;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class UserControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void rejectsMeWithoutJwt() throws Exception {
		mockMvc.perform(get("/api/users/me"))
				.andExpect(status().isForbidden());
	}

	@Test
	void returnsCurrentUserWithValidJwt() throws Exception {
		String token = registerAndGetToken();

		mockMvc.perform(get("/api/users/me")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.username").value("turysta123"))
				.andExpect(jsonPath("$.email").value("turysta@example.com"))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.profile").doesNotExist());
	}

	@Test
	void updatesCurrentUserProfileWithValidJwt() throws Exception {
		String token = registerAndGetToken();

		mockMvc.perform(put("/api/users/me")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "firstName": "Adam",
						  "height": 170,
						  "weight": 75,
						  "hikingLevel": "BEGINNER"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("turysta123"))
				.andExpect(jsonPath("$.profile.firstName").value("Adam"))
				.andExpect(jsonPath("$.profile.height").value(170))
				.andExpect(jsonPath("$.profile.weight").value(75))
				.andExpect(jsonPath("$.profile.hikingLevel").value("BEGINNER"));
	}

	@Test
	void rejectsInvalidProfileData() throws Exception {
		String token = registerAndGetToken();

		mockMvc.perform(put("/api/users/me")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "firstName": "Adam",
						  "height": 20,
						  "weight": 75,
						  "hikingLevel": "BEGINNER"
						}
						"""))
				.andExpect(status().isBadRequest());
	}

	private String registerAndGetToken() throws Exception {
		String response = mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "username": "turysta123",
						  "email": "turysta@example.com",
						  "password": "StrongPassword123!"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.token", not(blankOrNullString())))
				.andReturn()
				.getResponse()
				.getContentAsString();

		return extractToken(response);
	}

	private String extractToken(String response) {
		String marker = "\"token\":\"";
		int start = response.indexOf(marker);
		if (start < 0) {
			throw new IllegalStateException("Token is missing in response: " + response);
		}
		start += marker.length();
		int end = response.indexOf('"', start);
		return response.substring(start, end);
	}
}
