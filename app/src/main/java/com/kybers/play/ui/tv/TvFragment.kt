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
import android.widget.RelativeLayout
import android.widget.Toast
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.BehindLiveWindowException
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.recyclerview.widget.LinearLayoutManager
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
import java.io.IOException

class TvFragment : Fragment() {

    private var _binding: FragmentTvBinding? = null
    private val binding get() = _binding!!

    private var player: ExoPlayer? = null
    private lateinit var tvCategoryAdapter: TvCategoryAdapter
    private lateinit var apiService: XtreamApiService
    private lateinit var username: String
    private lateinit var password: String
    private lateinit var serverUrl: String

    private val MAX_RETRIES = 5

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
        binding.btnRetry.setOnClickListener {
            loadInitialData()
        }
    }

    private fun setupToolbar() {
        binding.btnSearchTv.setOnClickListener {
            startActivity(Intent(activity, SearchActivity::class.java))
        }
        binding.btnSettingsTv.setOnClickListener {
            startActivity(Intent(activity, SettingsActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        tvCategoryAdapter = TvCategoryAdapter(
            onChannelClick = { channel -> playStream(channel) },
            onFavoriteClick = { channel -> handleFavoriteClick(channel) }
        )
        binding.recyclerViewTv.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewTv.adapter = tvCategoryAdapter
    }

    private fun handleFavoriteClick(channel: LiveStream) {
        context?.let {
            if (LiveFavoritesManager.isFavorite(it, channel)) {
                LiveFavoritesManager.removeFavorite(it, channel)
                Toast.makeText(it, "${channel.name} eliminado de favoritos", Toast.LENGTH_SHORT).show()
            } else {
                LiveFavoritesManager.addFavorite(it, channel)
                Toast.makeText(it, "${channel.name} añadido a favoritos", Toast.LENGTH_SHORT).show()
            }
            loadInitialData()
        }
    }

    private fun initializePlayer() {
        if (player == null) {
            // 1. Aumentamos el buffer para tener un "colchón" de 30-60 segundos
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    30_000, // minBufferMs: Mínimo 30 segundos de video en caché
                    60_000, // maxBufferMs: Máximo 60 segundos de video en caché
                    2_500,  // bufferForPlaybackMs: Empezar a reproducir tras 2.5s de carga
                    5_000   // bufferForPlaybackAfterRebufferMs: Reanudar tras 5s de carga
                ).build()

            // 2. Política de reintentos personalizada y MUY persistente
            val loadErrorHandlingPolicy = object : DefaultLoadErrorHandlingPolicy() {
                override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
                    // Si el error es por quedarse atrás en el directo, reintenta muy rápido.
                    if (loadErrorInfo.exception is BehindLiveWindowException) {
                        Log.w("TvFragment", "BehindLiveWindowException detectado, reintentando en 500ms...")
                        return 500
                    }
                    // Para otros errores de red, usa la estrategia por defecto (espera incremental).
                    return C.TIME_UNSET
                }

                override fun getMinimumLoadableRetryCount(dataType: Int): Int {
                    // Reintentar "infinitamente" para que la conexión se recupere sola.
                    return Int.MAX_VALUE
                }
            }

            val mediaSourceFactory = DefaultMediaSourceFactory(requireContext())
                .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)

            // 3. Construimos el reproductor con ambas mejoras
            player = ExoPlayer.Builder(requireContext())
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .build().also { exoPlayer ->
                    binding.playerViewTv.player = exoPlayer
                    exoPlayer.playWhenReady = true

                    exoPlayer.addListener(object : Player.Listener {
                        override fun onPlayerError(error: PlaybackException) {
                            // La política de reintentos ya maneja la mayoría de los errores.
                            // Este listener es un último recurso.
                            if (error.cause is BehindLiveWindowException) {
                                Log.w("TvFragment", "Atrás de la ventana en vivo. Saltando al directo.")
                                exoPlayer.seekToDefaultPosition()
                                exoPlayer.prepare()
                            } else {
                                Toast.makeText(context, "Error de reproducción: ${error.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    })
                }
        }
    }

    private fun playStream(stream: LiveStream) {
        val streamUrl = "$serverUrl/live/$username/$password/${stream.streamId}.m3u8"

        val mediaItem = MediaItem.Builder()
            .setUri(streamUrl)
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(10000)
                    .setMinOffsetMs(5000)
                    .setMaxOffsetMs(20000)
                    .build()
            )
            .build()

        player?.setMediaItem(mediaItem, true)
        player?.prepare()

        binding.tvNowPlayingChannel.text = "Reproduciendo: ${stream.name}"
        binding.tvNowPlayingChannel.visibility = View.VISIBLE
    }

    private fun loadInitialData() {
        DataCacheManager.tvCategories?.let {
            tvCategoryAdapter.submitList(it)
            binding.emptyStateLayout.visibility = View.GONE
            binding.progressBarTv.visibility = View.GONE
        }

        loadDataFromServer(isSilent = DataCacheManager.tvCategories != null)
    }

    private fun loadDataFromServer(isSilent: Boolean) {
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
                            finalCategoriesList.add(CategoryWithChannels(favoritesCategory, favoriteChannels, isExpanded = true))
                        }
                    }

                    val categoriesResponse = apiService.getLiveCategories(username, password)
                    if (categoriesResponse.isSuccessful) {
                        val apiCategories = categoriesResponse.body() ?: emptyList()

                        val apiCategoriesWithChannels = apiCategories.map { category ->
                            async {
                                val channelsResponse = apiService.getLiveStreams(username, password, categoryId = category.categoryId)
                                if (channelsResponse.isSuccessful) {
                                    CategoryWithChannels(category, channelsResponse.body() ?: emptyList())
                                } else {
                                    null
                                }
                            }
                        }.awaitAll().filterNotNull()

                        finalCategoriesList.addAll(apiCategoriesWithChannels)

                        DataCacheManager.tvCategories = finalCategoriesList
                        tvCategoryAdapter.submitList(finalCategoriesList)

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

            if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
                if (!isSilent) {
                    binding.progressBarTv.visibility = View.GONE
                    if (!success) {
                        showErrorState()
                    }
                }
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
        val isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE

        activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.visibility = if (isLandscape) View.GONE else View.VISIBLE
        binding.appBarLayout.visibility = if (isLandscape) View.GONE else View.VISIBLE
        binding.recyclerViewTv.visibility = if (isLandscape) View.GONE else View.VISIBLE
        binding.tvNowPlayingChannel.visibility = if (isLandscape || player?.isPlaying != true) View.GONE else View.VISIBLE

        val playerContainer = binding.playerContainer
        val playerView = binding.playerViewTv

        if (isLandscape) {
            val containerParams = playerContainer.layoutParams as RelativeLayout.LayoutParams
            containerParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            playerContainer.layoutParams = containerParams

            val playerParams = playerView.layoutParams
            playerParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            playerView.layoutParams = playerParams

            hideSystemUi()
        } else {
            val containerParams = playerContainer.layoutParams as RelativeLayout.LayoutParams
            containerParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            playerContainer.layoutParams = containerParams

            val playerParams = playerView.layoutParams
            playerParams.height = resources.getDimensionPixelSize(R.dimen.player_height_portrait)
            playerView.layoutParams = playerParams

            showSystemUi()
        }
    }

    private fun hideSystemUi() {
        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, binding.playerViewTv).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    private fun showSystemUi() {
        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, true)
            WindowInsetsControllerCompat(window, binding.playerViewTv).show(WindowInsetsCompat.Type.systemBars())
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
        player?.release()
        player = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
