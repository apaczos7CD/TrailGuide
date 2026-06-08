package pl.trailguide.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.math.BigDecimal;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pl.trailguide.app.api.ApiClient;
import pl.trailguide.app.api.AuthResponse;
import pl.trailguide.app.api.FcmTokenRequest;
import pl.trailguide.app.api.FcmTokenResponse;
import pl.trailguide.app.api.FinishTripRequest;
import pl.trailguide.app.api.LocationPointResponse;
import pl.trailguide.app.api.LoginRequest;
import pl.trailguide.app.api.RegisterRequest;
import pl.trailguide.app.api.StartTripRequest;
import pl.trailguide.app.api.TrailGuideApi;
import pl.trailguide.app.api.TripResponse;
import pl.trailguide.app.api.UpdateUserProfileRequest;
import pl.trailguide.app.api.UserMeResponse;
import pl.trailguide.app.api.UserProfileResponse;
import pl.trailguide.app.auth.TokenStorage;
import pl.trailguide.app.notifications.NotificationHelper;
import pl.trailguide.app.tracking.LocationTrackingService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextInputEditText usernameInput;
    private TextInputEditText tripTitleInput;
    private TextInputEditText tripDescriptionInput;
    private TextInputEditText profileFirstNameInput;
    private TextInputEditText profileCityInput;
    private TextInputEditText profileHeightInput;
    private TextInputEditText profileWeightInput;

    private View authPanel;
    private View homePanel;
    private View profilePanel;
    private View currentTripPanel;
    private View historyPanel;
    private View tripDetailsPanel;

    private MaterialButton loginButton;
    private MaterialButton registerButton;
    private MaterialButton polishLanguageButton;
    private MaterialButton englishLanguageButton;
    private MaterialButton logoutButton;
    private MaterialButton currentTripMenuButton;
    private MaterialButton profileMenuButton;
    private MaterialButton historyMenuButton;
    private MaterialButton startTripButton;
    private MaterialButton finishTripButton;
    private MaterialButton refreshCurrentTripButton;
    private MaterialButton refreshHistoryButton;
    private MaterialButton refreshProfileButton;
    private MaterialButton saveProfileButton;
    private MaterialButton backFromProfileButton;
    private MaterialButton backFromCurrentButton;
    private MaterialButton backFromHistoryButton;
    private MaterialButton backFromDetailsButton;

    private TextView userSummaryText;
    private TextView profileText;
    private TextView currentTripText;
    private TextView gpsStatusText;
    private TextView historyTripsText;
    private LinearLayout historyTripsList;
    private TextView tripDetailsText;
    private MapView tripMapView;
    private TextView statusText;

    private TokenStorage tokenStorage;
    private TrailGuideApi api;
    private ActivityResultLauncher<String> locationPermissionLauncher;
    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private UserMeResponse currentUser;
    private Long activeTripId;
    private boolean trackingLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bindViews();
        setupLocationTracking();
        setupNotifications();

        try {
            tokenStorage = new TokenStorage(this);
            api = ApiClient.create(tokenStorage::getToken);
        } catch (GeneralSecurityException | IOException e) {
            setStatus(getString(R.string.status_token_storage_init_error, e.getMessage()));
            setAuthButtonsEnabled(false);
            return;
        }

        loginButton.setOnClickListener(v -> login());
        registerButton.setOnClickListener(v -> register());
        polishLanguageButton.setOnClickListener(v -> setAppLanguage("pl"));
        englishLanguageButton.setOnClickListener(v -> setAppLanguage("en"));
        updateLanguageSelection();
        logoutButton.setOnClickListener(v -> logout());
        currentTripMenuButton.setOnClickListener(v -> openCurrentTrip());
        profileMenuButton.setOnClickListener(v -> openProfile());
        historyMenuButton.setOnClickListener(v -> openHistory());
        startTripButton.setOnClickListener(v -> startTrip());
        finishTripButton.setOnClickListener(v -> finishActiveTrip());
        refreshCurrentTripButton.setOnClickListener(v -> loadCurrentTrip());
        refreshProfileButton.setOnClickListener(v -> loadProfile());
        saveProfileButton.setOnClickListener(v -> saveProfile());
        refreshHistoryButton.setOnClickListener(v -> loadHistory());
        backFromProfileButton.setOnClickListener(v -> showHomePanel());
        backFromCurrentButton.setOnClickListener(v -> showHomePanel());
        backFromHistoryButton.setOnClickListener(v -> showHomePanel());
        backFromDetailsButton.setOnClickListener(v -> showHistoryPanel());

        if (tokenStorage.getToken() != null) {
            setStatus(getString(R.string.status_saved_token_found));
            loadCurrentUser();
        } else {
            showAuthForm();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tripMapView != null) {
            tripMapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (tripMapView != null) {
            tripMapView.onPause();
        }
        super.onPause();
    }

    private void bindViews() {
        authPanel = findViewById(R.id.authPanel);
        homePanel = findViewById(R.id.homePanel);
        profilePanel = findViewById(R.id.profilePanel);
        currentTripPanel = findViewById(R.id.currentTripPanel);
        historyPanel = findViewById(R.id.historyPanel);
        tripDetailsPanel = findViewById(R.id.tripDetailsPanel);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        usernameInput = findViewById(R.id.usernameInput);
        tripTitleInput = findViewById(R.id.tripTitleInput);
        tripDescriptionInput = findViewById(R.id.tripDescriptionInput);
        profileFirstNameInput = findViewById(R.id.profileFirstNameInput);
        profileCityInput = findViewById(R.id.profileCityInput);
        profileHeightInput = findViewById(R.id.profileHeightInput);
        profileWeightInput = findViewById(R.id.profileWeightInput);

        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        polishLanguageButton = findViewById(R.id.polishLanguageButton);
        englishLanguageButton = findViewById(R.id.englishLanguageButton);
        logoutButton = findViewById(R.id.logoutButton);
        currentTripMenuButton = findViewById(R.id.currentTripMenuButton);
        profileMenuButton = findViewById(R.id.profileMenuButton);
        historyMenuButton = findViewById(R.id.historyMenuButton);
        startTripButton = findViewById(R.id.startTripButton);
        finishTripButton = findViewById(R.id.finishTripButton);
        refreshCurrentTripButton = findViewById(R.id.refreshCurrentTripButton);
        refreshProfileButton = findViewById(R.id.refreshProfileButton);
        saveProfileButton = findViewById(R.id.saveProfileButton);
        refreshHistoryButton = findViewById(R.id.refreshHistoryButton);
        backFromProfileButton = findViewById(R.id.backFromProfileButton);
        backFromCurrentButton = findViewById(R.id.backFromCurrentButton);
        backFromHistoryButton = findViewById(R.id.backFromHistoryButton);
        backFromDetailsButton = findViewById(R.id.backFromDetailsButton);

        userSummaryText = findViewById(R.id.userSummaryText);
        profileText = findViewById(R.id.profileText);
        currentTripText = findViewById(R.id.currentTripText);
        gpsStatusText = findViewById(R.id.gpsStatusText);
        historyTripsText = findViewById(R.id.historyTripsText);
        historyTripsList = findViewById(R.id.historyTripsList);
        tripDetailsText = findViewById(R.id.tripDetailsText);
        tripMapView = findViewById(R.id.tripMapView);
        statusText = findViewById(R.id.statusText);

        tripMapView.setTileSource(TileSourceFactory.MAPNIK);
        tripMapView.setMultiTouchControls(true);
        tripMapView.getController().setZoom(14.0);
    }

    private void setupLocationTracking() {
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        startLocationTracking();
                    } else {
                        setGpsStatus(getString(R.string.gps_status_location_permission_denied));
                    }
                });
    }

    private void setupNotifications() {
        NotificationHelper.createPushChannel(this);
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!granted) {
                        setStatus(getString(R.string.status_notification_permission_denied));
                    }
                });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void setAppLanguage(String languageTag) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag));
        updateLanguageSelection();
    }

    private void updateLanguageSelection() {
        String languageTags = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        if (languageTags.startsWith("en")) {
            englishLanguageButton.setChecked(true);
        } else {
            polishLanguageButton.setChecked(true);
        }
    }

    private void login() {
        String email = read(emailInput);
        String password = read(passwordInput);
        if (!validateCredentials(email, password)) {
            return;
        }

        setAuthBusy(getString(R.string.status_login_in_progress));
        api.login(new LoginRequest(email, password)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                handleAuthResponse(response);
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setAuthDone(getString(R.string.status_login_error, t.getMessage()));
            }
        });
    }

    private void register() {
        String username = read(usernameInput);
        String email = read(emailInput);
        String password = read(passwordInput);
        if (username.length() < 3) {
            setStatus(getString(R.string.status_username_too_short));
            return;
        }
        if (!validateCredentials(email, password)) {
            return;
        }

        setAuthBusy(getString(R.string.status_register_in_progress));
        api.register(new RegisterRequest(username, email, password)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                handleAuthResponse(response);
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setAuthDone(getString(R.string.status_register_error, t.getMessage()));
            }
        });
    }

    private void handleAuthResponse(Response<AuthResponse> response) {
        if (!response.isSuccessful() || response.body() == null || response.body().getToken() == null) {
            setAuthDone(httpStatus(R.string.status_auth_failed_http, response));
            return;
        }

        tokenStorage.saveToken(response.body().getToken());
        setStatus(getString(R.string.status_token_saved));
        loadCurrentUser();
    }

    private void registerFcmToken() {
        if (FirebaseApp.getApps(this).isEmpty()) {
            setStatus(getString(R.string.status_fcm_not_configured));
            return;
        }

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(fcmToken -> {
                    if (fcmToken == null || fcmToken.isEmpty()) {
                        setStatus(getString(R.string.status_fcm_token_empty));
                        return;
                    }
                    api.registerFcmToken(new FcmTokenRequest(fcmToken)).enqueue(new Callback<FcmTokenResponse>() {
                        @Override
                        public void onResponse(Call<FcmTokenResponse> call, Response<FcmTokenResponse> response) {
                            if (response.isSuccessful()) {
                                setStatus(getString(R.string.status_fcm_token_registered));
                            } else {
                                setStatus(httpStatus(R.string.status_fcm_register_failed_http, response));
                            }
                        }

                        @Override
                        public void onFailure(Call<FcmTokenResponse> call, Throwable t) {
                            setStatus(getString(R.string.status_fcm_register_error, t.getMessage()));
                        }
                    });
                })
                .addOnFailureListener(e -> setStatus(getString(R.string.status_fcm_token_fetch_error, e.getMessage())));
    }

    private void loadCurrentUser() {
        setAuthButtonsEnabled(false);
        api.me().enqueue(new Callback<UserMeResponse>() {
            @Override
            public void onResponse(Call<UserMeResponse> call, Response<UserMeResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    setAuthDone(httpStatus(R.string.status_users_me_failed_http, response));
                    return;
                }

                UserMeResponse user = response.body();
                currentUser = user;
                userSummaryText.setText(getString(R.string.user_summary_format,
                        user.getUsername(), user.getEmail(), user.getRole(), user.getId()));
                showHomePanel();
                setStatus(getString(R.string.status_logged_in_as, user.getUsername()));
                registerFcmToken();
                refreshActiveTripState();
            }

            @Override
            public void onFailure(Call<UserMeResponse> call, Throwable t) {
                setAuthDone(getString(R.string.status_users_me_error, t.getMessage()));
            }
        });
    }

    private void logout() {
        stopLocationTracking();
        tokenStorage.clear();
        activeTripId = null;
        currentUser = null;
        emailInput.setText("");
        passwordInput.setText("");
        usernameInput.setText("");
        tripTitleInput.setText("");
        tripDescriptionInput.setText("");
        profileFirstNameInput.setText("");
        profileCityInput.setText("");
        profileHeightInput.setText("");
        profileWeightInput.setText("");
        currentTripText.setText(R.string.current_trip_empty);
        gpsStatusText.setText(R.string.gps_status_idle);
        historyTripsText.setText(R.string.trips_empty);
        historyTripsList.removeAllViews();
        tripDetailsText.setText(R.string.trip_details_loading);
        clearTripMap();
        showAuthForm();
        setAuthDone(getString(R.string.status_logged_out));
    }

    private void openProfile() {
        showProfilePanel();
        if (currentUser != null) {
            profileText.setText(formatProfile(currentUser));
        }
        loadProfile();
    }

    private void loadProfile() {
        setProfileButtonsEnabled(false);
        profileText.setText(R.string.profile_loading);
        api.me().enqueue(new Callback<UserMeResponse>() {
            @Override
            public void onResponse(Call<UserMeResponse> call, Response<UserMeResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    profileText.setText(httpStatus(R.string.status_users_me_http, response));
                    setProfileDone(getString(R.string.status_profile_refresh_failed));
                    return;
                }

                currentUser = response.body();
                profileText.setText(formatProfile(currentUser));
                fillProfileForm(currentUser);
                setProfileDone(getString(R.string.status_profile_refreshed));
            }

            @Override
            public void onFailure(Call<UserMeResponse> call, Throwable t) {
                profileText.setText(getString(R.string.status_profile_error, t.getMessage()));
                setProfileDone(getString(R.string.status_profile_error_short));
            }
        });
    }

    private void saveProfile() {
        Integer height = parseProfileInteger(profileHeightInput, 50, 250);
        Integer weight = parseProfileInteger(profileWeightInput, 20, 300);
        if (height == null && !read(profileHeightInput).isEmpty()) {
            setStatus(getString(R.string.status_height_invalid));
            return;
        }
        if (weight == null && !read(profileWeightInput).isEmpty()) {
            setStatus(getString(R.string.status_weight_invalid));
            return;
        }

        setProfileButtonsEnabled(false);
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                emptyToNull(read(profileFirstNameInput)),
                emptyToNull(read(profileCityInput)),
                height,
                weight,
                null);

        api.updateMe(request).enqueue(new Callback<UserMeResponse>() {
            @Override
            public void onResponse(Call<UserMeResponse> call, Response<UserMeResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    profileText.setText(httpStatus(R.string.status_users_me_put_http, response));
                    setProfileDone(getString(R.string.status_profile_save_failed));
                    return;
                }

                currentUser = response.body();
                profileText.setText(formatProfile(currentUser));
                fillProfileForm(currentUser);
                setProfileDone(getString(R.string.status_profile_saved));
            }

            @Override
            public void onFailure(Call<UserMeResponse> call, Throwable t) {
                profileText.setText(getString(R.string.status_profile_save_error, t.getMessage()));
                setProfileDone(getString(R.string.status_profile_save_error_short));
            }
        });
    }

    private void openCurrentTrip() {
        showCurrentTripPanel();
        loadCurrentTrip();
    }

    private void openHistory() {
        showHistoryPanel();
        loadHistory();
    }

    private void startTrip() {
        String title = read(tripTitleInput);
        String description = read(tripDescriptionInput);

        setTripButtonsEnabled(false);
        setStatus(getString(R.string.status_trip_starting));
        api.startTrip(new StartTripRequest(title, description)).enqueue(new Callback<TripResponse>() {
            @Override
            public void onResponse(Call<TripResponse> call, Response<TripResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    setTripDone(httpStatus(R.string.status_trip_start_failed_http, response));
                    return;
                }

                activeTripId = response.body().getId();
                currentTripText.setText(formatTrip(response.body()));
                tripTitleInput.setText("");
                tripDescriptionInput.setText("");
                setTripDone(getString(R.string.status_trip_started, activeTripId));
                startLocationTracking();
            }

            @Override
            public void onFailure(Call<TripResponse> call, Throwable t) {
                setTripDone(getString(R.string.status_trip_start_error, t.getMessage()));
            }
        });
    }

    private void startLocationTracking() {
        if (activeTripId == null || trackingLocation) {
            return;
        }

        if (!hasLocationPermission()) {
            setStatus(getString(R.string.status_location_permission_needed));
            setGpsStatus(getString(R.string.gps_status_waiting_permission));
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }

        LocationTrackingService.start(this, activeTripId);
        trackingLocation = true;
        setGpsStatus(getString(R.string.gps_status_service_running));
    }

    private void stopLocationTracking() {
        LocationTrackingService.stop(this);
        trackingLocation = false;
        setGpsStatus(getString(R.string.gps_status_stopped));
    }

    private void finishActiveTrip() {
        if (activeTripId == null) {
            setStatus(getString(R.string.status_no_active_trip_to_finish));
            return;
        }

        long tripId = activeTripId;
        stopLocationTracking();
        setTripButtonsEnabled(false);
        setStatus(getString(R.string.status_trip_finishing, tripId));
        api.finishTrip(tripId, new FinishTripRequest()).enqueue(new Callback<TripResponse>() {
            @Override
            public void onResponse(Call<TripResponse> call, Response<TripResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    setTripDone(httpStatus(R.string.status_trip_finish_failed_http, response));
                    return;
                }

                activeTripId = null;
                currentTripText.setText(R.string.current_trip_empty);
                gpsStatusText.setText(R.string.gps_status_idle);
                setTripDone(getString(R.string.status_trip_finished, response.body().getDistanceMeters()));
            }

            @Override
            public void onFailure(Call<TripResponse> call, Throwable t) {
                setTripDone(getString(R.string.status_trip_finish_error, t.getMessage()));
            }
        });
    }

    private void loadCurrentTrip() {
        setTripButtonsEnabled(false);
        api.trips().enqueue(new Callback<List<TripResponse>>() {
            @Override
            public void onResponse(Call<List<TripResponse>> call, Response<List<TripResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    setTripDone(httpStatus(R.string.status_trips_failed_http, response));
                    return;
                }

                activeTripId = findActiveTripId(response.body());
                if (activeTripId == null) {
                    currentTripText.setText(R.string.current_trip_empty);
                    setTripDone(getString(R.string.status_no_active_trip));
                    return;
                }

                loadTripDetails(activeTripId);
            }

            @Override
            public void onFailure(Call<List<TripResponse>> call, Throwable t) {
                setTripDone(getString(R.string.status_trips_error, t.getMessage()));
            }
        });
    }

    private void loadTripDetails(long tripId) {
        api.tripDetails(tripId).enqueue(new Callback<TripResponse>() {
            @Override
            public void onResponse(Call<TripResponse> call, Response<TripResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    setTripDone(httpStatus(R.string.status_trip_details_failed_http, response, tripId));
                    return;
                }

                currentTripText.setText(formatTrip(response.body()));
                setTripDone(getString(R.string.status_current_trip_refreshed));
                if (!response.body().isFinished()) {
                    activeTripId = response.body().getId();
                    startLocationTracking();
                }
            }

            @Override
            public void onFailure(Call<TripResponse> call, Throwable t) {
                setTripDone(getString(R.string.status_trip_details_error, t.getMessage()));
            }
        });
    }

    private void loadHistory() {
        setHistoryButtonsEnabled(false);
        api.trips().enqueue(new Callback<List<TripResponse>>() {
            @Override
            public void onResponse(Call<List<TripResponse>> call, Response<List<TripResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    setHistoryDone(httpStatus(R.string.status_trips_failed_http, response));
                    return;
                }

                List<TripResponse> trips = response.body();
                activeTripId = findActiveTripId(trips);
                List<TripResponse> finishedTrips = finishedTrips(trips);
                historyTripsList.removeAllViews();
                if (finishedTrips.isEmpty()) {
                    historyTripsText.setText(R.string.history_empty);
                    setHistoryDone(getString(R.string.status_history_refreshed));
                    return;
                }

                historyTripsText.setText(R.string.history_choose_trip);
                renderHistoryTripButtons(finishedTrips);
                setHistoryDone(getString(R.string.status_history_refreshed));
            }

            @Override
            public void onFailure(Call<List<TripResponse>> call, Throwable t) {
                setHistoryDone(getString(R.string.status_trips_error, t.getMessage()));
            }
        });
    }

    private void renderHistoryTripButtons(List<TripResponse> trips) {
        historyTripsList.removeAllViews();
        for (TripResponse trip : trips) {
            MaterialButton button = new MaterialButton(this);
            button.setText(formatTripListItem(trip));
            button.setAllCaps(false);
            button.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            button.setOnClickListener(v -> openTripDetails(trip.getId()));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 12);
            historyTripsList.addView(button, params);
        }
    }

    private void openTripDetails(long tripId) {
        showTripDetailsPanel();
        tripDetailsText.setText(R.string.trip_details_loading);
        clearTripMap();
        setDetailsButtonsEnabled(false);
        api.tripDetails(tripId).enqueue(new Callback<TripResponse>() {
            @Override
            public void onResponse(Call<TripResponse> call, Response<TripResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    tripDetailsText.setText(httpStatus(R.string.status_trip_details_fetch_failed_http, response));
                    setDetailsDone(getString(R.string.status_trip_details_fetch_failed));
                    return;
                }

                TripResponse trip = response.body();
                tripDetailsText.setText(formatTripDetails(trip));
                renderTripMap(trip);
                setDetailsDone(getString(R.string.status_trip_details_loaded));
            }

            @Override
            public void onFailure(Call<TripResponse> call, Throwable t) {
                tripDetailsText.setText(getString(R.string.status_trip_details_error, t.getMessage()));
                setDetailsDone(getString(R.string.status_trip_details_error_short));
            }
        });
    }

    private void refreshActiveTripState() {
        api.trips().enqueue(new Callback<List<TripResponse>>() {
            @Override
            public void onResponse(Call<List<TripResponse>> call, Response<List<TripResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    activeTripId = findActiveTripId(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<TripResponse>> call, Throwable t) {
                setStatus(getString(R.string.status_logged_in_trips_refresh_error, t.getMessage()));
            }
        });
    }

    private boolean validateCredentials(String email, String password) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            setStatus(getString(R.string.status_credentials_required));
            return false;
        }
        if (password.length() < 8) {
            setStatus(getString(R.string.status_password_too_short));
            return false;
        }
        return true;
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private String read(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private String readError(Response<?> response) {
        if (response.errorBody() == null) {
            return "";
        }
        try {
            String error = response.errorBody().string();
            return error.isEmpty() ? "" : "\n" + error;
        } catch (IOException e) {
            return "";
        }
    }

    private String httpStatus(int messageResId, Response<?> response, Object... args) {
        Object[] formatArgs = new Object[args.length + 2];
        System.arraycopy(args, 0, formatArgs, 0, args.length);
        formatArgs[args.length] = response.code();
        formatArgs[args.length + 1] = readError(response);
        return getString(messageResId, formatArgs);
    }

    private void showAuthForm() {
        authPanel.setVisibility(View.VISIBLE);
        homePanel.setVisibility(View.GONE);
        profilePanel.setVisibility(View.GONE);
        currentTripPanel.setVisibility(View.GONE);
        historyPanel.setVisibility(View.GONE);
        tripDetailsPanel.setVisibility(View.GONE);
        setAuthButtonsEnabled(true);
    }

    private void showHomePanel() {
        authPanel.setVisibility(View.GONE);
        homePanel.setVisibility(View.VISIBLE);
        profilePanel.setVisibility(View.GONE);
        currentTripPanel.setVisibility(View.GONE);
        historyPanel.setVisibility(View.GONE);
        tripDetailsPanel.setVisibility(View.GONE);
        setHomeButtonsEnabled(true);
    }

    private void showProfilePanel() {
        authPanel.setVisibility(View.GONE);
        homePanel.setVisibility(View.GONE);
        profilePanel.setVisibility(View.VISIBLE);
        currentTripPanel.setVisibility(View.GONE);
        historyPanel.setVisibility(View.GONE);
        tripDetailsPanel.setVisibility(View.GONE);
        setProfileButtonsEnabled(true);
    }

    private void showCurrentTripPanel() {
        authPanel.setVisibility(View.GONE);
        homePanel.setVisibility(View.GONE);
        profilePanel.setVisibility(View.GONE);
        currentTripPanel.setVisibility(View.VISIBLE);
        historyPanel.setVisibility(View.GONE);
        tripDetailsPanel.setVisibility(View.GONE);
        setTripButtonsEnabled(true);
    }

    private void showHistoryPanel() {
        authPanel.setVisibility(View.GONE);
        homePanel.setVisibility(View.GONE);
        profilePanel.setVisibility(View.GONE);
        currentTripPanel.setVisibility(View.GONE);
        historyPanel.setVisibility(View.VISIBLE);
        tripDetailsPanel.setVisibility(View.GONE);
        setHistoryButtonsEnabled(true);
    }

    private void showTripDetailsPanel() {
        authPanel.setVisibility(View.GONE);
        homePanel.setVisibility(View.GONE);
        profilePanel.setVisibility(View.GONE);
        currentTripPanel.setVisibility(View.GONE);
        historyPanel.setVisibility(View.GONE);
        tripDetailsPanel.setVisibility(View.VISIBLE);
        setDetailsButtonsEnabled(true);
    }

    private void setAuthBusy(String message) {
        setAuthButtonsEnabled(false);
        setStatus(message);
    }

    private void setAuthDone(String message) {
        setAuthButtonsEnabled(true);
        setStatus(message);
    }

    private void setTripDone(String message) {
        setTripButtonsEnabled(true);
        setStatus(message);
    }

    private void setHistoryDone(String message) {
        setHistoryButtonsEnabled(true);
        setStatus(message);
    }

    private void setProfileDone(String message) {
        setProfileButtonsEnabled(true);
        setStatus(message);
    }

    private void setDetailsDone(String message) {
        setDetailsButtonsEnabled(true);
        setStatus(message);
    }

    private void setAuthButtonsEnabled(boolean enabled) {
        loginButton.setEnabled(enabled);
        registerButton.setEnabled(enabled);
    }

    private void setHomeButtonsEnabled(boolean enabled) {
        currentTripMenuButton.setEnabled(enabled);
        profileMenuButton.setEnabled(enabled);
        historyMenuButton.setEnabled(enabled);
        logoutButton.setEnabled(enabled);
    }

    private void setProfileButtonsEnabled(boolean enabled) {
        saveProfileButton.setEnabled(enabled);
        refreshProfileButton.setEnabled(enabled);
        backFromProfileButton.setEnabled(enabled);
    }

    private void setTripButtonsEnabled(boolean enabled) {
        startTripButton.setEnabled(enabled && activeTripId == null);
        refreshCurrentTripButton.setEnabled(enabled);
        backFromCurrentButton.setEnabled(enabled);
        finishTripButton.setEnabled(enabled && activeTripId != null);
    }

    private void setHistoryButtonsEnabled(boolean enabled) {
        refreshHistoryButton.setEnabled(enabled);
        backFromHistoryButton.setEnabled(enabled);
    }

    private void setDetailsButtonsEnabled(boolean enabled) {
        backFromDetailsButton.setEnabled(enabled);
    }

    private void setStatus(String message) {
        statusText.setText(message);
        statusText.setVisibility(View.VISIBLE);
    }

    private void setGpsStatus(String message) {
        gpsStatusText.setText(message);
        setStatus(message);
    }

    private Long findActiveTripId(List<TripResponse> trips) {
        for (TripResponse trip : trips) {
            if (!trip.isFinished()) {
                return trip.getId();
            }
        }
        return null;
    }

    private List<TripResponse> finishedTrips(List<TripResponse> trips) {
        List<TripResponse> finishedTrips = new ArrayList<>();
        for (TripResponse trip : trips) {
            if (trip.isFinished()) {
                finishedTrips.add(trip);
            }
        }
        return finishedTrips;
    }

    private String formatProfile(UserMeResponse user) {
        UserProfileResponse profile = user.getProfile();
        return getString(R.string.profile_first_name_label) + ": " + profileString(profile == null ? null : profile.getFirstName())
                + "\n" + getString(R.string.profile_city_label) + ": " + profileString(profile == null ? null : profile.getCity())
                + "\n" + getString(R.string.profile_height_label) + ": "
                + profileInteger(profile == null ? null : profile.getHeight(), R.string.profile_height_unit)
                + "\n" + getString(R.string.profile_weight_label) + ": "
                + profileInteger(profile == null ? null : profile.getWeight(), R.string.profile_weight_unit);
    }

    private String profileString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return getString(R.string.profile_empty_value);
        }
        return value;
    }

    private String profileInteger(Integer value, int unitResId) {
        if (value == null) {
            return getString(R.string.profile_empty_value);
        }
        return value + getString(unitResId);
    }

    private void fillProfileForm(UserMeResponse user) {
        UserProfileResponse profile = user.getProfile();
        profileFirstNameInput.setText(profile == null || profile.getFirstName() == null ? "" : profile.getFirstName());
        profileCityInput.setText(profile == null || profile.getCity() == null ? "" : profile.getCity());
        profileHeightInput.setText(profile == null || profile.getHeight() == null ? "" : String.valueOf(profile.getHeight()));
        profileWeightInput.setText(profile == null || profile.getWeight() == null ? "" : String.valueOf(profile.getWeight()));
    }

    private Integer parseProfileInteger(TextInputEditText input, int min, int max) {
        String value = read(input);
        if (value.isEmpty()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < min || parsed > max) {
                return null;
            }
            return parsed;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    private String formatTrip(TripResponse trip) {
        return formatTripDetails(trip);
    }

    private String formatTripListItem(TripResponse trip) {
        return "#" + trip.getId()
                + "  " + trip.getTitle()
                + "\n" + getString(R.string.trip_start_label) + ": " + trip.getStartTime()
                + "\n" + getString(R.string.trip_distance_label) + ": " + formatDistance(trip.getDistanceMeters())
                + "   " + getString(R.string.trip_points_label) + ": " + trip.getLocationPointCount();
    }

    private String formatTripDetails(TripResponse trip) {
        StringBuilder builder = new StringBuilder();
        builder.append("#").append(trip.getId())
                .append("  ").append(trip.getTitle())
                .append(trip.isFinished()
                        ? "  [" + getString(R.string.trip_status_finished) + "]"
                        : "  [" + getString(R.string.trip_status_active) + "]")
                .append("\n").append(getString(R.string.trip_start_label)).append(": ").append(trip.getStartTime());
        if (trip.getEndTime() != null) {
            builder.append("\n").append(getString(R.string.trip_end_label)).append(": ").append(trip.getEndTime());
        }
        builder.append("\n").append(getString(R.string.trip_duration_label)).append(": ").append(formatDuration(trip))
                .append("\n").append(getString(R.string.trip_points_label)).append(": ").append(trip.getLocationPointCount())
                .append("\n").append(getString(R.string.trip_distance_label)).append(": ").append(formatDistance(trip.getDistanceMeters()));
        if (trip.getDescription() != null && !trip.getDescription().isEmpty()) {
            builder.append("\n").append(getString(R.string.trip_description_label)).append(": ").append(trip.getDescription());
        }
        builder.append(formatLocationPoints(trip.getLocationPoints()));
        return builder.toString();
    }

    private String formatLocationPoints(List<LocationPointResponse> points) {
        if (points == null || points.isEmpty()) {
            return "\n\n" + getString(R.string.gps_points_label) + ":\n" + getString(R.string.gps_points_empty);
        }

        StringBuilder builder = new StringBuilder("\n\n" + getString(R.string.gps_points_label) + ":");
        for (int i = 0; i < points.size(); i++) {
            LocationPointResponse point = points.get(i);
            builder.append("\n").append(i + 1).append(". ")
                    .append(point.getTimestamp())
                    .append("\n   ").append(getString(R.string.gps_latitude_label)).append(": ").append(point.getLatitude())
                    .append("\n   ").append(getString(R.string.gps_longitude_label)).append(": ").append(point.getLongitude());
            if (point.getAltitude() != null) {
                builder.append("\n   ").append(getString(R.string.gps_altitude_label)).append(": ")
                        .append(point.getAltitude()).append(getString(R.string.distance_meter_unit));
            }
            if (point.getAccuracy() != null) {
                builder.append("\n   ").append(getString(R.string.gps_accuracy_label)).append(": ")
                        .append(point.getAccuracy()).append(getString(R.string.distance_meter_unit));
            }
        }
        return builder.toString();
    }

    private void renderTripMap(TripResponse trip) {
        clearTripMap();
        List<GeoPoint> geoPoints = toGeoPoints(trip.getLocationPoints());
        if (geoPoints.isEmpty()) {
            return;
        }

        Polyline routeLine = new Polyline();
        routeLine.setPoints(geoPoints);
        routeLine.setWidth(8f);
        tripMapView.getOverlays().add(routeLine);

        addMapMarker(geoPoints.get(0), getString(R.string.map_marker_start));
        if (geoPoints.size() > 1) {
            addMapMarker(geoPoints.get(geoPoints.size() - 1), trip.isFinished()
                    ? getString(R.string.map_marker_finish)
                    : getString(R.string.map_marker_last_point));
        }

        if (geoPoints.size() == 1) {
            tripMapView.getController().setCenter(geoPoints.get(0));
            tripMapView.getController().setZoom(16.0);
        } else {
            tripMapView.post(() -> tripMapView.zoomToBoundingBox(BoundingBox.fromGeoPoints(geoPoints), true, 64));
        }
        tripMapView.invalidate();
    }

    private void clearTripMap() {
        if (tripMapView == null) {
            return;
        }
        tripMapView.getOverlays().clear();
        tripMapView.invalidate();
    }

    private List<GeoPoint> toGeoPoints(List<LocationPointResponse> points) {
        List<GeoPoint> geoPoints = new ArrayList<>();
        if (points == null) {
            return geoPoints;
        }
        for (LocationPointResponse point : points) {
            if (point.getLatitude() != null && point.getLongitude() != null) {
                geoPoints.add(new GeoPoint(point.getLatitude().doubleValue(), point.getLongitude().doubleValue()));
            }
        }
        return geoPoints;
    }

    private void addMapMarker(GeoPoint position, String title) {
        Marker marker = new Marker(tripMapView);
        marker.setPosition(position);
        marker.setTitle(title);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        tripMapView.getOverlays().add(marker);
    }

    private String formatDistance(BigDecimal distanceMeters) {
        if (distanceMeters == null) {
            return getString(R.string.distance_zero);
        }
        double meters = distanceMeters.doubleValue();
        return String.format(Locale.US, getString(R.string.distance_format), meters, meters / 1000.0);
    }

    private String formatDuration(TripResponse trip) {
        Instant start = parseInstant(trip.getStartTime());
        if (start == null) {
            return getString(R.string.duration_no_data);
        }
        Instant end = trip.getEndTime() == null ? Instant.now() : parseInstant(trip.getEndTime());
        if (end == null || end.isBefore(start)) {
            return getString(R.string.duration_no_data);
        }

        Duration duration = Duration.between(start, end);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        if (hours > 0) {
            return String.format(Locale.US, getString(R.string.duration_hours_format), hours, minutes, seconds);
        }
        return String.format(Locale.US, getString(R.string.duration_minutes_format), minutes, seconds);
    }

    private Instant parseInstant(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

}
