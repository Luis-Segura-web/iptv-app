package com.kybers.play.repository

import android.util.Log
import com.kybers.play.api.LiveStream
import com.kybers.play.api.Movie
import com.kybers.play.api.Series
import com.kybers.play.api.XtreamApiService
import com.kybers.play.database.ContentDao
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Repositorio para gestionar el contenido. Actúa como una única fuente de verdad para los datos.
 * Decide si obtener los datos del caché local (Room) o de la red (API).
 */
class ContentRepository(private val apiService: XtreamApiService, private val contentDao: ContentDao) {

    suspend fun getLiveStreams(username: String, password: String, categoryId: String): List<LiveStream> {
        val cachedStreams = contentDao.getLiveStreamsByCategory(categoryId)
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

    suspend fun getMovies(username: String, password: String): List<Movie> {
        val cachedMovies = contentDao.getAllMovies()
        if (cachedMovies.isNotEmpty()) {
            return cachedMovies
        }

        return try {
            Log.d("ContentRepository", "Caché de películas vacío. Obteniendo categorías de VOD...")
            val categoriesResponse = apiService.getVodCategories(username, password)
            if (categoriesResponse.isSuccessful) {
                // CORREGIDO: Limitamos la carga a las primeras 10 categorías para mejorar el rendimiento.
                val categories = categoriesResponse.body()?.take(10)
                if (categories.isNullOrEmpty()) {
                    Log.d("ContentRepository", "No se encontraron categorías de VOD.")
                    return emptyList()
                }

                Log.d("ContentRepository", "Se encontraron ${categories.size} categorías de VOD. Obteniendo películas...")
                coroutineScope {
                    val movieJobs = categories.map { category ->
                        async {
                            try {
                                val response = apiService.getVodStreams(username, password, categoryId = category.categoryId)
                                if (response.isSuccessful) {
                                    response.body() ?: emptyList()
                                } else {
                                    Log.e("ContentRepository", "Error al obtener películas para categoría ${category.categoryId}: ${response.code()}")
                                    emptyList()
                                }
                            } catch (e: Exception) {
                                Log.e("ContentRepository", "Excepción al obtener películas para categoría ${category.categoryId}", e)
                                emptyList()
                            }
                        }
                    }
                    val allMovies = movieJobs.awaitAll().flatten()
                    Log.d("ContentRepository", "Carga finalizada. Se obtuvieron un total de ${allMovies.size} películas.")
                    if (allMovies.isNotEmpty()) {
                        contentDao.insertAllMovies(allMovies)
                    }
                    allMovies
                }
            } else {
                Log.e("ContentRepository", "Error al obtener categorías de VOD: ${categoriesResponse.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("ContentRepository", "Excepción al obtener películas", e)
            emptyList()
        }
    }

    suspend fun getSeries(username: String, password: String): List<Series> {
        val cachedSeries = contentDao.getAllSeries()
        if (cachedSeries.isNotEmpty()) {
            return cachedSeries
        }

        return try {
            Log.d("ContentRepository", "Caché de series vacío. Obteniendo categorías de Series...")
            val categoriesResponse = apiService.getSeriesCategories(username, password)
            if (categoriesResponse.isSuccessful) {
                // CORREGIDO: Limitamos la carga a las primeras 10 categorías.
                val categories = categoriesResponse.body()?.take(10)
                if (categories.isNullOrEmpty()) {
                    Log.d("ContentRepository", "No se encontraron categorías de Series.")
                    return emptyList()
                }

                Log.d("ContentRepository", "Se encontraron ${categories.size} categorías de Series. Obteniendo series...")
                coroutineScope {
                    val seriesJobs = categories.map { category ->
                        async {
                            try {
                                val response = apiService.getSeries(username, password, categoryId = category.categoryId)
                                if (response.isSuccessful) {
                                    response.body() ?: emptyList()
                                } else {
                                    Log.e("ContentRepository", "Error al obtener series para categoría ${category.categoryId}: ${response.code()}")
                                    emptyList()
                                }
                            } catch (e: Exception) {
                                Log.e("ContentRepository", "Excepción al obtener series para categoría ${category.categoryId}", e)
                                emptyList()
                            }
                        }
                    }
                    val allSeries = seriesJobs.awaitAll().flatten()
                    Log.d("ContentRepository", "Carga finalizada. Se obtuvieron un total de ${allSeries.size} series.")
                    if (allSeries.isNotEmpty()) {
                        contentDao.insertAllSeries(allSeries)
                    }
                    allSeries
                }
            } else {
                Log.e("ContentRepository", "Error al obtener categorías de Series: ${categoriesResponse.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("ContentRepository", "Excepción al obtener series", e)
            emptyList()
        }
    }
}
