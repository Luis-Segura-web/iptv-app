package com.kybers.play.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kybers.play.api.LiveStream
import com.kybers.play.api.Movie
import com.kybers.play.api.Series

@Dao
interface ContentDao {
    // --- Canales (LiveStream) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLiveStreams(streams: List<LiveStream>)

    @Query("SELECT * FROM live_streams WHERE categoryId = :categoryId")
    suspend fun getLiveStreamsByCategory(categoryId: String): List<LiveStream>

    @Query("SELECT * FROM live_streams")
    suspend fun getAllLiveStreams(): List<LiveStream>

    @Query("DELETE FROM live_streams")
    suspend fun clearAllLiveStreams()

    // --- Películas (Movie) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMovies(movies: List<Movie>)

    @Query("SELECT * FROM movies")
    suspend fun getAllMovies(): List<Movie>

    @Query("DELETE FROM movies")
    suspend fun clearAllMovies()

    // --- Series ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSeries(series: List<Series>)

    @Query("SELECT * FROM series")
    suspend fun getAllSeries(): List<Series>

    @Query("DELETE FROM series")
    suspend fun clearAllSeries()
}
