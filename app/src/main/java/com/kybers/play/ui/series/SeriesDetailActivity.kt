package com.kybers.play.ui.series

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.kybers.play.PlayerActivity
import com.kybers.play.R
import com.kybers.play.adapter.EpisodeAdapter
import com.kybers.play.api.Episode
import com.kybers.play.api.RetrofitClient
import com.kybers.play.api.Season
import com.kybers.play.api.SeriesInfoResponse
import com.kybers.play.api.XtreamApiService
import com.kybers.play.databinding.ActivitySeriesDetailBinding
import com.kybers.play.manager.FavoritesManager
import kotlinx.coroutines.launch

class SeriesDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySeriesDetailBinding
    private lateinit var apiService: XtreamApiService
    private var seriesId: Int = -1
    private var seriesInfo: SeriesInfoResponse? = null

    private lateinit var serverUrl: String
    private lateinit var username: String
    private lateinit var password: String

    private var isFavorite = false
    private var currentEpisodes: List<Episode> = emptyList()
    private var episodeAdapter: EpisodeAdapter? = null

    companion object {
        const val EXTRA_SERIES_ID = "extra_series_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeriesDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        seriesId = intent.getIntExtra(EXTRA_SERIES_ID, -1)
        if (seriesId == -1) {
            finish()
            return
        }

        val sharedPreferences = getSharedPreferences("IPTV_PREFS", MODE_PRIVATE)
        serverUrl = sharedPreferences.getString("SERVER_URL", "") ?: ""
        username = sharedPreferences.getString("USERNAME", "") ?: ""
        password = sharedPreferences.getString("PASSWORD", "") ?: ""

        apiService = RetrofitClient.getClient(serverUrl).create(XtreamApiService::class.java)

        setupFavoriteButton()
        loadSeriesInfo()
    }

    private fun setupFavoriteButton() {
        isFavorite = FavoritesManager.isFavorite(this, seriesId)
        updateFabIcon()

        binding.fabFavorite.setOnClickListener {
            seriesInfo?.info?.let { series ->
                if (isFavorite) {
                    FavoritesManager.removeFavoriteSeries(this, series)
                    Toast.makeText(this, "Eliminado de favoritos", Toast.LENGTH_SHORT).show()
                } else {
                    FavoritesManager.addFavoriteSeries(this, series)
                    Toast.makeText(this, "Añadido a favoritos", Toast.LENGTH_SHORT).show()
                }
                isFavorite = !isFavorite
                updateFabIcon()
            }
        }
    }

    private fun updateFabIcon() {
        if (isFavorite) {
            binding.fabFavorite.setImageResource(R.drawable.ic_favorite)
        } else {
            binding.fabFavorite.setImageResource(R.drawable.ic_favorite_border)
        }
    }

    private fun loadSeriesInfo() {
        lifecycleScope.launch {
            try {
                val response = apiService.getSeriesInfo(username, password, seriesId = seriesId)
                if (response.isSuccessful) {
                    seriesInfo = response.body()
                    updateUI()
                } else {
                    Toast.makeText(this@SeriesDetailActivity, "Error al cargar detalles: ${response.message()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SeriesDetailActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateUI() {
        seriesInfo?.let { info ->
            binding.collapsingToolbar.title = info.info.name
            binding.tvSeriesPlot.text = info.info.plot
            Glide.with(this).load(info.info.cover).into(binding.ivSeriesCover)

            setupSeasonSpinner(info.seasons)
        }
    }

    private fun setupSeasonSpinner(seasons: List<Season>) {
        val seasonNames = seasons.map { "Temporada ${it.seasonNumber}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, seasonNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSeasons.adapter = adapter

        binding.spinnerSeasons.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedSeasonNumber = seasons[position].seasonNumber.toString()
                val episodes = seriesInfo?.episodes?.get(selectedSeasonNumber) ?: emptyList()
                setupEpisodeList(episodes)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    @OptIn(UnstableApi::class)
    private fun setupEpisodeList(episodes: List<Episode>) {
        currentEpisodes = episodes
        episodeAdapter = EpisodeAdapter(episodes) { episode, position ->
            episodeAdapter?.setNowPlaying(episode.id)

            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_PLAYLIST_JSON, Gson().toJson(currentEpisodes))
                putExtra(PlayerActivity.EXTRA_CURRENT_INDEX, position)
                putExtra(PlayerActivity.EXTRA_ITEM_TYPE, "series")
                putExtra(PlayerActivity.EXTRA_SERIES_TITLE, seriesInfo?.info?.name)
            }
            startActivity(intent)
        }
        binding.recyclerViewEpisodes.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewEpisodes.adapter = episodeAdapter
    }
}
