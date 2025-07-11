package com.kybers.play

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kybers.play.worker.SyncWorker
import java.util.concurrent.TimeUnit

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setupRecurringWork()
    }

    private fun setupRecurringWork() {
        // Creamos la solicitud de trabajo periódico para que se ejecute cada 4 horas
        val repeatingRequest = PeriodicWorkRequestBuilder<SyncWorker>(4, TimeUnit.HOURS)
            .build()

        // Programamos el trabajo, asegurándonos de que no se duplique
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Mantiene el trabajo existente si ya está programado
            repeatingRequest
        )
    }
}
