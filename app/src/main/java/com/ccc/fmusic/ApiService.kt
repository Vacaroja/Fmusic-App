package com.ccc.fmusic

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// Modelos de datos
data class Song(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnail: String,
    val type: String = "Canción"
)

data class SearchResponse(val songs: List<Song>)
data class VolumeRequest(val level: Int)
data class PlayRequest(val videoId: String)

data class TrackInfo(
    val videoId: String,
    val title: String,
    val artist: String,
    val duration: String,
    val thumbnail: String
)

data class PlaylistInfoResponse(
    val status: String,
    val playlistId: String,
    val title: String,
    val author: String,
    val thumbnail: String,
    val trackCount: Int,
    val tracks: List<TrackInfo>
)

data class NowPlayingData(
    val isPlaying: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val playlistId: String = "",
    val videoId: String = "",        // <-- Agregar este campo
    val thumbnail: String = "",
    val currentTime: Int = 0,
    val duration: Int = 0
)

data class NowPlayingResponse(
    val status: String,
    val data: NowPlayingData
)

data class VolumeResponse(val status: String, val volume: Int)

interface ApiService {
    @GET("search")
    suspend fun searchSongs(@Query("query") query: String): SearchResponse

    @GET("playlist-info")
    suspend fun getPlaylistInfo(@Query("playlistId") playlistId: String): PlaylistInfoResponse

    @POST("play")
    suspend fun playSong(@Body request: PlayRequest): Response<Unit>

    @POST("volume")
    suspend fun setVolume(@Body request: VolumeRequest): Response<Unit>

    @GET("volume")
    suspend fun getVolume(): VolumeResponse

    @GET("now-playing")
    suspend fun getNowPlaying(): NowPlayingResponse

    @POST("media/control")
    suspend fun mediaControl(@Query("action") action: String): Response<Unit>
}

object RetrofitClient {
    @Volatile
    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String? = null
    private var currentApiKey: String? = null

    fun getInstance(context: Context): ApiService {
        val prefs = AppPreferences(context)
        val ip = prefs.serverIp.trim()
        val port = prefs.serverPort
        val apiKey = prefs.apiKey.trim()

        val baseUrl = "http://$ip:$port/"

        if (retrofit == null || currentBaseUrl != baseUrl || currentApiKey != apiKey) {
            synchronized(this) {
                if (retrofit == null || currentBaseUrl != baseUrl || currentApiKey != apiKey) {
                    currentBaseUrl = baseUrl
                    currentApiKey = apiKey

                    // Interceptor para inyectar X-API-Key en las cabeceras HTTP
                    val authInterceptor = Interceptor { chain ->
                        val originalRequest = chain.request()
                        val newRequest = originalRequest.newBuilder()
                            .header("X-API-Key", apiKey)
                            .build()
                        chain.proceed(newRequest)
                    }

                    val okHttpClient = OkHttpClient.Builder()
                        .addInterceptor(authInterceptor)
                        .connectTimeout(5, TimeUnit.SECONDS)
                        .readTimeout(5, TimeUnit.SECONDS)
                        .writeTimeout(5, TimeUnit.SECONDS)
                        .build()

                    retrofit = Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .client(okHttpClient)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                }
            }
        }

        return retrofit!!.create(ApiService::class.java)
    }
}