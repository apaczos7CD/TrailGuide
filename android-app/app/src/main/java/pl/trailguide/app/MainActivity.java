package pl.trailguide.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
import pl.trailguide.app.api.FinishTripRequest;
import pl.trailguide.app.api.LocationPointResponse;
import pl.trailguide.app.api.LoginRequest;
import pl.trailguide.app.api.RegisterRequest;
import pl.trailguide.app.api.StartTripRequest;
import pl.trailguide.app.api.TrailGuideApi;
import pl.trailguide.app.api.TripResponse;
import pl.trailguide.app.api.UserMeResponse;
import pl.trailguide.app.auth.TokenStorage;
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

    private View authPanel;
    private View homePanel;
    private View currentTripPanel;
    private View historyPanel;
    private View tripDetailsPanel;

    private MaterialButton loginButton;
    private MaterialButton registerButton;
    private MaterialButton logoutButton;
    private MaterialButton currentTripMenuButton;
    private MaterialButton historyMenuButton;
    private MaterialButton startTripButton;
    private MaterialButton finishTripButton;
    private MaterialButton refreshCurrentTripButton;
    private MaterialButton refreshHistoryButton;
    private MaterialButton backFromCurrentButton;
    private MaterialButton backFromHistoryButton;
    private MaterialButton backFromDetailsButton;

    private TextView userSummaryText;
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

        try {
            tokenStorage = new TokenStorage(this);
            api = ApiClient.create(tokenStorage::getToken);
        } catch (GeneralSecurityException | IOException e) {
            setStatus("Nie mozna zainicjalizowac bezpiecznego zapisu tokenu: " + e.getMessage());
            setAuthButtonsEnabled(false);
            return;
        }

        loginButton.setOnClickListener(v -> login());
        registerButton.setOnClickListener(v -> register());
        logoutButton.setOnClickListener(v -> logout());
        currentTripMenuButton.setOnClickListener(v -> openCurrentTrip());
        historyMenuButton.setOnClickListener(v -> openHistory());
        startTripButton.setOnClickListener(v -> startTrip());
        finishTripButton.setOnClickListener(v -> finishActiveTrip());
        refreshCurrentTripButton.setOnClickListener(v -> loadCurrentTrip());
        refreshHistoryButton.setOnClickListener(v -> loadHistory());
        backFromCurrentButton.setOnClickListener(v -> showHomePanel());
        backFromHistoryButton.setOnClickListener(v -> showHomePanel());
        backFromDetailsButton.setOnClickListener(v -> showHistoryPanel());

        if (tokenStorage.getToken() != null) {
            setStatus("Znaleziono zapisany token. Sprawdzam /api/users/me...");
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
        currentTripPanel = findViewById(R.id.currentTripPanel);
        historyPanel = findViewById(R.id.historyPanel);
        tripDetailsPanel = findViewById(R.id.tripDetailsPanel);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        usernameInput = findViewById(R.id.usernameInput);
        tripTitleInput = findViewById(R.id.tripTitleInput);
        tripDescriptionInput = findViewById(R.id.tripDescriptionInput);

        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        logoutButton = findViewById(R.id.logoutButton);
        currentTripMenuButton = findViewById(R.id.currentTripMenuButton);
        historyMenuButton = findViewById(R.id.historyMenuButton);
        startTripButton = findViewById(R.id.startTripButton);
        finishTripButton = findViewById(R.id.finishTripButton);
        refreshCurrentTripButton = findViewById(R.id.refreshCurrentTripButton);
        refreshHistoryButton = findViewById(R.id.refreshHistoryButton);
        backFromCurrentButton = findViewById(R.id.backFromCurrentButton);
        backFromHistoryButton = findViewById(R.id.backFromHistoryButton);
        backFromDetailsButton = findViewById(R.id.backFromDetailsButton);

        userSummaryText = findViewById(R.id.userSummaryText);
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
                        setGpsStatus("GPS: brak zgody lokalizacji. Trasa dziala, ale bez punktow.");
                    }
                });
    }

    private void login() {
        String email = read(emailInput);
        String password = read(passwordInput);
        if (!validateCredentials(email, password)) {
            return;
        }

        setAuthBusy("Logowanie...");
        api.login(new LoginRequest(email, password)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                handleAuthResponse(response);
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setAuthDone("Blad logowania: " + t.getMessage());
            }
        });
    }

    private void register() {
        String username = read(usernameInput);
        String email = read(emailInput);
        String password = read(passwordInput);
        if (username.length() < 3) {
            setStatus("Username musi miec co najmniej 3 znaki.");
            return;
        }
        if (!validateCredentials(email, password)) {
            return;
        }

        setAuthBusy("Rejestracja...");
        api.register(new RegisterRequest(username, email, password)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                handleAuthResponse(response);
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setAuthDone("Blad rejestracji: " + t.getMessage());
            }
        });
    }

    private void handleAuthResponse(Response<AuthResponse> response) {
        if (!response.isSuccessful() || response.body() == null || response.body().getToken() == null) {
            setAuthDone("Auth nie powiodl sie: HTTP " + response.code() + readError(response));
            return;
        }

        tokenStorage.saveToken(response.body().getToken());
        setStatus("Token zapisany. Sprawdzam /api/users/me...");
        loadCurrentUser();
    }

    private void loadCurrentUser() {
        setAuthButtonsEnabled(false);
        api.me().enqueue(new Callback<UserMeResponse>() {
            @Override
            public void onResponse(Call<UserMeResponse> call, Response<UserMeResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    setAuthDone("GET /api/users/me nie powiodl sie: HTTP " + response.code() + readError(response));
                    return;
                }

                UserMeResponse user = response.body();
                userSummaryText.setText("Username: " + user.getUsername()
                        + "\nEmail: " + user.getEmail()
                        + "\nRole: " + user.getRole()
                        + "\nID: " + user.getId());
                showHomePanel();
                setStatus("Zalogowano jako " + user.getUsername() + ".");
                refreshActiveTripState();
            }

            @Override
            public void onFailure(Call<UserMeResponse> call, Throwable t) {
                setAuthDone("Blad GET /api/users/me: " + t.getMessage());
            }
        });
    }

    private void logout() {
        stopLocationTracking();
        tokenStorage.clear();
        activeTripId = null;
        emailInput.setText("");
        passwordInput.setText("");
        usernameInput.setText("");
        tripTitleInput.setText("");
        tripDescriptionInput.setText("");
        currentTripText.setText(R.string.current_trip_empty);
        gpsStatusText.setText(R.string.gps_status_idle);
        historyTripsText.setText(R.string.trips_empty);
        historyTripsList.removeAllViews();
        tripDetailsText.setText(R.string.trip_details_loading);
        clearTripMap();
        showAuthForm();
        setAuthDone("Wylogowano. Token usuniety z pamieci aplikacji.");
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
        setStatus("Startuje trase...");
        api.startTrip(new StartTripRequest(title, description)).enqueue(new Callback<TripResponse>() {
            @Override
            public void onResponse(Call<TripResponse> call, Response<TripResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    setTripDone("Start trip nie powiodl sie: HTTP " + response.code() + readError(response));
                    return;
                }

                activeTripId = response.body().getId();
                currentTripText.setText(formatTrip(response.body()));
                tripTitleInput.setText("");
                tripDescriptionInput.setText("");
                setTripDone("Trasa wystartowala. ID: " + activeTripId);
                startLocationTracking();
            }

            @Override
            public void onFailure(Call<TripResponse> call, Throwable t) {
                setTripDone("Blad startu trasy: " + t.getMessage());
            }
        });
    }

    private void startLocationTracking() {
        if (activeTripId == null || trackingLocation) {
            return;
        }

        if (!hasLocationPermission()) {
            setStatus("Potrzebna zgoda lokalizacji do zapisu punktow GPS.");
            setGpsStatus("GPS: czeka na zgode lokalizacji.");
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }

        LocationTrackingService.start(this, activeTripId);
        trackingLocation = true;
        setGpsStatus("GPS: foreground service wlaczony. Punkty beda wysylane co ok. 10 sekund.");
    }

    private void stopLocationTracking() {
        LocationTrackingService.stop(this);
        trackingLocation = false;
        setGpsStatus("GPS: zatrzymany.");
    }

    private void finishActiveTrip() {
        if (activeTripId == null) {
            setStatus("Brak aktywnej trasy do zakonczenia.");
            return;
        }

        long tripId = activeTripId;
        stopLocationTracking();
        setTripButtonsEnabled(false);
        setStatus("Koncze trase ID " + tripId + "...");
        api.finishTrip(tripId, new FinishTripRequest()).enqueue(new Callback<TripResponse>() {
            @Override
            public void onResponse(Call<TripResponse> call, Response<TripResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    setTripDone("Finish trip nie powiodl sie: HTTP " + response.code() + readError(response));
                    return;
                }

                activeTripId = null;
                currentTripText.setText(R.string.current_trip_empty);
                gpsStatusText.setText(R.string.gps_status_idle);
                setTripDone("Trasa zakonczona. Dystans: " + response.body().getDistanceMeters() + " m");
            }

            @Override
            public void onFailure(Call<TripResponse> call, Throwable t) {
                setTripDone("Blad konczenia trasy: " + t.getMessage());
            }
        });
    }

    private void loadCurrentTrip() {
        setTripButtonsEnabled(false);
        api.trips().enqueue(new Callback<List<TripResponse>>() {
            @Override
            public void onResponse(Call<List<TripResponse>> call, Response<List<TripResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    setTripDone("GET /api/trips nie powiodl sie: HTTP " + response.code() + readError(response));
                    return;
                }

                activeTripId = findActiveTripId(response.body());
                if (activeTripId == null) {
                    currentTripText.setText(R.string.current_trip_empty);
                    setTripDone("Brak aktywnej trasy.");
                    return;
                }

                loadTripDetails(activeTripId);
            }

            @Override
            public void onFailure(Call<List<TripResponse>> call, Throwable t) {
                setTripDone("Blad GET /api/trips: " + t.getMessage());
            }
        });
    }

    private void loadTripDetails(long tripId) {
        api.tripDetails(tripId).enqueue(new Callback<TripResponse>() {
            @Override
            public void onResponse(Call<TripResponse> call, Response<TripResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    setTripDone("GET /api/trips/" + tripId + " nie powiodl sie: HTTP "
                            + response.code() + readError(response));
                    return;
                }

                currentTripText.setText(formatTrip(response.body()));
                setTripDone("Aktualna trasa odswiezona.");
                if (!response.body().isFinished()) {
                    activeTripId = response.body().getId();
                    startLocationTracking();
                }
            }

            @Override
            public void onFailure(Call<TripResponse> call, Throwable t) {
                setTripDone("Blad szczegolow trasy: " + t.getMessage());
            }
        });
    }

    private void loadHistory() {
        setHistoryButtonsEnabled(false);
        api.trips().enqueue(new Callback<List<TripResponse>>() {
            @Override
            public void onResponse(Call<List<TripResponse>> call, Response<List<TripResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    setHistoryDone("GET /api/trips nie powiodl sie: HTTP " + response.code() + readError(response));
                    return;
                }

                List<TripResponse> trips = response.body();
                activeTripId = findActiveTripId(trips);
                List<TripResponse> finishedTrips = finishedTrips(trips);
                historyTripsList.removeAllViews();
                if (finishedTrips.isEmpty()) {
                    historyTripsText.setText("Brak historycznych tras.");
                    setHistoryDone("Historia tras odswiezona.");
                    return;
                }

                historyTripsText.setText("Wybierz trase, aby zobaczyc szczegoly.");
                renderHistoryTripButtons(finishedTrips);
                setHistoryDone("Historia tras odswiezona.");
            }

            @Override
            public void onFailure(Call<List<TripResponse>> call, Throwable t) {
                setHistoryDone("Blad GET /api/trips: " + t.getMessage());
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
                    tripDetailsText.setText("Nie udalo sie pobrac szczegolow trasy: HTTP "
                            + response.code() + readError(response));
                    setDetailsDone("Nie udalo sie pobrac szczegolow trasy.");
                    return;
                }

                TripResponse trip = response.body();
                tripDetailsText.setText(formatTripDetails(trip));
                renderTripMap(trip);
                setDetailsDone("Szczegoly trasy zaladowane.");
            }

            @Override
            public void onFailure(Call<TripResponse> call, Throwable t) {
                tripDetailsText.setText("Blad szczegolow trasy: " + t.getMessage());
                setDetailsDone("Blad szczegolow trasy.");
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
                setStatus("Zalogowano. Nie udalo sie odswiezyc tras: " + t.getMessage());
            }
        });
    }

    private boolean validateCredentials(String email, String password) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            setStatus("Podaj email i haslo.");
            return false;
        }
        if (password.length() < 8) {
            setStatus("Haslo musi miec co najmniej 8 znakow.");
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

    private void showAuthForm() {
        authPanel.setVisibility(View.VISIBLE);
        homePanel.setVisibility(View.GONE);
        currentTripPanel.setVisibility(View.GONE);
        historyPanel.setVisibility(View.GONE);
        tripDetailsPanel.setVisibility(View.GONE);
        setAuthButtonsEnabled(true);
    }

    private void showHomePanel() {
        authPanel.setVisibility(View.GONE);
        homePanel.setVisibility(View.VISIBLE);
        currentTripPanel.setVisibility(View.GONE);
        historyPanel.setVisibility(View.GONE);
        tripDetailsPanel.setVisibility(View.GONE);
        setHomeButtonsEnabled(true);
    }

    private void showCurrentTripPanel() {
        authPanel.setVisibility(View.GONE);
        homePanel.setVisibility(View.GONE);
        currentTripPanel.setVisibility(View.VISIBLE);
        historyPanel.setVisibility(View.GONE);
        tripDetailsPanel.setVisibility(View.GONE);
        setTripButtonsEnabled(true);
    }

    private void showHistoryPanel() {
        authPanel.setVisibility(View.GONE);
        homePanel.setVisibility(View.GONE);
        currentTripPanel.setVisibility(View.GONE);
        historyPanel.setVisibility(View.VISIBLE);
        tripDetailsPanel.setVisibility(View.GONE);
        setHistoryButtonsEnabled(true);
    }

    private void showTripDetailsPanel() {
        authPanel.setVisibility(View.GONE);
        homePanel.setVisibility(View.GONE);
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
        historyMenuButton.setEnabled(enabled);
        logoutButton.setEnabled(enabled);
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

    private String formatTrip(TripResponse trip) {
        return formatTripDetails(trip);
    }

    private String formatTripListItem(TripResponse trip) {
        return "#" + trip.getId()
                + "  " + trip.getTitle()
                + "\nStart: " + trip.getStartTime()
                + "\nDystans: " + formatDistance(trip.getDistanceMeters())
                + "   Punkty: " + trip.getLocationPointCount();
    }

    private String formatTripDetails(TripResponse trip) {
        StringBuilder builder = new StringBuilder();
        builder.append("#").append(trip.getId())
                .append("  ").append(trip.getTitle())
                .append(trip.isFinished() ? "  [finished]" : "  [active]")
                .append("\nStart: ").append(trip.getStartTime());
        if (trip.getEndTime() != null) {
            builder.append("\nKoniec: ").append(trip.getEndTime());
        }
        builder.append("\nCzas: ").append(formatDuration(trip))
                .append("\nPunkty: ").append(trip.getLocationPointCount())
                .append("\nDystans: ").append(formatDistance(trip.getDistanceMeters()));
        if (trip.getDescription() != null && !trip.getDescription().isEmpty()) {
            builder.append("\nOpis: ").append(trip.getDescription());
        }
        builder.append(formatLocationPoints(trip.getLocationPoints()));
        return builder.toString();
    }

    private String formatLocationPoints(List<LocationPointResponse> points) {
        if (points == null || points.isEmpty()) {
            return "\n\nPunkty GPS:\nBrak punktow.";
        }

        StringBuilder builder = new StringBuilder("\n\nPunkty GPS:");
        for (int i = 0; i < points.size(); i++) {
            LocationPointResponse point = points.get(i);
            builder.append("\n").append(i + 1).append(". ")
                    .append(point.getTimestamp())
                    .append("\n   Lat: ").append(point.getLatitude())
                    .append("\n   Lng: ").append(point.getLongitude());
            if (point.getAltitude() != null) {
                builder.append("\n   Alt: ").append(point.getAltitude()).append(" m");
            }
            if (point.getAccuracy() != null) {
                builder.append("\n   Accuracy: ").append(point.getAccuracy()).append(" m");
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

        addMapMarker(geoPoints.get(0), "Start");
        if (geoPoints.size() > 1) {
            addMapMarker(geoPoints.get(geoPoints.size() - 1), trip.isFinished() ? "Finish" : "Last point");
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
            return "0 m / 0.00 km";
        }
        double meters = distanceMeters.doubleValue();
        return String.format(Locale.US, "%.2f m / %.3f km", meters, meters / 1000.0);
    }

    private String formatDuration(TripResponse trip) {
        Instant start = parseInstant(trip.getStartTime());
        if (start == null) {
            return "brak danych";
        }
        Instant end = trip.getEndTime() == null ? Instant.now() : parseInstant(trip.getEndTime());
        if (end == null || end.isBefore(start)) {
            return "brak danych";
        }

        Duration duration = Duration.between(start, end);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        if (hours > 0) {
            return String.format(Locale.US, "%d h %02d min %02d s", hours, minutes, seconds);
        }
        return String.format(Locale.US, "%d min %02d s", minutes, seconds);
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
