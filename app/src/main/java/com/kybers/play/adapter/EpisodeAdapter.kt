package com.kybers.play.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.kybers.play.R
import com.kybers.play.api.Episode
import com.kybers.play.databinding.ItemEpisodeBinding

class EpisodeAdapter(
    private val episodes: List<Episode>,
    private val onItemClick: (Episode, Int) -> Unit
) : RecyclerView.Adapter<EpisodeAdapter.ViewHolder>() {

    private var nowPlayingEpisodeId: String? = null

    inner class ViewHolder(val binding: ItemEpisodeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(episode: Episode, position: Int) {
            binding.tvEpisodeNumber.text = "${episode.episodeNum}."
            binding.tvEpisodeTitle.text = episode.title

            if (episode.id == nowPlayingEpisodeId) {
                binding.root.setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.now_playing_background))
            } else {
                binding.root.setBackgroundColor(ContextCompat.getColor(binding.root.context, android.R.color.transparent))
            }

            binding.root.setOnClickListener { onItemClick(episode, position) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEpisodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(episodes[position], position)
    }

    override fun getItemCount() = episodes.size

    fun setNowPlaying(episodeId: String) {
        val previousPlayingId = nowPlayingEpisodeId
        nowPlayingEpisodeId = episodeId

        findPositionById(previousPlayingId)?.let { notifyItemChanged(it) }
        findPositionById(nowPlayingEpisodeId)?.let { notifyItemChanged(it) }
    }

    private fun findPositionById(episodeId: String?): Int? {
        if (episodeId == null) return null
        val position = episodes.indexOfFirst { it.id == episodeId }
        return if (position != -1) position else null
    }
}
