package com.kybers.play.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kybers.play.R
import com.kybers.play.api.Episode
import com.kybers.play.databinding.ItemEpisodeBinding

/**
 * Adaptador para la lista de episodios en la pantalla de detalles de la serie.
 * Usa ListAdapter y DiffUtil para un rendimiento eficiente.
 */
class EpisodeAdapter(
    private val onItemClick: (Episode, Int) -> Unit
) : ListAdapter<Episode, EpisodeAdapter.ViewHolder>(EpisodeDiffCallback()) {

    private var nowPlayingEpisodeId: String? = null

    /**
     * ViewHolder para un solo item de episodio.
     */
    inner class ViewHolder(val binding: ItemEpisodeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(episode: Episode, position: Int) {
            binding.tvEpisodeNumber.text = "${episode.episodeNum}."
            binding.tvEpisodeTitle.text = episode.title

            // Resalta el episodio que se está reproduciendo
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
        holder.bind(getItem(position), position)
    }

    /**
     * Actualiza el ID del episodio en reproducción y notifica los cambios
     * para que la UI se actualice y resalte el item correcto.
     */
    fun setNowPlaying(episodeId: String?) {
        val previousPlayingId = nowPlayingEpisodeId
        nowPlayingEpisodeId = episodeId

        // Notifica el cambio en el item anterior (si existía) para quitarle el resaltado.
        findPositionById(previousPlayingId)?.let { notifyItemChanged(it) }
        // Notifica el cambio en el nuevo item para añadirle el resaltado.
        findPositionById(nowPlayingEpisodeId)?.let { notifyItemChanged(it) }
    }

    private fun findPositionById(episodeId: String?): Int? {
        if (episodeId == null) return null
        return currentList.indexOfFirst { it.id == episodeId }.takeIf { it != -1 }
    }
}

/**
 * Callback para que DiffUtil calcule las diferencias entre dos listas de episodios.
 */
class EpisodeDiffCallback : DiffUtil.ItemCallback<Episode>() {
    override fun areItemsTheSame(oldItem: Episode, newItem: Episode): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Episode, newItem: Episode): Boolean {
        return oldItem == newItem
    }
}
