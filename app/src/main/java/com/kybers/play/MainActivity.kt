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

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            var selectedFragment: Fragment? = null
            when (item.itemId) {
                R.id.nav_home -> selectedFragment = HomeFragment()
                R.id.nav_tv -> selectedFragment = TvFragment()
                R.id.nav_movies -> selectedFragment = MoviesFragment()
                R.id.nav_series -> selectedFragment = SeriesFragment()
            }
            if (selectedFragment != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit()
            }
            true
        }
    }
}
