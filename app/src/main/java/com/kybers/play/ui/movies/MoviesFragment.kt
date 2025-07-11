package com.kybers.play.ui.movies

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import com.kybers.play.adapter.MoviesAdapter
import com.kybers.play.api.Movie
import com.kybers.play.api.RetrofitClient
import com.kybers.play.api.XtreamApiService
import com.kybers.play.database.AppDatabase
import com.kybers.play.databinding.FragmentMoviesBinding
import com.kybers.play.repository.ContentRepository
import com.kybers.play.util.NetworkUtils
import kotlinx.coroutines.launch

class MoviesFragment : Fragment() {

    private var _binding: FragmentMoviesBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: ContentRepository
    private lateinit var username: String
    private lateinit var password: String

    private var allMovies = listOf<Movie>()
    private lateinit var moviesAdapter: MoviesAdapter

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
            val apiService = RetrofitClient.getClient(serverUrl).create(XtreamApiService::class.java)
            val dao = AppDatabase.getDatabase(requireContext()).contentDao()
            repository = ContentRepository(apiService, dao)

            setupRecyclerView()
            loadMovies()
            setupSearchView()
        }
    }

    @OptIn(UnstableApi::class)
    private fun setupRecyclerView() {
        moviesAdapter = MoviesAdapter(mutableListOf()) { movie ->
            val intent = Intent(activity, PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_ITEM_JSON, Gson().toJson(movie))
                putExtra(PlayerActivity.EXTRA_ITEM_TYPE, "movie")
            }
            startActivity(intent)
        }
        binding.recyclerViewMovies.layoutManager = GridLayoutManager(context, 3)
        binding.recyclerViewMovies.adapter = moviesAdapter
    }

    private fun loadMovies() {
        binding.spinnerMovieCategories.visibility = View.GONE
        showLoading(true)
        lifecycleScope.launch {
            try {
                val movies = repository.getMovies(username, password)
                showLoading(false)
                if (movies.isNotEmpty()) {
                    binding.recyclerViewMovies.visibility = View.VISIBLE
                    binding.emptyStateContainer.root.visibility = View.GONE
                    allMovies = movies
                    moviesAdapter.updateMovies(allMovies)
                } else {
                    if (NetworkUtils.isNetworkAvailable(requireContext())) {
                        showEmptyState("No se encontraron películas.")
                    } else {
                        showEmptyState("Sin conexión. No se pudo cargar la lista de películas.")
                    }
                }
            } catch (_: Exception) {
                showLoading(false)
                showEmptyState("Error al cargar películas.")
            }
        }
    }

    private fun setupSearchView() {
        binding.searchViewMovies.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                val filteredList = allMovies.filter {
                    it.name.contains(newText ?: "", ignoreCase = true)
                }
                moviesAdapter.updateMovies(filteredList)
                return true
            }
        })
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.shimmerViewContainerMovies.startShimmer()
            binding.shimmerViewContainerMovies.visibility = View.VISIBLE
            binding.recyclerViewMovies.visibility = View.GONE
            binding.emptyStateContainer.root.visibility = View.GONE
        } else {
            binding.shimmerViewContainerMovies.stopShimmer()
            binding.shimmerViewContainerMovies.visibility = View.GONE
        }
    }

    private fun showEmptyState(message: String) {
        binding.recyclerViewMovies.visibility = View.GONE
        binding.shimmerViewContainerMovies.visibility = View.GONE
        binding.emptyStateContainer.root.visibility = View.VISIBLE
        binding.emptyStateContainer.tvEmptyMessage.text = message

        val icon = if (NetworkUtils.isNetworkAvailable(requireContext())) R.drawable.ic_movie else R.drawable.ic_no_internet
        binding.emptyStateContainer.ivEmptyIcon.setImageResource(icon)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
