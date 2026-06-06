package pl.trailguide.app.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = "https://10.0.2.2:8443/";

    private ApiClient() {
    }

    public static TrailGuideApi create(TokenProvider tokenProvider) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    String token = tokenProvider.getToken();
                    if (token == null || token.isEmpty()) {
                        return chain.proceed(chain.request());
                    }
                    return chain.proceed(chain.request().newBuilder()
                            .header("Authorization", "Bearer " + token)
                            .build());
                })
                .addInterceptor(loggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(TrailGuideApi.class);
    }
}
