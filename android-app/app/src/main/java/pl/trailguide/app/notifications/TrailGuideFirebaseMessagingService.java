package pl.trailguide.app.notifications;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.security.GeneralSecurityException;

import pl.trailguide.app.R;
import pl.trailguide.app.api.ApiClient;
import pl.trailguide.app.api.FcmTokenRequest;
import pl.trailguide.app.api.FcmTokenResponse;
import pl.trailguide.app.api.TrailGuideApi;
import pl.trailguide.app.auth.TokenStorage;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrailGuideFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "TrailGuideFcm";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        registerToken(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);

        String title = getString(R.string.push_notification_default_title);
        String body = getString(R.string.push_notification_default_body);

        RemoteMessage.Notification notification = message.getNotification();
        if (notification != null) {
            if (notification.getTitle() != null && !notification.getTitle().isEmpty()) {
                title = notification.getTitle();
            }
            if (notification.getBody() != null && !notification.getBody().isEmpty()) {
                body = notification.getBody();
            }
        }

        if (message.getData().containsKey("title")) {
            title = message.getData().get("title");
        }
        if (message.getData().containsKey("body")) {
            body = message.getData().get("body");
        }

        NotificationHelper.showPushNotification(this, title, body);
    }

    private void registerToken(String fcmToken) {
        try {
            TokenStorage tokenStorage = new TokenStorage(this);
            if (tokenStorage.getToken() == null) {
                Log.d(TAG, "JWT is missing; FCM token will be registered after login.");
                return;
            }

            TrailGuideApi api = ApiClient.create(tokenStorage::getToken);
            api.registerFcmToken(new FcmTokenRequest(fcmToken)).enqueue(new Callback<FcmTokenResponse>() {
                @Override
                public void onResponse(Call<FcmTokenResponse> call, Response<FcmTokenResponse> response) {
                    if (!response.isSuccessful()) {
                        Log.w(TAG, "FCM token registration failed with HTTP " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<FcmTokenResponse> call, Throwable t) {
                    Log.w(TAG, "FCM token registration error", t);
                }
            });
        } catch (GeneralSecurityException | IOException e) {
            Log.w(TAG, "Cannot initialize token storage for FCM token registration", e);
        }
    }
}
