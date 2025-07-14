package com.kybers.play.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.kybers.play.PlayerActivity
import com.kybers.play.R
import com.kybers.play.api.Movie
import com.kybers.play.api.Series
import com.kybers.play.database.AppDatabase
import com.kybers.play.databinding.FragmentHomeBinding
import com.kybers.play.manager.FavoritesManager
import com.kybers.play.manager.HistoryItem
import com.kybers.play.manager.HistoryManager
import com.kybers.play.ui.home.adapter.HomeAdapter
import com.kybers.play.ui.search.SearchActivity
import com.kybers.play.ui.series.SeriesDetailActivity
import com.kybers.play.ui.settings.SettingsActivity
import com.kybers.play.util.NetworkUtils
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeAdapter: HomeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        // Recargamos el contenido cada vez que el fragmento se vuelve visible
        // para asegurar que los datos como el historial estén actualizados.
        loadHomePageContent()
    }

    private fun setupToolbar() {
        binding.btnSearch.setOnClickListener {
            startActivity(Intent(activity, SearchActivity::class.java))
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(activity, SettingsActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        homeAdapter = HomeAdapter(
            onMovieClick = { movie ->
                val intent = Intent(activity, PlayerActivity::class.java).apply {
                    putExtra(PlayerActivity.EXTRA_ITEM_JSON, Gson().toJson(movie))
                    putExtra(PlayerActivity.EXTRA_ITEM_TYPE, "movie")
                }
                startActivity(intent)
            },
            onSeriesClick = { series ->
                val intent = Intent(activity, SeriesDetailActivity::class.java)
                intent.putExtra(SeriesDetailActivity.EXTRA_SERIES_ID, series.seriesId)
                startActivity(intent)
            },
            onHistoryItemClick = { historyItem ->
                val intent = Intent(activity, PlayerActivity::class.java).apply {
                    val contentJson = Gson().toJson(historyItem.content as Any)
                    putExtra(PlayerActivity.EXTRA_ITEM_JSON, contentJson)
                    putExtra(PlayerActivity.EXTRA_ITEM_TYPE, historyItem.content.getType())
                    putExtra(PlayerActivity.EXTRA_START_POSITION, historyItem.lastPosition)
                }
                startActivity(intent)
            }
        )
        binding.recyclerViewHome.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewHome.adapter = homeAdapter
    }

    private fun loadHomePageContent() {
        showLoading(true)
        // Usamos una corutina para cargar los datos en segundo plano.
        viewLifecycleOwner.lifecycleScope.launch {
            val homeCategories = mutableListOf<HomeCategory>()
            val dao = AppDatabase.getDatabase(requireContext()).contentDao()

            // 1. Cargar datos locales (siempre disponibles y rápidos)
            val historyItems = HistoryManager.getHistory(requireContext())
            val favoriteSeries = FavoritesManager.getFavoriteSeries(requireContext())

            if (historyItems.isNotEmpty()) {
                homeCategories.add(HomeCategory("Continuar Viendo", historyItems))
            }
            if (favoriteSeries.isNotEmpty()) {
                homeCategories.add(HomeCategory("Mis Series Favoritas", favoriteSeries))
            }

            // 2. Cargar datos del caché (pueden o no estar disponibles)
            val recentMovies = dao.getAllMovies().take(20)
            if (recentMovies.isNotEmpty()) {
                homeCategories.add(HomeCategory("Películas Populares", recentMovies))
            }

            val recentSeries = dao.getAllSeries().take(20)
            if (recentSeries.isNotEmpty()) {
                homeCategories.add(HomeCategory("Series Populares", recentSeries))
            }

            showLoading(false)

            if (homeCategories.isEmpty()) {
                val message = if (NetworkUtils.isNetworkAvailable(requireContext())) {
                    "No hay contenido disponible. La primera sincronización puede tardar."
                } else {
                    "Sin conexión. Conéctate a internet para cargar el contenido."
                }
                showEmptyState(message)
            } else {
                binding.emptyStateContainer.root.visibility = View.GONE
                binding.recyclerViewHome.visibility = View.VISIBLE
                homeAdapter.submitList(homeCategories)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.shimmerViewContainerHome.startShimmer()
            binding.shimmerViewContainerHome.visibility = View.VISIBLE
            binding.recyclerViewHome.visibility = View.GONE
            binding.emptyStateContainer.root.visibility = View.GONE
        } else {
            binding.shimmerViewContainerHome.stopShimmer()
            binding.shimmerViewContainerHome.visibility = View.GONE
        }
    }

    private fun showEmptyState(message: String) {
        binding.recyclerViewHome.visibility = View.GONE
        binding.shimmerViewContainerHome.visibility = View.GONE
        binding.emptyStateContainer.root.visibility = View.VISIBLE
        binding.emptyStateContainer.tvEmptyMessage.text = message
        binding.emptyStateContainer.ivEmptyIcon.setImageResource(
            if (NetworkUtils.isNetworkAvailable(requireContext())) R.drawable.ic_home else R.drawable.ic_no_internet
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Limpia la referencia al binding para evitar fugas de memoria.
    }
}
