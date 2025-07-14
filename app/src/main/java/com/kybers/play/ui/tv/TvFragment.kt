package com.kybers.play.ui.tv

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.BehindLiveWindowException
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.kybers.play.PlayerActivity
import com.kybers.play.R
import com.kybers.play.adapter.CategoryWithChannels
import com.kybers.play.adapter.TvCategoryAdapter
import com.kybers.play.api.Category
import com.kybers.play.api.LiveStream
import com.kybers.play.api.RetrofitClient
import com.kybers.play.api.XtreamApiService
import com.kybers.play.databinding.FragmentTvBinding
import com.kybers.play.manager.DataCacheManager
import com.kybers.play.manager.LiveFavoritesManager
import com.kybers.play.ui.settings.SettingsActivity
import com.kybers.play.util.NetworkUtils
import com.kybers.play.util.StickyHeaderDecoration
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@UnstableApi
class TvFragment : Fragment() {

    private var _binding: FragmentTvBinding? = null
    private val binding get() = _binding!!

    private var player: ExoPlayer? = null
    private lateinit var tvCategoryAdapter: TvCategoryAdapter
    private lateinit var apiService: XtreamApiService
    private lateinit var username: String
    private lateinit var password: String
    private lateinit var serverUrl: String

    private var currentPlayingStream: LiveStream? = null

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
        serverUrl = sharedPreferences.getString("SERVER_URL", "") ?: ""
        username = sharedPreferences.getString("USERNAME", "") ?: ""
        password = sharedPreferences.getString("PASSWORD", "") ?: ""

        if (serverUrl.isBlank()) {
            Toast.makeText(context, "Error: Faltan las credenciales.", Toast.LENGTH_LONG).show()
            return
        }

        apiService = RetrofitClient.getClient(serverUrl).create(XtreamApiService::class.java)

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        binding.emptyStateLayout.btnRetry.setOnClickListener {
            loadDataFromServer(isSilent = false)
        }
    }

    private fun setupToolbar() {
        binding.btnSettingsTv.setOnClickListener {
            startActivity(Intent(activity, SettingsActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        tvCategoryAdapter = TvCategoryAdapter(
            context = requireContext(),
            onChannelClick = { channel -> playStream(channel) },
            onCategoryToggled = { position ->
                binding.appBarLayout.setExpanded(true, true)
                (binding.recyclerViewTv.layoutManager as LinearLayoutManager)
                    .scrollToPositionWithOffset(position, 0)
            }
        )
        binding.recyclerViewTv.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = tvCategoryAdapter
            addItemDecoration(StickyHeaderDecoration(tvCategoryAdapter, this))
        }
    }

    private fun setupSearch() {
        binding.searchViewTv.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                tvCategoryAdapter.filter(newText)
                return true
            }
        })
    }

    private fun initializePlayer() {
        if (player == null && context != null) {
            player = ExoPlayer.Builder(requireContext()).build().also { exoPlayer ->
                binding.playerViewTv.player = exoPlayer
                exoPlayer.playWhenReady = true
                exoPlayer.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        if (error.cause is BehindLiveWindowException) {
                            exoPlayer.seekToDefaultPosition()
                            exoPlayer.prepare()
                        } else {
                            Toast.makeText(context, "Error de reproducción", Toast.LENGTH_SHORT).show()
                            Log.e("TvFragmentPlayer", "Error: ${error.message}")
                        }
                    }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        binding.playerBuffering.visibility = if (playbackState == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
                    }
                })
                binding.playerClickOverlay.setOnClickListener {
                    openPlayerActivity()
                }
            }
        }
    }

    private fun openPlayerActivity() {
        currentPlayingStream?.let { stream ->
            val intent = Intent(activity, PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_ITEM_JSON, Gson().toJson(stream))
                putExtra(PlayerActivity.EXTRA_ITEM_TYPE, "live")
                putExtra(PlayerActivity.EXTRA_START_POSITION, player?.currentPosition ?: 0)
            }
            startActivity(intent)
        }
    }

    private fun playStream(stream: LiveStream) {
        binding.playerContainer.visibility = View.VISIBLE
        currentPlayingStream = stream
        tvCategoryAdapter.setNowPlaying(stream.streamId, stream.categoryId)

        val streamUrl = "$serverUrl/live/$username/$password/${stream.streamId}.m3u8"
        val mediaItem = MediaItem.fromUri(streamUrl)

        player?.setMediaItem(mediaItem)
        player?.prepare()

        binding.toolbarTv.title = stream.name
    }

    private fun loadInitialData() {
        if (_binding == null) return

        val fileData = DataCacheManager.loadTvCacheFromFile(requireContext())
        if (!fileData.isNullOrEmpty()) {
            DataCacheManager.tvCategories = fileData
            prepareAndSubmitList(fileData)
        }

        if (DataCacheManager.tvCategories.isNullOrEmpty() || DataCacheManager.isTvCacheStale(requireContext())) {
            loadDataFromServer(isSilent = !DataCacheManager.tvCategories.isNullOrEmpty())
        }
    }

    private fun prepareAndSubmitList(baseList: List<CategoryWithChannels>) {
        if (context == null || !isAdded) return
        val finalList = mutableListOf<CategoryWithChannels>()
        val favoriteChannels = LiveFavoritesManager.getFavorites(requireContext())

        // CORREGIDO: Siempre añadimos la categoría de Favoritos, incluso si está vacía.
        val favoritesCategory = Category(categoryId = "favorites", categoryName = "⭐ Favoritos", parentId = 0)
        finalList.add(CategoryWithChannels(favoritesCategory, favoriteChannels.toMutableList(), isExpanded = true))

        finalList.addAll(baseList.filter { it.category.categoryId != "favorites" })
        tvCategoryAdapter.submitList(finalList)
        binding.emptyStateLayout.root.visibility = if (finalList.isEmpty()) View.VISIBLE else View.GONE
        binding.progressBarTv.visibility = View.GONE
    }

    private fun loadDataFromServer(isSilent: Boolean) {
        if (_binding == null || !NetworkUtils.isNetworkAvailable(requireContext())) {
            if (!isSilent) showErrorState("Sin conexión a internet.")
            return
        }

        if (!isSilent) {
            binding.emptyStateLayout.root.visibility = View.GONE
            binding.progressBarTv.visibility = View.VISIBLE
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val categoriesResponse = apiService.getLiveCategories(username, password)
                if (categoriesResponse.isSuccessful) {
                    val apiCategories = categoriesResponse.body() ?: emptyList()
                    val apiCategoriesWithChannels = apiCategories.map { category ->
                        async {
                            try {
                                val channelsResponse = apiService.getLiveStreams(username, password, categoryId = category.categoryId)
                                if (channelsResponse.isSuccessful) {
                                    CategoryWithChannels(category, (channelsResponse.body() ?: emptyList()).toMutableList())
                                } else {
                                    Log.e("TvFragment", "Error al cargar canales para categoría ${category.categoryName}: ${channelsResponse.code()}")
                                    null
                                }
                            } catch (e: Exception) {
                                Log.e("TvFragment", "Excepción al cargar canales para ${category.categoryName}", e)
                                null
                            }
                        }
                    }.awaitAll().filterNotNull()

                    if (isActive) {
                        DataCacheManager.tvCategories = apiCategoriesWithChannels
                        DataCacheManager.saveTvCacheToFile(requireContext(), apiCategoriesWithChannels)
                        DataCacheManager.updateTvSyncTimestamp(requireContext())
                        prepareAndSubmitList(apiCategoriesWithChannels)
                    }
                } else {
                    Log.e("TvFragment", "Error al cargar categorías: ${categoriesResponse.code()}")
                    if (isActive && !isSilent) showErrorState("Error al obtener categorías del servidor.")
                }
            } catch (e: Exception) {
                Log.e("TvFragment", "Excepción al cargar datos del servidor", e)
                if (isActive && !isSilent) showErrorState("No se pudo conectar con el servidor.")
            } finally {
                if (isActive && !isSilent) {
                    binding.progressBarTv.visibility = View.GONE
                }
            }
        }
    }

    private fun showErrorState(message: String) {
        binding.emptyStateLayout.root.visibility = View.VISIBLE
        binding.emptyStateLayout.tvEmptyMessage.text = message
        binding.emptyStateLayout.ivEmptyIcon.setImageResource(R.drawable.ic_no_internet)
        binding.emptyStateLayout.btnRetry.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
        loadInitialData()
    }

    override fun onResume() {
        super.onResume()
        player?.play()
        currentPlayingStream?.let {
            tvCategoryAdapter.setNowPlaying(it.streamId, it.categoryId)
        }
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun releasePlayer() {
        if (player != null) {
            binding.playerViewTv.player = null
            player?.release()
            player = null
            tvCategoryAdapter.setNowPlaying(null, null)
            currentPlayingStream = null
            binding.toolbarTv.title = "TV en Vivo"
            binding.playerContainer.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releasePlayer()
        _binding = null
    }
}
