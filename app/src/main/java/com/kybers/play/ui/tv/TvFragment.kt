package com.kybers.play.ui.tv

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.BehindLiveWindowException
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
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
import com.kybers.play.ui.search.SearchActivity
import com.kybers.play.ui.settings.SettingsActivity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@androidx.media3.common.util.UnstableApi
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
    private val MAX_RETRIES = 5

    private var currentStickyHeaderPosition = RecyclerView.NO_POSITION

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
        setupSearch() // AÑADIDO
        binding.btnRetry.setOnClickListener {
            loadInitialData()
        }

        handleScreenOrientation(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
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
        binding.recyclerViewTv.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewTv.adapter = tvCategoryAdapter

        setupStickyHeader()
    }

    // AÑADIDO: Nueva función para configurar la búsqueda.
    private fun setupSearch() {
        binding.searchViewTv.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                tvCategoryAdapter.filter(newText)
                return true
            }
        })
    }

    private fun setupStickyHeader() {
        binding.recyclerViewTv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                if (firstVisibleItemPosition == RecyclerView.NO_POSITION) {
                    binding.stickyHeaderView.root.visibility = View.GONE
                    return
                }

                val headerPosition = tvCategoryAdapter.getHeaderPositionForItem(firstVisibleItemPosition)

                if (headerPosition != RecyclerView.NO_POSITION) {
                    if (currentStickyHeaderPosition != headerPosition) {
                        currentStickyHeaderPosition = headerPosition
                        tvCategoryAdapter.bindHeaderData(binding.stickyHeaderView.root, headerPosition)
                    }
                    binding.stickyHeaderView.root.visibility = View.VISIBLE

                    val nextHeaderView = findNextHeaderView(layoutManager, firstVisibleItemPosition)
                    if (nextHeaderView != null && nextHeaderView.top <= binding.stickyHeaderView.root.height) {
                        val translation = nextHeaderView.top - binding.stickyHeaderView.root.height
                        binding.stickyHeaderView.root.translationY = translation.toFloat()
                    } else {
                        binding.stickyHeaderView.root.translationY = 0f
                    }

                } else {
                    binding.stickyHeaderView.root.visibility = View.GONE
                    currentStickyHeaderPosition = RecyclerView.NO_POSITION
                }
            }
        })

        binding.stickyHeaderView.root.setOnClickListener {
            if (currentStickyHeaderPosition != RecyclerView.NO_POSITION) {
                tvCategoryAdapter.handleCategoryClick(currentStickyHeaderPosition)
            }
        }
    }

    private fun findNextHeaderView(layoutManager: LinearLayoutManager, firstVisibleItemPosition: Int): View? {
        for (i in 0 until layoutManager.childCount) {
            val child = layoutManager.getChildAt(i)
            val position = layoutManager.getPosition(child!!)
            if (position > firstVisibleItemPosition && tvCategoryAdapter.isHeader(position)) {
                return child
            }
        }
        return null
    }

    private fun initializePlayer() {
        if (player == null && context != null) {
            val renderersFactory = DefaultRenderersFactory(requireContext())
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(30_000, 60_000, 2_500, 5_000).build()

            val loadErrorHandlingPolicy = object : DefaultLoadErrorHandlingPolicy() {
                override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
                    if (loadErrorInfo.exception is BehindLiveWindowException) return 500
                    return C.TIME_UNSET
                }
                override fun getMinimumLoadableRetryCount(dataType: Int): Int = Int.MAX_VALUE
            }

            val mediaSourceFactory = DefaultMediaSourceFactory(requireContext())
                .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)

            player = ExoPlayer.Builder(requireContext(), renderersFactory)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .build().also { exoPlayer ->
                    binding.playerViewTv.player = exoPlayer
                    exoPlayer.playWhenReady = true
                    exoPlayer.addListener(object : Player.Listener {
                        override fun onPlayerError(error: PlaybackException) {
                            if (error.cause is BehindLiveWindowException) {
                                Log.w("TvFragment", "Atrás de la ventana en vivo. Saltando al directo.")
                                exoPlayer.seekToDefaultPosition()
                                exoPlayer.prepare()
                            } else {
                                Toast.makeText(context, "Error de reproducción", Toast.LENGTH_SHORT).show()
                            }
                        }
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            binding.playerBuffering.visibility = if (playbackState == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
                        }
                    })
                }
        }
    }

    private fun playStream(stream: LiveStream) {
        currentPlayingStream = stream
        // CORREGIDO: Pasa también el categoryId al adaptador.
        tvCategoryAdapter.setNowPlaying(stream.streamId, stream.categoryId)

        val streamUrl = "$serverUrl/live/$username/$password/${stream.streamId}.m3u8"
        val mediaItem = MediaItem.Builder()
            .setUri(streamUrl)
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(30000)
                    .setMinOffsetMs(20000)
                    .setMaxOffsetMs(50000)
                    .build()
            ).build()

        player?.setMediaItem(mediaItem, true)
        player?.prepare()

        // CORREGIDO: Actualiza el título de la Toolbar.
        binding.toolbarTv.title = "TV: ${stream.name}"
    }

    private fun loadInitialData() {
        if (_binding == null) return
        DataCacheManager.tvCategories?.let {
            tvCategoryAdapter.submitList(it)
            binding.emptyStateLayout.visibility = View.GONE
            binding.progressBarTv.visibility = View.GONE
        }
        loadDataFromServer(isSilent = DataCacheManager.tvCategories != null)
    }

    private fun loadDataFromServer(isSilent: Boolean) {
        if (_binding == null) return
        if (!isSilent) {
            binding.emptyStateLayout.visibility = View.GONE
            binding.progressBarTv.visibility = View.VISIBLE
        }
        viewLifecycleOwner.lifecycleScope.launch {
            var success = false
            for (attempt in 1..MAX_RETRIES) {
                try {
                    val finalCategoriesList = mutableListOf<CategoryWithChannels>()
                    context?.let {
                        val favoriteChannels = LiveFavoritesManager.getFavorites(it)
                        if (favoriteChannels.isNotEmpty()) {
                            val favoritesCategory = Category(categoryId = "favorites", categoryName = "⭐ Favoritos", parentId = 0)
                            finalCategoriesList.add(CategoryWithChannels(favoritesCategory, favoriteChannels.toMutableList(), isExpanded = true))
                        }
                    }
                    val categoriesResponse = apiService.getLiveCategories(username, password)
                    if (categoriesResponse.isSuccessful) {
                        val apiCategories = categoriesResponse.body() ?: emptyList()
                        val apiCategoriesWithChannels = apiCategories.map { category ->
                            async {
                                val channelsResponse = apiService.getLiveStreams(username, password, categoryId = category.categoryId)
                                if (channelsResponse.isSuccessful) {
                                    CategoryWithChannels(category, (channelsResponse.body() ?: emptyList()).toMutableList())
                                } else { null }
                            }
                        }.awaitAll().filterNotNull()
                        finalCategoriesList.addAll(apiCategoriesWithChannels)
                        if (isActive) {
                            DataCacheManager.tvCategories = finalCategoriesList
                            tvCategoryAdapter.submitList(finalCategoriesList)
                        }
                        success = true
                        break
                    } else {
                        throw Exception("Error de la API: ${categoriesResponse.code()}")
                    }
                } catch (e: Exception) {
                    Log.e("TvFragment", "Intento de carga #$attempt falló: ${e.message}")
                    if (attempt == MAX_RETRIES && !isSilent) {
                        if (context != null) {
                            Toast.makeText(context, "No se pudo conectar después de $MAX_RETRIES intentos.", Toast.LENGTH_LONG).show()
                        }
                    } else if (attempt < MAX_RETRIES) {
                        delay(2000L * attempt)
                    }
                }
            }
            if (isActive && !isSilent) {
                binding.progressBarTv.visibility = View.GONE
                if (!success) showErrorState()
            }
        }
    }

    private fun showErrorState() {
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.tvEmptyMessage.text = "Error al cargar el contenido."
        binding.ivEmptyIcon.setImageResource(R.drawable.ic_no_internet)
        binding.btnRetry.visibility = View.VISIBLE
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        handleScreenOrientation(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
    }

    private fun handleScreenOrientation(isLandscape: Boolean) {
        if (_binding == null) return

        activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.visibility = if (isLandscape) View.GONE else View.VISIBLE
        binding.appBarLayout.visibility = if (isLandscape) View.GONE else View.VISIBLE
        binding.listContainer.visibility = if (isLandscape) View.GONE else View.VISIBLE
        binding.searchViewTv.visibility = if (isLandscape) View.GONE else View.VISIBLE // Ocultar búsqueda en horizontal

        val playerContainer = binding.playerContainer
        val params = playerContainer.layoutParams as ViewGroup.MarginLayoutParams

        val contentParent = binding.listContainer.parent as View
        val contentParams = contentParent.layoutParams as CoordinatorLayout.LayoutParams

        if (isLandscape) {
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            contentParams.behavior = null
            hideSystemUi()
        } else {
            params.height = resources.getDimensionPixelSize(R.dimen.player_height_portrait)
            contentParams.behavior = AppBarLayout.ScrollingViewBehavior()
            showSystemUi()
        }
        playerContainer.layoutParams = params
        contentParent.layoutParams = contentParams
    }

    private fun hideSystemUi() {
        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, binding.root).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    private fun showSystemUi() {
        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, true)
            WindowInsetsControllerCompat(window, binding.root).show(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
        loadInitialData()
    }

    override fun onResume() {
        super.onResume()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        player?.play()
        currentPlayingStream?.let {
            tvCategoryAdapter.setNowPlaying(it.streamId, it.categoryId)
        }
    }

    override fun onPause() {
        super.onPause()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
            tvCategoryAdapter.setNowPlaying(null, null) // Limpiar resaltado
            currentPlayingStream = null
            binding.toolbarTv.title = "TV en Vivo" // Restaurar título
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releasePlayer()
        _binding = null
    }
}
