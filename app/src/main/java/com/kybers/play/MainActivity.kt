package com.kybers.play

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.kybers.play.databinding.ActivityMainBinding
import com.kybers.play.ui.home.HomeFragment
import com.kybers.play.ui.movies.MoviesFragment
import com.kybers.play.ui.series.SeriesFragment
import com.kybers.play.ui.tv.TvFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Si es la primera vez que se crea la actividad, carga el HomeFragment por defecto.
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }

        // Listener para la barra de navegación.
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            var selectedFragment: Fragment? = null
            // Decide qué fragmento mostrar según el item presionado.
            when (item.itemId) {
                R.id.nav_home -> selectedFragment = HomeFragment()
                R.id.nav_tv -> selectedFragment = TvFragment()
                R.id.nav_movies -> selectedFragment = MoviesFragment()
                R.id.nav_series -> selectedFragment = SeriesFragment()
            }
            // Si se seleccionó un fragmento válido, lo reemplaza en el contenedor.
            if (selectedFragment != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit()
            }
            true
        }
    }
}
