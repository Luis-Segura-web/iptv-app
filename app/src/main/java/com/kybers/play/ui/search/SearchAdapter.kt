package com.kybers.play.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kybers.play.R
import com.kybers.play.api.LiveStream
import com.kybers.play.api.Movie
import com.kybers.play.api.PlayableContent
import com.kybers.play.api.Series
import com.kybers.play.databinding.ItemContentBinding

/**
 * Adaptador para la lista de resultados de búsqueda.
 * Es capaz de mostrar diferentes tipos de contenido (LiveStream, Movie, Series)
 * en una única lista, usando el mismo layout de item.
 */
class SearchAdapter(
    private val onResultClick: (PlayableContent) -> Unit
) : ListAdapter<PlayableContent, SearchAdapter.ViewHolder>(SearchDiffCallback()) {

    /**
     * ViewHolder que contiene la vista de un solo resultado de búsqueda.
     */
    inner class ViewHolder(private val binding: ItemContentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PlayableContent) {
            binding.tvName.text = item.getTitle()

            // Determina el tipo de contenido para mostrarlo en el subtítulo
            // y cargar la imagen y el placeholder correctos.
            val placeholder: Int
            when (item) {
                is LiveStream -> {
                    binding.tvType.text = "Canal en Vivo"
                    placeholder = R.drawable.ic_tv
                }
                is Movie -> {
                    binding.tvType.text = "Película"
                    placeholder = R.drawable.ic_movie
                }
                is Series -> {
                    binding.tvType.text = "Serie"
                    placeholder = R.drawable.ic_series
                }
                else -> {
                    binding.tvType.text = ""
                    placeholder = R.drawable.ic_logo_placeholder
                }
            }

            Glide.with(binding.ivIcon.context)
                .load(item.getCoverUrl())
                .placeholder(placeholder)
                .error(placeholder) // Muestra el placeholder también si hay un error al cargar
                .into(binding.ivIcon)

            binding.root.setOnClickListener { onResultClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemContentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

/**
 * Callback para que DiffUtil calcule las diferencias entre dos listas de resultados de búsqueda.
 * Compara items usando la interfaz PlayableContent.
 */
class SearchDiffCallback : DiffUtil.ItemCallback<PlayableContent>() {
    override fun areItemsTheSame(oldItem: PlayableContent, newItem: PlayableContent): Boolean {
        // Los items son los mismos si su ID y tipo son iguales.
        return oldItem.getContentId() == newItem.getContentId() && oldItem.getType() == newItem.getType()
    }

    override fun areContentsTheSame(oldItem: PlayableContent, newItem: PlayableContent): Boolean {
        // El contenido es el mismo si los objetos son idénticos.
        return oldItem == newItem
    }
}
