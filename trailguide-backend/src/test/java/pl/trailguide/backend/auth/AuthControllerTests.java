package pl.trailguide.backend.auth;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void registersUserAndReturnsJwt() throws Exception {
		mockMvc.perform(post("/api/auth/register")
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
				.andExpect(jsonPath("$.role").value("USER"));
	}

	@Test
	void logsUserInWithValidPassword() throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "username": "adam",
						  "email": "adam@example.com",
						  "password": "StrongPassword123!"
						}
						"""))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "adam@example.com",
						  "password": "StrongPassword123!"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token", not(blankOrNullString())))
				.andExpect(jsonPath("$.role").value("USER"));
	}

	@Test
	void rejectsInvalidPassword() throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "username": "ewa",
						  "email": "ewa@example.com",
						  "password": "StrongPassword123!"
						}
						"""))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "ewa@example.com",
						  "password": "wrong-password"
						}
						"""))
				.andExpect(status().isUnauthorized());
	}
}
