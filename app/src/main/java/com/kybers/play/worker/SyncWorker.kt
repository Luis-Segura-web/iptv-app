package com.kybers.play.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kybers.play.api.RetrofitClient
import com.kybers.play.api.XtreamApiService
import com.kybers.play.database.AppDatabase
import com.kybers.play.database.ContentDao

class SyncWorker(appContext: Context, workerParams: WorkerParameters): CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "SyncContentWorker"
    }

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val contentDao = database.contentDao()

        val prefs = applicationContext.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        val serverUrl = prefs.getString("SERVER_URL", null)
        val username = prefs.getString("USERNAME", null)
        val password = prefs.getString("PASSWORD", null)

        if (serverUrl == null || username == null || password == null) {
            return Result.failure() // No se puede sincronizar sin credenciales
        }

        val apiService = RetrofitClient.getClient(serverUrl).create(XtreamApiService::class.java)

        return try {
            // Limpiar caché antiguo
            contentDao.clearAllLiveStreams()
            contentDao.clearAllMovies()
            contentDao.clearAllSeries()

            // Descargar y guardar el nuevo contenido
            // CORREGIDO: Añadimos categoryId = "*" para obtener todo el contenido
            val liveResponse = apiService.getLiveStreams(username, password, categoryId = "*")
            if (liveResponse.isSuccessful) {
                contentDao.insertAllLiveStreams(liveResponse.body() ?: emptyList())
            }

            val movieResponse = apiService.getVodStreams(username, password, categoryId = "*")
            if (movieResponse.isSuccessful) {
                contentDao.insertAllMovies(movieResponse.body() ?: emptyList())
            }

            val seriesResponse = apiService.getSeries(username, password, categoryId = "*")
            if (seriesResponse.isSuccessful) {
                contentDao.insertAllSeries(seriesResponse.body() ?: emptyList())
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry() // Reintentar más tarde si falla la conexión
        }
    }
}
