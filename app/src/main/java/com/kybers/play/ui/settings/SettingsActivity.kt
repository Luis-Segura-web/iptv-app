package com.kybers.play.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.kybers.play.LoginActivity
import com.kybers.play.database.AppDatabase
import com.kybers.play.databinding.ActivitySettingsBinding
import com.kybers.play.manager.FavoritesManager
import com.kybers.play.manager.HistoryManager
import com.kybers.play.manager.LiveFavoritesManager
import com.kybers.play.manager.SettingsManager
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura la barra de herramientas con el botón de "atrás".
        setSupportActionBar(binding.toolbarSettings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarSettings.setNavigationOnClickListener { finish() }

        setupListeners()
        loadSettings()
    }

    /**
     * Configura todos los listeners para los elementos de la UI.
     */
    private fun setupListeners() {
        // Listener para el switch de auto-reproducción.
        binding.switchAutoplay.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.setAutoplayNextEpisode(this, isChecked)
        }

        // Listener para el botón de User-Agent.
        binding.btnUserAgent.setOnClickListener {
            showUserAgentDialog()
        }

        // Listener para el botón de limpiar caché.
        binding.btnClearCache.setOnClickListener {
            showClearCacheConfirmation()
        }

        // Listener para el botón de cerrar sesión.
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    /**
     * Carga el estado actual de los ajustes y lo refleja en la UI.
     */
    private fun loadSettings() {
        binding.switchAutoplay.isChecked = SettingsManager.isAutoplayNextEpisodeEnabled(this)
    }

    /**
     * Muestra un diálogo para que el usuario edite el User-Agent.
     */
    private fun showUserAgentDialog() {
        val editText = EditText(this).apply {
            setText(SettingsManager.getUserAgent(this@SettingsActivity))
        }

        AlertDialog.Builder(this)
            .setTitle("User-Agent")
            .setMessage("Cambiar esto puede ayudar a solucionar problemas de reproducción con algunos proveedores.")
            .setView(editText)
            .setPositiveButton("Guardar") { dialog, _ ->
                val newUserAgent = editText.text.toString()
                if (newUserAgent.isNotBlank()) {
                    SettingsManager.setUserAgent(this, newUserAgent)
                    Toast.makeText(this, "User-Agent guardado", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Muestra un diálogo de confirmación antes de limpiar el caché.
     */
    private fun showClearCacheConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Limpiar Caché")
            .setMessage("¿Estás seguro de que quieres borrar todos los datos de canales, películas y series guardados? La aplicación los descargará de nuevo la próxima vez que la uses.")
            .setPositiveButton("Limpiar") { dialog, _ ->
                clearCache()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Ejecuta la limpieza de la base de datos en una corutina.
     */
    private fun clearCache() {
        lifecycleScope.launch {
            val dao = AppDatabase.getDatabase(this@SettingsActivity).contentDao()
            dao.clearAllLiveStreams()
            dao.clearAllMovies()
            dao.clearAllSeries()
            Toast.makeText(this@SettingsActivity, "Caché limpiado", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Muestra un diálogo de confirmación antes de cerrar la sesión.
     */
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar la sesión? Se borrarán tus credenciales, favoritos e historial.")
            .setPositiveButton("Sí, cerrar sesión") { dialog, _ ->
                logout()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Cierra la sesión del usuario, borrando todos los datos guardados y
     * redirigiendo a la pantalla de Login.
     */
    private fun logout() {
        // Borra las credenciales
        val sharedPreferences = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        sharedPreferences.edit {
            clear()
        }

        // Borra los managers de datos del usuario
        // (Esto se hace borrando sus respectivos SharedPreferences)
        getSharedPreferences("IPTV_FAVORITES", Context.MODE_PRIVATE).edit().clear().apply()
        getSharedPreferences("IPTV_LIVE_FAVORITES", Context.MODE_PRIVATE).edit().clear().apply()
        getSharedPreferences("IPTV_HISTORY", Context.MODE_PRIVATE).edit().clear().apply()
        getSharedPreferences("IPTV_SETTINGS", Context.MODE_PRIVATE).edit().clear().apply()

        // Borra el caché de la base de datos
        lifecycleScope.launch {
            AppDatabase.getDatabase(this@SettingsActivity).contentDao().apply {
                clearAllLiveStreams()
                clearAllMovies()
                clearAllSeries()
            }
        }

        // Redirige al Login y limpia la pila de actividades
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
