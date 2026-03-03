package polete.utaplayer

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface LyricsApi {
    @GET("get")
    //suspend per executar en fil secundari
    suspend fun getLyrics(
        @Query("artist_name") artist: String,
        @Query("track_name") title: String
    ): LrcResponse

    @GET("search")
    suspend fun getLyricsLlista(
        @Query("q") value: String
    ): List<LyricSearchResult>
}

data class LrcResponse(
    val syncedLyrics: String? // text tipus LRC: [mm:ss.xx] ...
)

data class LyricSearchResult(
    val id: Int,
    val trackName: String,
    val artistName: String,
    val albumName: String?,
    val duration: Double?,
    val plainLyrics: String?,
    val syncedLyrics: String?
)

// constructor
object RetrofitClient {
    val instance: LyricsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://lrclib.net/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LyricsApi::class.java)
    }
}