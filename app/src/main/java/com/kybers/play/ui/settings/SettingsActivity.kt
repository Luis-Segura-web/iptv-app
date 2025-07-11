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
import com.kybers.play.manager.SettingsManager
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarSettings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarSettings.setNavigationOnClickListener { finish() }

        setupListeners()
        loadSettings()
    }

    private fun setupListeners() {
        binding.switchAutoplay.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.setAutoplayNextEpisode(this, isChecked)
        }

        binding.btnUserAgent.setOnClickListener {
            showUserAgentDialog()
        }

        binding.btnClearCache.setOnClickListener {
            showClearCacheConfirmation()
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun loadSettings() {
        binding.switchAutoplay.isChecked = SettingsManager.isAutoplayNextEpisodeEnabled(this)
    }

    private fun showUserAgentDialog() {
        val editText = EditText(this)
        editText.setText(SettingsManager.getUserAgent(this))

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

    private fun clearCache() {
        lifecycleScope.launch {
            val dao = AppDatabase.getDatabase(this@SettingsActivity).contentDao()
            dao.clearAllLiveStreams()
            dao.clearAllMovies()
            dao.clearAllSeries()
            Toast.makeText(this@SettingsActivity, "Caché limpiado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar la sesión?")
            .setPositiveButton("Sí, cerrar sesión") { dialog, _ ->
                logout()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun logout() {
        val sharedPreferences = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        // CORREGIDO: Usamos la función de extensión KTX
        sharedPreferences.edit {
            clear()
        }
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
