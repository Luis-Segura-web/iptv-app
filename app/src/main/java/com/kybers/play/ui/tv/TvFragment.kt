package com.kybers.play.ui.tv

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.kybers.play.MainActivity
import com.kybers.play.R
import com.kybers.play.adapter.CategoryWithChannels
import com.kybers.play.adapter.TvCategoryAdapter
import com.kybers.play.api.LiveStream
import com.kybers.play.api.RetrofitClient
import com.kybers.play.api.XtreamApiService
import com.kybers.play.databinding.FragmentTvBinding
import com.kybers.play.manager.SettingsManager
import com.kybers.play.ui.search.SearchActivity
import com.kybers.play.ui.settings.SettingsActivity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class TvFragment : Fragment() {

    private var _binding: FragmentTvBinding? = null
    private val binding get() = _binding!!

    private var player: ExoPlayer? = null
    private lateinit var tvCategoryAdapter: TvCategoryAdapter
    private lateinit var apiService: XtreamApiService
    private lateinit var username: String
    private lateinit var password: String
    private lateinit var serverUrl: String

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
        initializePlayer()
        loadData()
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
            onCategoryClick = { /* Podrías añadir lógica aquí si lo necesitas */ },
            onChannelClick = { channel -> playStream(channel) }
        )
        binding.recyclerViewTv.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewTv.adapter = tvCategoryAdapter
        binding.recyclerViewTv.isNestedScrollingEnabled = false
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(requireContext()).build().also {
            binding.playerViewTv.player = it
            it.playWhenReady = true
        }
    }

    private fun playStream(stream: LiveStream) {
        val streamUrl = "$serverUrl/live/$username/$password/${stream.streamId}.m3u8"
        val mediaItem = MediaItem.fromUri(streamUrl)
        player?.setMediaItem(mediaItem)
        player?.prepare()
        Toast.makeText(context, "Reproduciendo: ${stream.name}", Toast.LENGTH_SHORT).show()
    }

    private fun loadData() {
        binding.progressBarTv.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val categoriesResponse = apiService.getLiveCategories(username, password)
                if (categoriesResponse.isSuccessful) {
                    val categories = categoriesResponse.body() ?: emptyList()

                    // Cargar canales para cada categoría en paralelo
                    val categoriesWithChannels = categories.map { category ->
                        async {
                            val channelsResponse = apiService.getLiveStreams(username, password, categoryId = category.categoryId)
                            if (channelsResponse.isSuccessful) {
                                CategoryWithChannels(category, channelsResponse.body() ?: emptyList())
                            } else {
                                null
                            }
                        }
                    }.awaitAll().filterNotNull()

                    tvCategoryAdapter.submitList(categoriesWithChannels)
                } else {
                    Toast.makeText(context, "Error al cargar categorías", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBarTv.visibility = View.GONE
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val mainActivity = activity as? MainActivity
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Entrar en modo pantalla completa
            binding.playerViewTv.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            mainActivity?.findViewById<View>(R.id.bottom_navigation)?.visibility = View.GONE
            hideSystemUi()
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // Salir de pantalla completa
            binding.playerViewTv.layoutParams.height = resources.getDimensionPixelSize(R.dimen.player_height_portrait)
            mainActivity?.findViewById<View>(R.id.bottom_navigation)?.visibility = View.VISIBLE
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

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onResume() {
        super.onResume()
        if (player?.isPlaying == false && player?.mediaItemCount ?: 0 > 0) {
            player?.play()
        }
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releasePlayer()
        _binding = null
    }
}
