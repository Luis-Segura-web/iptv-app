package com.kybers.play.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.kybers.play.PlayerActivity
import com.kybers.play.R
import com.kybers.play.api.LiveStream
import com.kybers.play.api.Movie
import com.kybers.play.api.PlayableContent
import com.kybers.play.api.Series
import com.kybers.play.database.AppDatabase
import com.kybers.play.databinding.ActivitySearchBinding
import com.kybers.play.ui.series.SeriesDetailActivity
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var searchAdapter: SearchAdapter
    private var allContent = mutableListOf<PlayableContent>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarSearch)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarSearch.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        loadAllContentFromCache()
        setupSearchView()
    }

    @OptIn(UnstableApi::class)
    private fun setupRecyclerView() {
        searchAdapter = SearchAdapter { item ->
            // Decide a qué pantalla navegar según el tipo de contenido.
            when (item) {
                is LiveStream -> {
                    val intent = Intent(this, PlayerActivity::class.java).apply {
                        putExtra(PlayerActivity.EXTRA_ITEM_JSON, Gson().toJson(item))
                        putExtra(PlayerActivity.EXTRA_ITEM_TYPE, "live")
                    }
                    startActivity(intent)
                }
                is Movie -> {
                    val intent = Intent(this, PlayerActivity::class.java).apply {
                        putExtra(PlayerActivity.EXTRA_ITEM_JSON, Gson().toJson(item))
                        putExtra(PlayerActivity.EXTRA_ITEM_TYPE, "movie")
                    }
                    startActivity(intent)
                }
                is Series -> {
                    val intent = Intent(this, SeriesDetailActivity::class.java).apply {
                        putExtra(SeriesDetailActivity.EXTRA_SERIES_ID, item.seriesId)
                    }
                    startActivity(intent)
                }
            }
        }
        binding.recyclerViewSearch.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewSearch.adapter = searchAdapter
    }

    /**
     * Carga todo el contenido desde la base de datos Room a una lista en memoria.
     */
    private fun loadAllContentFromCache() {
        binding.progressBarSearch.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val dao = AppDatabase.getDatabase(this@SearchActivity).contentDao()
                allContent.clear()
                allContent.addAll(dao.getAllLiveStreams())
                allContent.addAll(dao.getAllMovies())
                allContent.addAll(dao.getAllSeries())

                if (allContent.isEmpty()) {
                    showEmptyState("El caché de contenido está vacío. La búsqueda funcionará después de la primera sincronización.")
                }

            } catch (e: Exception) {
                Toast.makeText(this@SearchActivity, "Error al cargar contenido del caché", Toast.LENGTH_LONG).show()
                showEmptyState("Error al cargar el caché.")
            } finally {
                binding.progressBarSearch.visibility = View.GONE
            }
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText ?: ""
                // Solo filtra si el usuario ha escrito al menos 2 caracteres.
                val filteredList = if (query.length < 2) {
                    emptyList()
                } else {
                    allContent.filter {
                        it.getTitle().contains(query, ignoreCase = true)
                    }
                }
                searchAdapter.submitList(filteredList)

                // Muestra un mensaje si la búsqueda no arroja resultados.
                if (filteredList.isEmpty() && query.length >= 2) {
                    showEmptyState("No se encontraron resultados para \"$query\"")
                } else if (query.length < 2) {
                    showEmptyState("Escribe al menos 2 caracteres para buscar.")
                } else {
                    binding.emptyStateSearch.root.visibility = View.GONE
                }
                return true
            }
        })
        // Muestra un mensaje inicial.
        showEmptyState("Busca en todo el catálogo de canales, películas y series.")
    }

    private fun showEmptyState(message: String) {
        binding.recyclerViewSearch.visibility = View.GONE
        binding.emptyStateSearch.root.visibility = View.VISIBLE
        binding.emptyStateSearch.tvEmptyMessage.text = message
        binding.emptyStateSearch.ivEmptyIcon.setImageResource(R.drawable.ic_search)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
