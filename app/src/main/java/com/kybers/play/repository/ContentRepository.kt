package com.kybers.play.repository

import com.kybers.play.api.LiveStream
import com.kybers.play.api.Movie
import com.kybers.play.api.Series
import com.kybers.play.api.XtreamApiService
import com.kybers.play.database.ContentDao

class ContentRepository(private val apiService: XtreamApiService, private val contentDao: ContentDao) {

    // Obtiene los canales: primero intenta desde el caché, si está vacío, va a la API.
    suspend fun getLiveStreams(username: String, password: String, categoryId: String): List<LiveStream> {
        val cachedStreams = contentDao.getLiveStreamsByCategory(categoryId)
        // CORREGIDO: Usamos la función de extensión 'ifEmpty' para un código más limpio.
        return cachedStreams.ifEmpty {
            try {
                val response = apiService.getLiveStreams(username, password, categoryId = categoryId)
                if (response.isSuccessful) {
                    val streams = response.body() ?: emptyList()
                    contentDao.insertAllLiveStreams(streams)
                    streams
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // Obtiene las películas: primero del caché, si no, de la API.
    suspend fun getMovies(username: String, password: String): List<Movie> {
        val cachedMovies = contentDao.getAllMovies()
        // CORREGIDO: Usamos la función de extensión 'ifEmpty' para un código más limpio.
        return cachedMovies.ifEmpty {
            try {
                val response = apiService.getVodStreams(username, password, categoryId = "*")
                if (response.isSuccessful) {
                    val movies = response.body() ?: emptyList()
                    contentDao.insertAllMovies(movies)
                    movies
                } else { emptyList() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // Obtiene las series: primero del caché, si no, de la API.
    suspend fun getSeries(username: String, password: String): List<Series> {
        val cachedSeries = contentDao.getAllSeries()
        // CORREGIDO: Usamos la función de extensión 'ifEmpty' para un código más limpio.
        return cachedSeries.ifEmpty {
            try {
                val response = apiService.getSeries(username, password, categoryId = "*")
                if (response.isSuccessful) {
                    val series = response.body() ?: emptyList()
                    contentDao.insertAllSeries(series)
                    series
                } else { emptyList() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
