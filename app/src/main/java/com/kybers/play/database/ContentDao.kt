package com.kybers.play.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kybers.play.api.LiveStream
import com.kybers.play.api.Movie
import com.kybers.play.api.Series

/**
 * DAO (Data Access Object) para el contenido.
 * Esta interfaz define todas las interacciones con la base de datos (consultas, inserciones, etc.).
 * Room generará automáticamente el código necesario para implementar estos métodos.
 */
@Dao
interface ContentDao {

    // --- Operaciones para Canales (LiveStream) ---

    /**
     * Inserta una lista de canales.
     * OnConflictStrategy.REPLACE: Si un canal con el mismo ID ya existe, será reemplazado.
     * Esto es útil para mantener los datos actualizados.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLiveStreams(streams: List<LiveStream>)

    /**
     * Obtiene todos los canales que pertenecen a una categoría específica.
     */
    @Query("SELECT * FROM live_streams WHERE categoryId = :categoryId")
    suspend fun getLiveStreamsByCategory(categoryId: String): List<LiveStream>

    /**
     * Obtiene todos los canales guardados en la base de datos.
     */
    @Query("SELECT * FROM live_streams")
    suspend fun getAllLiveStreams(): List<LiveStream>

    /**
     * Borra todos los canales de la tabla.
     */
    @Query("DELETE FROM live_streams")
    suspend fun clearAllLiveStreams()

    // --- Operaciones para Películas (Movie) ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMovies(movies: List<Movie>)

    @Query("SELECT * FROM movies")
    suspend fun getAllMovies(): List<Movie>

    @Query("DELETE FROM movies")
    suspend fun clearAllMovies()

    // --- Operaciones para Series ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSeries(series: List<Series>)

    @Query("SELECT * FROM series")
    suspend fun getAllSeries(): List<Series>

    @Query("DELETE FROM series")
    suspend fun clearAllSeries()
}
