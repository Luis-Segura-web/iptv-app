package com.kybers.play

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Rect
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.util.Rational
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.ui.AspectRatioFrameLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kybers.play.api.Episode
import com.kybers.play.api.LiveStream
import com.kybers.play.api.Movie
import com.kybers.play.databinding.ActivityPlayerBinding
import com.kybers.play.manager.HistoryItem
import com.kybers.play.manager.HistoryManager
import com.kybers.play.manager.SettingsManager
import com.kybers.play.util.GestureControlView
import java.util.Locale

@UnstableApi
class PlayerActivity : AppCompatActivity(), GestureControlView.GestureListener {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null

    private var livePlaylist: List<LiveStream> = emptyList()
    private var seriesPlaylist: List<Episode> = emptyList()
    private var currentIndex: Int = -1
    private var itemType: String? = null
    private var seriesTitle: String? = null
    private var isFullscreen = false
    private var currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
    private var startPosition: Long = 0L

    private var countdownTimer: CountDownTimer? = null
    private lateinit var audioManager: AudioManager
    private var maxVolume: Int = 0
    private var currentVolume: Int = 0
    private var currentBrightness: Float = 0.5f

    companion object {
        const val EXTRA_ITEM_JSON = "extra_item_json"
        const val EXTRA_ITEM_TYPE = "extra_item_type"
        const val EXTRA_PLAYLIST_JSON = "extra_playlist_json"
        const val EXTRA_CURRENT_INDEX = "extra_current_index"
        const val EXTRA_SERIES_TITLE = "extra_series_title"
        const val EXTRA_START_POSITION = "extra_start_position"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        itemType = intent.getStringExtra(EXTRA_ITEM_TYPE)

        binding.playerView.setControllerLayoutId(R.layout.custom_player_controls)

        val playlistJson = intent.getStringExtra(EXTRA_PLAYLIST_JSON)
        currentIndex = intent.getIntExtra(EXTRA_CURRENT_INDEX, -1)
        seriesTitle = intent.getStringExtra(EXTRA_SERIES_TITLE)
        startPosition = intent.getLongExtra(EXTRA_START_POSITION, 0L)

        setupGestureControls()
        initVolumeAndBrightness()

        if (playlistJson != null && currentIndex != -1) {
            val gson = Gson()
            when (itemType) {
                "live" -> {
                    val type = object : TypeToken<List<LiveStream>>() {}.type
                    livePlaylist = gson.fromJson(playlistJson, type)
                    playItemFromLivePlaylist(currentIndex)
                }
                "series" -> {
                    val type = object : TypeToken<List<Episode>>() {}.type
                    seriesPlaylist = gson.fromJson(playlistJson, type)
                    playItemFromSeriesPlaylist(currentIndex)
                }
            }
        } else {
            val itemJson = intent.getStringExtra(EXTRA_ITEM_JSON)
            if (itemJson != null && itemType != null) {
                playSingleItem(itemJson, itemType!!)
            } else {
                finish()
                return
            }
        }
    }

    private fun playSingleItem(itemJson: String, type: String) {
        val gson = Gson()
        var playbackUrl: String? = null
        var title: String? = null
        val sharedPrefs = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        val serverUrl = sharedPrefs.getString("SERVER_URL", "")!!
        val username = sharedPrefs.getString("USERNAME", "")!!
        val password = sharedPrefs.getString("PASSWORD", "")!!

        if (type == "movie") {
            val item = gson.fromJson(itemJson, Movie::class.java)
            playbackUrl = "$serverUrl/movie/$username/$password/${item.streamId}.${item.containerExtension}"
            title = item.name
            HistoryManager.addItemToHistory(this, item)
        }

        binding.tvContentTitle.text = title
        initializePlayer(playbackUrl)
    }

    private fun playItemFromLivePlaylist(index: Int) {
        if (livePlaylist.isEmpty() || index < 0 || index >= livePlaylist.size) return
        currentIndex = index
        val item = livePlaylist[currentIndex]

        val sharedPrefs = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        val serverUrl = sharedPrefs.getString("SERVER_URL", "")!!
        val username = sharedPrefs.getString("USERNAME", "")!!
        val password = sharedPrefs.getString("PASSWORD", "")!!

        val playbackUrl = "$serverUrl/live/$username/$password/${item.streamId}.m3u8"
        binding.tvContentTitle.text = item.name

        HistoryManager.addItemToHistory(this, item)
        initializePlayer(playbackUrl)
    }

    private fun playItemFromSeriesPlaylist(index: Int) {
        if (seriesPlaylist.isEmpty() || index < 0 || index >= seriesPlaylist.size) return
        currentIndex = index
        val item = seriesPlaylist[currentIndex]

        val sharedPrefs = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        val serverUrl = sharedPrefs.getString("SERVER_URL", "")!!
        val username = sharedPrefs.getString("USERNAME", "")!!
        val password = sharedPrefs.getString("PASSWORD", "")!!

        val playbackUrl = "$serverUrl/series/$username/$password/${item.id}.${item.containerExtension}"
        val titleText = "$seriesTitle: T${item.episodeNum} E${item.episodeNum} - ${item.title}"
        binding.tvContentTitle.text = titleText

        initializePlayer(playbackUrl)
    }

    private fun initializePlayer(url: String?) {
        if (url == null) return
        releasePlayer()

        val loadControl = DefaultLoadControl.Builder().build()
        val userAgent = SettingsManager.getUserAgent(this)

        val mediaSourceFactory = HlsMediaSource.Factory(DefaultHttpDataSource.Factory().setUserAgent(userAgent))

        player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .also { exoPlayer ->
                binding.playerView.player = exoPlayer
                val mediaItem = MediaItem.fromUri(url)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.seekTo(startPosition)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
                exoPlayer.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            handlePlaybackEnd()
                        }
                    }
                    override fun onTracksChanged(tracks: Tracks) {
                        super.onTracksChanged(tracks)
                        updateTrackSelectionButton()
                    }
                })
                binding.playerView.post { setupCustomControls() }
            }
    }

    private fun handlePlaybackEnd() {
        if (itemType == "series" && SettingsManager.isAutoplayNextEpisodeEnabled(this)) {
            showAutoplayCountdown()
        }
    }

    private fun showAutoplayCountdown() {
        val countdownText = binding.playerView.findViewById<TextView>(R.id.countdown_text)
        countdownText?.visibility = View.VISIBLE

        countdownTimer = object : CountDownTimer(6000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val remainingSeconds = millisUntilFinished / 1000
                countdownText?.text = getString(R.string.next_episode_countdown, remainingSeconds)
            }
            override fun onFinish() {
                countdownText?.visibility = View.GONE
                playNextEpisode()
            }
        }.start()
    }

    private fun setupCustomControls() {
        val btnFullscreen = binding.playerView.findViewById<ImageButton>(R.id.btn_fullscreen)
        val btnClose = binding.playerView.findViewById<ImageButton>(R.id.btn_close)
        val btnTracks = binding.playerView.findViewById<ImageButton>(R.id.btn_tracks)
        val btnAspectRatio = binding.playerView.findViewById<ImageButton>(R.id.btn_aspect_ratio)
        val btnPip = binding.playerView.findViewById<ImageButton>(R.id.btn_pip)
        val btnNextLive = binding.playerView.findViewById<ImageButton>(R.id.btn_next)
        val btnPrevLive = binding.playerView.findViewById<ImageButton>(R.id.btn_prev)
        val btnFfwd = binding.playerView.findViewById<ImageButton>(androidx.media3.ui.R.id.exo_ffwd)
        val btnRew = binding.playerView.findViewById<ImageButton>(androidx.media3.ui.R.id.exo_rew)
        val btnNextEpisode = binding.playerView.findViewById<ImageButton>(R.id.btn_next_episode)

        btnClose.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        btnFullscreen.setOnClickListener { toggleFullscreen() }
        btnTracks?.setOnClickListener { showTrackSelectionDialog() }
        btnAspectRatio?.setOnClickListener { toggleAspectRatio() }
        btnPip?.setOnClickListener { enterPiPMode() }

        when (itemType) {
            "live" -> {
                btnNextLive.visibility = View.VISIBLE
                btnPrevLive.visibility = View.VISIBLE
                btnFfwd.visibility = View.GONE
                btnRew.visibility = View.GONE
                btnNextEpisode?.visibility = View.GONE
                btnNextLive.setOnClickListener { playNext() }
                btnPrevLive.setOnClickListener { playPrevious() }
            }
            "movie" -> {
                btnNextLive.visibility = View.GONE
                btnPrevLive.visibility = View.GONE
                btnFfwd.visibility = View.VISIBLE
                btnRew.visibility = View.VISIBLE
                btnNextEpisode?.visibility = View.GONE
            }
            "series" -> {
                btnNextLive.visibility = View.GONE
                btnPrevLive.visibility = View.GONE
                btnFfwd.visibility = View.VISIBLE
                btnRew.visibility = View.VISIBLE
                btnNextEpisode?.visibility = View.VISIBLE
                btnNextEpisode.setOnClickListener {
                    countdownTimer?.cancel()
                    binding.playerView.findViewById<TextView>(R.id.countdown_text)?.visibility = View.GONE
                    playNextEpisode()
                }
            }
        }
    }

    private fun updateTrackSelectionButton() {
        val btnTracks = binding.playerView.findViewById<ImageButton>(R.id.btn_tracks) ?: return
        val trackGroups = player?.currentTracks?.groups ?: return

        val hasAudioOptions = trackGroups.any { it.type == C.TRACK_TYPE_AUDIO && it.length > 1 }
        val hasSubtitleOptions = trackGroups.any { it.type == C.TRACK_TYPE_TEXT }

        btnTracks.visibility = if (hasAudioOptions || hasSubtitleOptions) View.VISIBLE else View.GONE
    }

    private fun showTrackSelectionDialog() {
        val player = this.player ?: return
        val trackGroups = player.currentTracks.groups

        val dialogView = layoutInflater.inflate(R.layout.dialog_track_selection, null)
        val audioGroup = dialogView.findViewById<RadioGroup>(R.id.audio_track_group)
        val subtitleGroup = dialogView.findViewById<RadioGroup>(R.id.subtitle_track_group)

        var audioGroupIndex = -1
        var subtitleGroupIndex = -1

        for ((groupIndex, trackGroup) in trackGroups.withIndex()) {
            if (trackGroup.type == C.TRACK_TYPE_AUDIO) {
                audioGroupIndex = groupIndex
                for (i in 0 until trackGroup.length) {
                    val format = trackGroup.getFormat(i)
                    val lang = format.language
                    val label = format.label ?: lang?.let { Locale.forLanguageTag(it).displayLanguage } ?: "Audio #${i + 1}"
                    val radioButton = RadioButton(this).apply { text = label; id = i }
                    if (trackGroup.isTrackSelected(i)) {
                        radioButton.isChecked = true
                    }
                    audioGroup.addView(radioButton)
                }
            } else if (trackGroup.type == C.TRACK_TYPE_TEXT) {
                subtitleGroupIndex = groupIndex
                for (i in 0 until trackGroup.length) {
                    val format = trackGroup.getFormat(i)
                    val lang = format.language
                    val label = format.label ?: lang?.let { Locale.forLanguageTag(it).displayLanguage } ?: "Subtítulos #${i + 1}"
                    val radioButton = RadioButton(this).apply { text = label; id = i }
                    if (trackGroup.isTrackSelected(i)) {
                        radioButton.isChecked = true
                    }
                    subtitleGroup.addView(radioButton)
                }
            }
        }

        val noSubsButton = RadioButton(this).apply { text = getString(R.string.disabled); id = -1 }
        if (subtitleGroup.checkedRadioButtonId == -1) {
            noSubsButton.isChecked = true
        }
        subtitleGroup.addView(noSubsButton, 0)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.audio_and_subtitles))
            .setView(dialogView)
            .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                val newParams = player.trackSelectionParameters.buildUpon()

                if (audioGroupIndex != -1) {
                    newParams.setOverrideForType(
                        TrackSelectionOverride(trackGroups[audioGroupIndex].mediaTrackGroup, audioGroup.checkedRadioButtonId)
                    )
                }

                if (subtitleGroupIndex != -1) {
                    val selectedSubtitleTrackIndex = subtitleGroup.checkedRadioButtonId
                    if (selectedSubtitleTrackIndex == -1) {
                        newParams.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    } else {
                        newParams.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        newParams.setOverrideForType(
                            TrackSelectionOverride(trackGroups[subtitleGroupIndex].mediaTrackGroup, selectedSubtitleTrackIndex)
                        )
                    }
                }

                player.trackSelectionParameters = newParams.build()
                dialog.dismiss()
            }
            .setNegativeButton(getString(android.R.string.cancel), null)
            .show()
    }

    private fun playNext() {
        if (currentIndex < livePlaylist.size - 1) playItemFromLivePlaylist(currentIndex + 1)
    }

    private fun playPrevious() {
        if (currentIndex > 0) playItemFromLivePlaylist(currentIndex - 1)
    }

    private fun playNextEpisode() {
        if (currentIndex < seriesPlaylist.size - 1) {
            playItemFromSeriesPlaylist(currentIndex + 1)
        } else {
            Toast.makeText(this, R.string.end_of_season, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun toggleFullscreen() {
        if (isFullscreen) exitFullscreen() else enterFullscreen()
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun enterFullscreen() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun exitFullscreen() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val btnFullscreen = binding.playerView.findViewById<ImageButton>(R.id.btn_fullscreen)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            hideSystemUi()
            binding.infoContainer.visibility = View.GONE
            btnFullscreen.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_fullscreen_exit))
            isFullscreen = true
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            showSystemUi()
            binding.infoContainer.visibility = View.VISIBLE
            btnFullscreen.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_fullscreen))
            isFullscreen = false
        }
    }

    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun showSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, binding.root).show(WindowInsetsCompat.Type.systemBars())
    }

    private fun releasePlayer() {
        countdownTimer?.cancel()
        saveHistory()
        player?.release()
        player = null
    }

    private fun saveHistory() {
        val player = this.player ?: return
        val content = this.currentContent ?: return

        val lastPosition = player.currentPosition
        val duration = player.duration

        if (lastPosition > 1000 && duration > 0) {
            val historyItem = HistoryItem(content, lastPosition, duration)
            HistoryManager.addItemToHistory(this, historyItem)
        }
    }

    override fun onResume() {
        super.onResume()
        if (player == null) {
            if (livePlaylist.isNotEmpty()) playItemFromLivePlaylist(currentIndex)
            else if (seriesPlaylist.isNotEmpty()) playItemFromSeriesPlaylist(currentIndex)
        }
    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode) {
            // No liberar el reproductor si estamos en modo PiP
        } else {
            releasePlayer()
        }
    }

    private fun setupGestureControls() {
        binding.gestureView.setGestureListener(this)
    }

    private fun initVolumeAndBrightness() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        try {
            currentBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }
    }

    override fun onSingleTap() {
        if (binding.playerView.isControllerFullyVisible) {
            binding.playerView.hideController()
        } else {
            binding.playerView.showController()
        }
    }

    override fun onDoubleTap(isLeft: Boolean) {
        if (isLeft) {
            player?.seekTo(player!!.currentPosition - 10000)
            showSeekAnimation(binding.replayContainer)
        } else {
            player?.seekTo(player!!.currentPosition + 10000)
            showSeekAnimation(binding.forwardContainer)
        }
    }

    private fun showSeekAnimation(view: View) {
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)

        fadeIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) { view.startAnimation(fadeOut) }
            override fun onAnimationRepeat(animation: Animation?) {}
        })

        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) { view.visibility = View.GONE }
            override fun onAnimationRepeat(animation: Animation?) {}
        })

        view.visibility = View.VISIBLE
        view.startAnimation(fadeIn)
    }

    override fun onScroll(distanceY: Float, isLeft: Boolean) {
        val sensitivity = 1.5f
        val screenHeight = binding.root.height
        if (screenHeight == 0) return

        if (isLeft) { // Brillo
            currentBrightness -= (distanceY / screenHeight) * sensitivity
            currentBrightness = currentBrightness.coerceIn(0.0f, 1.0f)
            setBrightness(currentBrightness)
            showIndicator(true, (currentBrightness * 100).toInt())
        } else { // Volumen
            val delta = -(distanceY / screenHeight) * maxVolume * sensitivity
            currentVolume += delta.toInt()
            currentVolume = currentVolume.coerceIn(0, maxVolume)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
            showIndicator(false, (currentVolume.toFloat() / maxVolume * 100).toInt())
        }
    }

    private fun setBrightness(value: Float) {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = value
        window.attributes = layoutParams
    }

    private fun showIndicator(isBrightness: Boolean, progress: Int) {
        val indicator = if (isBrightness) binding.brightnessIndicator else binding.volumeIndicator
        val icon = indicator.indicatorIcon
        val progressBar = indicator.indicatorProgress
        val indicatorView = indicator.root

        icon.setImageResource(if (isBrightness) R.drawable.ic_brightness else R.drawable.ic_volume)
        progressBar.progress = progress

        indicatorView.visibility = View.VISIBLE
        indicatorView.postDelayed({ indicatorView.visibility = View.GONE }, 1500)
    }

    private fun toggleAspectRatio() {
        currentResizeMode = when (currentResizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> {
                Toast.makeText(this, "Zoom", Toast.LENGTH_SHORT).show()
                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> {
                Toast.makeText(this, "Rellenar", Toast.LENGTH_SHORT).show()
                AspectRatioFrameLayout.RESIZE_MODE_FILL
            }
            AspectRatioFrameLayout.RESIZE_MODE_FILL -> {
                Toast.makeText(this, "Ajustar", Toast.LENGTH_SHORT).show()
                AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        binding.playerView.resizeMode = currentResizeMode
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (player?.isPlaying == true) {
            enterPiPMode()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            binding.playerView.hideController()
        }
    }

    private fun enterPiPMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val sourceRectHint = Rect()
            binding.playerView.getGlobalVisibleRect(sourceRectHint)
            val aspectRatio = Rational(16, 9)
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .setSourceRectHint(sourceRectHint)
                .build()
            enterPictureInPictureMode(params)
        }
    }
}
