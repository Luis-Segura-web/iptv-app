package com.kybers.play.ui.tv

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.kybers.play.PlayerActivity
import com.kybers.play.R
import com.kybers.play.adapter.ContentAdapter
import com.kybers.play.api.Category
import com.kybers.play.api.RetrofitClient
import com.kybers.play.api.XtreamApiService
import com.kybers.play.database.AppDatabase
import com.kybers.play.databinding.FragmentTvBinding
import com.kybers.play.repository.ContentRepository
import com.kybers.play.util.NetworkUtils
import kotlinx.coroutines.launch

@Suppress("OPT_IN_ARGUMENT_IS_NOT_MARKER")
@UnstableApi
@OptIn(UnstableApi::class)
class TvFragment : Fragment() {

    private var _binding: FragmentTvBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: ContentRepository
    private lateinit var username: String
    private lateinit var password: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTvBinding.inflate(inflater, container, false)
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

            if (NetworkUtils.isNetworkAvailable(requireContext())) {
                loadCategories(apiService)
            } else {
                Toast.makeText(context, "Sin conexión. Mostrando datos de caché.", Toast.LENGTH_SHORT).show()
                showEmptyState("Sin conexión a internet.")
            }
        }
    }

    private fun loadCategories(apiService: XtreamApiService) {
        lifecycleScope.launch {
            try {
                val response = apiService.getLiveCategories(username, password)
                if (response.isSuccessful) {
                    setupSpinner(response.body())
                } else {
                    Toast.makeText(context, "Error al cargar categorías de TV", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(context, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSpinner(categories: List<Category>?) {
        if (categories.isNullOrEmpty() || context == null) {
            showEmptyState("No hay categorías de TV disponibles.")
            return
        }
        val categoryNames = categories.map { it.categoryName }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategories.adapter = adapter

        binding.spinnerCategories.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadStreamsForCategory(categories[position].categoryId)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun loadStreamsForCategory(categoryId: String) {
        showLoading(true)
        lifecycleScope.launch {
            val streams = repository.getLiveStreams(username, password, categoryId)
            showLoading(false)
            if (streams.isNotEmpty()) {
                binding.recyclerViewContent.visibility = View.VISIBLE
                binding.emptyStateContainer.root.visibility = View.GONE
                binding.recyclerViewContent.layoutManager = LinearLayoutManager(context)
                // CORREGIDO: El parámetro no usado se reemplaza con _
                binding.recyclerViewContent.adapter = ContentAdapter(streams) { _, position ->
                    val intent = Intent(activity, PlayerActivity::class.java).apply {
                        putExtra(PlayerActivity.EXTRA_PLAYLIST_JSON, Gson().toJson(streams))
                        putExtra(PlayerActivity.EXTRA_CURRENT_INDEX, position)
                        putExtra(PlayerActivity.EXTRA_ITEM_TYPE, "live")
                    }
                    startActivity(intent)
                }
            } else {
                if (NetworkUtils.isNetworkAvailable(requireContext())) {
                    showEmptyState("No hay canales en esta categoría.")
                } else {
                    showEmptyState("Sin conexión. No se pudo cargar esta categoría.")
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.shimmerViewContainer.startShimmer()
            binding.shimmerViewContainer.visibility = View.VISIBLE
            binding.recyclerViewContent.visibility = View.GONE
            binding.emptyStateContainer.root.visibility = View.GONE
        } else {
            binding.shimmerViewContainer.stopShimmer()
            binding.shimmerViewContainer.visibility = View.GONE
        }
    }

    private fun showEmptyState(message: String) {
        binding.recyclerViewContent.visibility = View.GONE
        binding.shimmerViewContainer.visibility = View.GONE
        binding.emptyStateContainer.root.visibility = View.VISIBLE
        binding.emptyStateContainer.tvEmptyMessage.text = message

        val icon = if (NetworkUtils.isNetworkAvailable(requireContext())) R.drawable.ic_tv else R.drawable.ic_no_internet
        binding.emptyStateContainer.ivEmptyIcon.setImageResource(icon)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Limpiar la referencia al binding para evitar fugas de memoria
    }
}
