package com.kybers.play

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

/**
 * Muestra una pantalla de bienvenida y redirige al usuario a la pantalla
 * de Login o a la Principal dependiendo de si ya ha iniciado sesión.
 */
@SuppressLint("CustomSplashScreen") // Lo hacemos personalizado para tener más control.
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Usamos un Handler para esperar 2 segundos (2000 milisegundos)
        // antes de decidir a dónde ir.
        Handler(Looper.getMainLooper()).postDelayed({
            // Accedemos a las credenciales guardadas
            val sharedPreferences = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
            val serverUrl = sharedPreferences.getString("SERVER_URL", null)

            // Si hay una URL guardada, asumimos que el usuario ya inició sesión.
            if (serverUrl != null) {
                // Ir a la pantalla principal
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                // Ir a la pantalla de login
                startActivity(Intent(this, LoginActivity::class.java))
            }
            // Cierra la SplashActivity para que el usuario no pueda volver a ella
            // presionando el botón de "atrás".
            finish()
        }, 2000)
    }
}
