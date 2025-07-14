package com.kybers.play.ui.series

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.kybers.play.R
import com.kybers.play.adapter.SeriesAdapter
import com.kybers.play.api.RetrofitClient
import com.kybers.play.api.Series
import com.kybers.play.api.XtreamApiService
import com.kybers.play.database.AppDatabase
import com.kybers.play.databinding.FragmentSeriesBinding
import com.kybers.play.repository.ContentRepository
import com.kybers.play.util.NetworkUtils
import kotlinx.coroutines.launch

class SeriesFragment : Fragment() {

    private var _binding: FragmentSeriesBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: ContentRepository
    private lateinit var seriesAdapter: SeriesAdapter
    private var allSeries = listOf<Series>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSeriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = requireActivity().getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        val serverUrl = sharedPreferences.getString("SERVER_URL", "") ?: ""
        val username = sharedPreferences.getString("USERNAME", "") ?: ""
        val password = sharedPreferences.getString("PASSWORD", "") ?: ""

        if (serverUrl.isNotEmpty()) {
            val apiService = RetrofitClient.getClient(serverUrl).create(XtreamApiService::class.java)
            val dao = AppDatabase.getDatabase(requireContext()).contentDao()
            repository = ContentRepository(apiService, dao)

            setupRecyclerView()
            loadAllSeries(username, password)
            setupSearchView()
        }
    }

    private fun setupRecyclerView() {
        seriesAdapter = SeriesAdapter { series ->
            val intent = Intent(activity, SeriesDetailActivity::class.java).apply {
                putExtra(SeriesDetailActivity.EXTRA_SERIES_ID, series.seriesId)
            }
            startActivity(intent)
        }
        binding.recyclerViewSeries.layoutManager = GridLayoutManager(context, 3)
        binding.recyclerViewSeries.adapter = seriesAdapter
    }

    private fun loadAllSeries(username: String, password: String) {
        showLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d("SeriesFragment", "Iniciando carga de series...")
                val series = repository.getSeries(username, password)
                Log.d("SeriesFragment", "Carga finalizada. Se encontraron ${series.size} series.")
                showLoading(false)

                if (series.isNotEmpty()) {
                    binding.recyclerViewSeries.visibility = View.VISIBLE
                    binding.emptyStateContainer.root.visibility = View.GONE
                    allSeries = series
                    seriesAdapter.submitList(allSeries)
                } else {
                    val message = if (NetworkUtils.isNetworkAvailable(requireContext())) {
                        "No se encontraron series en el servidor."
                    } else {
                        "Sin conexión. No se pudo cargar la lista de series."
                    }
                    showEmptyState(message)
                }
            } catch (e: Exception) {
                showLoading(false)
                showEmptyState("Error al cargar series: ${e.message}")
                Log.e("SeriesFragment", "Excepción al cargar series", e)
            }
        }
    }

    private fun setupSearchView() {
        binding.searchViewSeries.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val filteredList = allSeries.filter { it.name.contains(newText ?: "", ignoreCase = true) }
                seriesAdapter.submitList(filteredList)
                return true
            }
        })
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.shimmerViewContainerSeries.startShimmer()
            binding.shimmerViewContainerSeries.visibility = View.VISIBLE
            binding.recyclerViewSeries.visibility = View.GONE
            binding.emptyStateContainer.root.visibility = View.GONE
        } else {
            binding.shimmerViewContainerSeries.stopShimmer()
            binding.shimmerViewContainerSeries.visibility = View.GONE
        }
    }

    private fun showEmptyState(message: String) {
        binding.recyclerViewSeries.visibility = View.GONE
        binding.shimmerViewContainerSeries.visibility = View.GONE
        binding.emptyStateContainer.root.visibility = View.VISIBLE
        binding.emptyStateContainer.tvEmptyMessage.text = message
        binding.emptyStateContainer.ivEmptyIcon.setImageResource(
            if (NetworkUtils.isNetworkAvailable(requireContext())) R.drawable.ic_series else R.drawable.ic_no_internet
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
