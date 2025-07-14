package com.kybers.play

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.kybers.play.api.RetrofitClient
import com.kybers.play.api.XtreamApiService
import com.kybers.play.databinding.ActivityLoginBinding
import com.kybers.play.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val serverUrl = binding.etServerUrl.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Validación de campos
            if (serverUrl.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validación de conexión a internet
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "No hay conexión a internet", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Muestra el progreso y deshabilita el botón para evitar múltiples clics
            binding.progressBar.visibility = View.VISIBLE
            binding.btnLogin.isEnabled = false

            // Usamos una corutina para hacer la llamada de red en un hilo de fondo
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val apiService = RetrofitClient.getClient(serverUrl).create(XtreamApiService::class.java)
                    val response = apiService.authenticate(username, password)

                    // Volvemos al hilo principal para actualizar la UI
                    withContext(Dispatchers.Main) {
                        // La API de Xtream devuelve auth=1 si el login es correcto
                        if (response.isSuccessful && response.body()?.userInfo?.auth == 1) {
                            // Guardamos las credenciales para futuras sesiones
                            val sharedPreferences = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
                            sharedPreferences.edit {
                                putString("SERVER_URL", serverUrl)
                                putString("USERNAME", username)
                                putString("PASSWORD", password)
                            }

                            // Navegamos a la pantalla principal
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish() // Cerramos LoginActivity para que el usuario no pueda volver
                        } else {
                            Toast.makeText(this@LoginActivity, "Credenciales incorrectas o error del servidor", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoginActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } finally {
                    // Nos aseguramos de que la UI vuelva a su estado normal
                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                        binding.btnLogin.isEnabled = true
                    }
                }
            }
        }
    }
}
