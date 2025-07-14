package com.kybers.play

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kybers.play.worker.SyncWorker
import java.util.concurrent.TimeUnit

/**
 * Clase principal de la aplicación.
 * Se utiliza para inicializar componentes globales, como el WorkManager
 * para la sincronización en segundo plano.
 */
class MainApplication : Application(), Configuration.Provider {

    /**
     * Se llama cuando la aplicación es creada.
     */
    override fun onCreate() {
        super.onCreate()
        Log.d("MainApplication", "La aplicación se ha iniciado. Programando el SyncWorker.")
        setupRecurringWork()
    }

    /**
     * Proporciona una configuración personalizada para WorkManager.
     * Esto es necesario para que WorkManager se inicialice correctamente.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    /**
     * Configura y programa la tarea periódica de sincronización.
     */
    private fun setupRecurringWork() {
        // Define las restricciones para la tarea. En este caso, no hay ninguna,
        // pero podrías añadir que solo se ejecute con WiFi o cuando el dispositivo se está cargando.
        // val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build()

        // Crea una solicitud de trabajo periódico para que se ejecute cada 12 horas.
        val repeatingRequest = PeriodicWorkRequestBuilder<SyncWorker>(12, TimeUnit.HOURS)
            // .setConstraints(constraints) // Descomentar para añadir restricciones.
            .build()

        // Le dice a WorkManager que encole la tarea.
        // 'KEEP' significa que si ya hay una tarea con este nombre programada, no se hará nada.
        // 'REPLACE' la reemplazaría con la nueva.
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "ContentSyncWorker", // Un nombre único para nuestra tarea.
            ExistingPeriodicWorkPolicy.KEEP,
            repeatingRequest
        )

        Log.d("MainApplication", "SyncWorker programado para ejecutarse cada 12 horas.")
    }
}
