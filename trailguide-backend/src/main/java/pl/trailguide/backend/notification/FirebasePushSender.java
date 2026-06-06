package pl.trailguide.backend.notification;

import java.io.FileInputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;

@Component
public class FirebasePushSender {

	private final boolean enabled;
	private final String serviceAccountPath;

	public FirebasePushSender(
			@Value("${trailguide.firebase.enabled}") boolean enabled,
			@Value("${trailguide.firebase.service-account-path}") String serviceAccountPath) {
		this.enabled = enabled;
		this.serviceAccountPath = serviceAccountPath;
	}

	public void sendToTokens(String title, String body, List<String> tokens) {
		if (!enabled || tokens.isEmpty()) {
			return;
		}

		initializeFirebaseIfNeeded();
		for (String token : tokens) {
			sendToToken(title, body, token);
		}
	}

	private void sendToToken(String title, String body, String token) {
		try {
			Message message = Message.builder()
					.setToken(token)
					.setNotification(com.google.firebase.messaging.Notification.builder()
							.setTitle(title)
							.setBody(body)
							.build())
					.build();
			FirebaseMessaging.getInstance().send(message);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Cannot send Firebase notification", ex);
		}
	}

	private void initializeFirebaseIfNeeded() {
		if (!FirebaseApp.getApps().isEmpty()) {
			return;
		}
		try (FileInputStream serviceAccount = new FileInputStream(serviceAccountPath)) {
			FirebaseOptions options = FirebaseOptions.builder()
					.setCredentials(GoogleCredentials.fromStream(serviceAccount))
					.build();
			FirebaseApp.initializeApp(options);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Cannot initialize Firebase Admin SDK", ex);
		}
	}
}
