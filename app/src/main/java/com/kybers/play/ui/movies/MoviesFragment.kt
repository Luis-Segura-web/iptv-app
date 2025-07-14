package com.kybers.play.ui.movies

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import com.kybers.play.PlayerActivity
import com.kybers.play.R
import com.kybers.play.adapter.CategoryWithMovies
import com.kybers.play.adapter.MovieCategoryAdapter
import com.kybers.play.api.Movie
import com.kybers.play.api.RetrofitClient
import com.kybers.play.api.XtreamApiService
import com.kybers.play.databinding.FragmentMoviesBinding
import com.kybers.play.util.NetworkUtils
import com.kybers.play.util.StickyHeaderDecoration
import kotlinx.coroutines.launch

class MoviesFragment : Fragment() {

    private var _binding: FragmentMoviesBinding? = null
    private val binding get() = _binding!!

    private lateinit var apiService: XtreamApiService
    private lateinit var movieCategoryAdapter: MovieCategoryAdapter
    private lateinit var username: String
    private lateinit var password: String

    // Lista maestra que mantiene el estado de todas las categorías.
    private val masterCategoryList = mutableListOf<CategoryWithMovies>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMoviesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = requireActivity().getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        val serverUrl = sharedPreferences.getString("SERVER_URL", "") ?: ""
        username = sharedPreferences.getString("USERNAME", "") ?: ""
        password = sharedPreferences.getString("PASSWORD", "") ?: ""

        if (serverUrl.isNotEmpty()) {
            apiService = RetrofitClient.getClient(serverUrl).create(XtreamApiService::class.java)
            setupRecyclerView()
            loadInitialCategories()
            setupSearchView()
        }
    }

    @OptIn(UnstableApi::class)
    private fun setupRecyclerView() {
        movieCategoryAdapter = MovieCategoryAdapter(
            onMovieClick = { movie ->
                val intent = Intent(activity, PlayerActivity::class.java).apply {
                    putExtra(PlayerActivity.EXTRA_ITEM_JSON, Gson().toJson(movie))
                    putExtra(PlayerActivity.EXTRA_ITEM_TYPE, "movie")
                }
                startActivity(intent)
            },
            // Le pasamos la función que se debe ejecutar cuando se expande una categoría por primera vez.
            onLoadMoviesForCategory = { categoryItem, position ->
                loadMoviesForCategory(categoryItem, position)
            }
        )

        val gridLayoutManager = GridLayoutManager(context, 3)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (movieCategoryAdapter.getItemViewType(position)) {
                    0 -> 3 // TYPE_CATEGORY
                    1 -> 1 // TYPE_MOVIE
                    else -> 1
                }
            }
        }

        binding.recyclerViewMovies.apply {
            layoutManager = gridLayoutManager
            adapter = movieCategoryAdapter
            addItemDecoration(StickyHeaderDecoration(movieCategoryAdapter, this))
        }
    }

    /**
     * Carga únicamente la lista de categorías.
     */
    private fun loadInitialCategories() {
        showLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                showEmptyState("Sin conexión a internet.")
                showLoading(false)
                return@launch
            }
            try {
                Log.d("MoviesFragment", "Obteniendo categorías de películas...")
                val categoriesResponse = apiService.getVodCategories(username, password)
                if (categoriesResponse.isSuccessful) {
                    val categories = categoriesResponse.body() ?: emptyList()
                    if (categories.isNotEmpty()) {
                        // Creamos la lista maestra, pero sin las películas (movies = null).
                        masterCategoryList.clear()
                        masterCategoryList.addAll(categories.map { CategoryWithMovies(it) })
                        movieCategoryAdapter.submitList(masterCategoryList)

                        binding.recyclerViewMovies.visibility = View.VISIBLE
                        binding.emptyStateContainer.root.visibility = View.GONE
                    } else {
                        showEmptyState("No se encontraron categorías de películas.")
                    }
                } else {
                    showEmptyState("Error al cargar categorías: ${categoriesResponse.code()}")
                }
            } catch (e: Exception) {
                showEmptyState("Error de conexión: ${e.message}")
                Log.e("MoviesFragment", "Excepción al cargar categorías de películas", e)
            } finally {
                showLoading(false)
            }
        }
    }

    /**
     * Se llama desde el adaptador para cargar las películas de una categoría específica.
     */
    private fun loadMoviesForCategory(categoryItem: CategoryWithMovies, position: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val moviesResponse = apiService.getVodStreams(username, password, categoryId = categoryItem.category.categoryId)
                if (moviesResponse.isSuccessful) {
                    val movies = moviesResponse.body() ?: emptyList()
                    // Actualizamos el item en nuestra lista maestra con las películas cargadas.
                    val masterItem = masterCategoryList.find { it.category.categoryId == categoryItem.category.categoryId }
                    masterItem?.let {
                        it.movies = movies
                        it.isLoading = false
                    }
                    // Notificamos al adaptador que los datos han cambiado.
                    movieCategoryAdapter.onCategoryLoaded(position, movies)
                } else {
                    Log.e("MoviesFragment", "Error al cargar películas para cat ${categoryItem.category.categoryId}")
                    categoryItem.isLoading = false
                    movieCategoryAdapter.notifyItemChanged(position)
                }
            } catch (e: Exception) {
                Log.e("MoviesFragment", "Excepción al cargar películas para cat ${categoryItem.category.categoryId}", e)
                categoryItem.isLoading = false
                movieCategoryAdapter.notifyItemChanged(position)
            }
        }
    }

    private fun setupSearchView() {
        binding.searchViewMovies.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                movieCategoryAdapter.filter(newText, masterCategoryList)
                return true
            }
        })
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBarMovies.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            binding.recyclerViewMovies.visibility = View.GONE
            binding.emptyStateContainer.root.visibility = View.GONE
        }
    }

    private fun showEmptyState(message: String) {
        binding.recyclerViewMovies.visibility = View.GONE
        binding.emptyStateContainer.root.visibility = View.VISIBLE
        binding.emptyStateContainer.tvEmptyMessage.text = message
        binding.emptyStateContainer.ivEmptyIcon.setImageResource(R.drawable.ic_movie)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
