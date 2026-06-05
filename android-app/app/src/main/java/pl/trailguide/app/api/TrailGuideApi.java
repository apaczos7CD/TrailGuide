package pl.trailguide.app.api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.POST;

public interface TrailGuideApi {
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @GET("api/users/me")
    Call<UserMeResponse> me();

    @POST("api/trips/start")
    Call<TripResponse> startTrip(@Body StartTripRequest request);

    @GET("api/trips")
    Call<List<TripResponse>> trips();

    @GET("api/trips/{id}")
    Call<TripResponse> tripDetails(@Path("id") long id);

    @POST("api/trips/{id}/location-points")
    Call<LocationPointResponse> addLocationPoint(
            @Path("id") long id,
            @Body AddLocationPointRequest request);

    @POST("api/trips/{id}/finish")
    Call<TripResponse> finishTrip(@Path("id") long id, @Body FinishTripRequest request);
}
