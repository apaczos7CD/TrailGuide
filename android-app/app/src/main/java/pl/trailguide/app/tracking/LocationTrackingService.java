package pl.trailguide.app.tracking;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.time.Instant;

import pl.trailguide.app.MainActivity;
import pl.trailguide.app.R;
import pl.trailguide.app.api.AddLocationPointRequest;
import pl.trailguide.app.api.ApiClient;
import pl.trailguide.app.api.LocationPointResponse;
import pl.trailguide.app.api.TrailGuideApi;
import pl.trailguide.app.auth.TokenStorage;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LocationTrackingService extends Service {
    public static final String ACTION_START = "pl.trailguide.app.tracking.START";
    public static final String ACTION_STOP = "pl.trailguide.app.tracking.STOP";
    public static final String EXTRA_TRIP_ID = "trip_id";

    private static final String CHANNEL_ID = "trip_tracking";
    private static final int NOTIFICATION_ID = 1001;
    private static final long LOCATION_INTERVAL_MILLIS = 10_000L;
    private static final long LOCATION_MIN_INTERVAL_MILLIS = 5_000L;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private TrailGuideApi api;
    private Long activeTripId;
    private int sentLocationPoints;
    private boolean tracking;

    public static void start(Context context, long tripId) {
        Intent intent = new Intent(context, LocationTrackingService.class)
                .setAction(ACTION_START)
                .putExtra(EXTRA_TRIP_ID, tripId);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, LocationTrackingService.class)
                .setAction(ACTION_STOP);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || activeTripId == null) {
                    return;
                }
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    sendLocationPoint(location);
                }
            }
        };

        try {
            TokenStorage tokenStorage = new TokenStorage(this);
            api = ApiClient.create(tokenStorage::getToken);
        } catch (GeneralSecurityException | IOException e) {
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || ACTION_STOP.equals(intent.getAction())) {
            stopTracking();
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ACTION_START.equals(intent.getAction())) {
            long tripId = intent.getLongExtra(EXTRA_TRIP_ID, -1L);
            if (tripId <= 0 || api == null || !hasLocationPermission()) {
                stopSelf();
                return START_NOT_STICKY;
            }

            activeTripId = tripId;
            sentLocationPoints = 0;
            startForegroundLocation(buildNotification(
                    getString(R.string.notification_tracking_title),
                    getString(R.string.notification_trip_prefix) + " #" + tripId));
            startTracking();
            return START_REDELIVER_INTENT;
        }

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopTracking();
        super.onDestroy();
    }

    @SuppressLint("MissingPermission")
    private void startTracking() {
        if (tracking || activeTripId == null || !hasLocationPermission()) {
            return;
        }

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_INTERVAL_MILLIS)
                .setMinUpdateIntervalMillis(LOCATION_MIN_INTERVAL_MILLIS)
                .build();

        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
                .addOnSuccessListener(unused -> tracking = true)
                .addOnFailureListener(e -> stopSelf());
    }

    private void stopTracking() {
        if (!tracking) {
            return;
        }
        fusedLocationClient.removeLocationUpdates(locationCallback);
        tracking = false;
    }

    private void sendLocationPoint(Location location) {
        Long tripId = activeTripId;
        if (tripId == null || api == null) {
            return;
        }

        AddLocationPointRequest request = new AddLocationPointRequest(
                BigDecimal.valueOf(location.getLatitude()),
                BigDecimal.valueOf(location.getLongitude()),
                location.hasAltitude() ? BigDecimal.valueOf(location.getAltitude()) : null,
                location.hasAccuracy() ? BigDecimal.valueOf(location.getAccuracy()) : null,
                Instant.ofEpochMilli(location.getTime() > 0 ? location.getTime() : System.currentTimeMillis()).toString());

        api.addLocationPoint(tripId, request).enqueue(new Callback<LocationPointResponse>() {
            @Override
            public void onResponse(Call<LocationPointResponse> call, Response<LocationPointResponse> response) {
                if (response.isSuccessful()) {
                    sentLocationPoints++;
                    updateNotification(getString(R.string.notification_point_sent, sentLocationPoints));
                }
            }

            @Override
            public void onFailure(Call<LocationPointResponse> call, Throwable t) {
                updateNotification(getString(R.string.notification_point_error));
            }
        });
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Trip tracking",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Aktywne zbieranie punktow GPS dla trasy");

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    private NotificationCompat.Builder notificationBuilder(String title, String text) {
        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                activityIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);
    }

    private android.app.Notification buildNotification(String title, String text) {
        return notificationBuilder(title, text).build();
    }

    private void startForegroundLocation(Notification notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void updateNotification(String text) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, buildNotification(getString(R.string.notification_tracking_title), text));
    }
}
