package com.kybers.play.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kybers.play.api.RetrofitClient
import com.kybers.play.api.XtreamApiService
import com.kybers.play.database.AppDatabase
import com.kybers.play.manager.DataCacheManager
import com.kybers.play.repository.ContentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Un CoroutineWorker que se encarga de sincronizar todos los datos
 * del servidor en segundo plano.
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    /**
     * Esta es la función que se ejecuta en segundo plano.
     */
    override suspend fun doWork(): Result {
        Log.d("SyncWorker", "Iniciando trabajo de sincronización en segundo plano.")
        val sharedPreferences = applicationContext.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        val serverUrl = sharedPreferences.getString("SERVER_URL", null)
        val username = sharedPreferences.getString("USERNAME", null)
        val password = sharedPreferences.getString("PASSWORD", null)

        // Si no hay credenciales, no podemos hacer nada.
        if (serverUrl.isNullOrBlank() || username.isNullOrBlank() || password.isNullOrBlank()) {
            Log.e("SyncWorker", "No se encontraron credenciales, no se puede sincronizar.")
            return Result.failure()
        }

        return withContext(Dispatchers.IO) {
            try {
                // Preparamos todas las herramientas que necesitamos.
                val apiService = RetrofitClient.getClient(serverUrl).create(XtreamApiService::class.java)
                val dao = AppDatabase.getDatabase(applicationContext).contentDao()
                val repository = ContentRepository(apiService, dao)

                // 1. Limpiamos el caché antiguo para asegurar datos frescos.
                Log.d("SyncWorker", "Limpiando caché antiguo...")
                dao.clearAllLiveStreams()
                dao.clearAllMovies()
                dao.clearAllSeries()

                // 2. Descargamos y guardamos todo de nuevo.
                Log.d("SyncWorker", "Sincronizando TV en vivo...")
                val liveCategories = repository.getLiveStreams(username, password, "*") // Usamos '*' para intentar obtener todo.

                Log.d("SyncWorker", "Sincronizando películas...")
                val movies = repository.getMovies(username, password)

                Log.d("SyncWorker", "Sincronizando series...")
                val series = repository.getSeries(username, password)

                // Actualizamos la marca de tiempo del caché de TV.
                DataCacheManager.updateTvSyncTimestamp(applicationContext)

                Log.d("SyncWorker", "Sincronización completada exitosamente.")
                Result.success()

            } catch (e: Exception) {
                Log.e("SyncWorker", "La sincronización en segundo plano falló", e)
                Result.failure()
            }
        }
    }
}
