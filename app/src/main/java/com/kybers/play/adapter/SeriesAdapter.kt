package com.kybers.play.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kybers.play.R
import com.kybers.play.api.Series
import com.kybers.play.databinding.ItemMovieBinding // Reutilizamos el mismo layout

/**
 * Adaptador para mostrar una lista de series en un RecyclerView.
 * Al igual que MoviesAdapter, usa ListAdapter con DiffUtil para un rendimiento óptimo.
 */
class SeriesAdapter(
    private val onItemClick: (Series) -> Unit
) : ListAdapter<Series, SeriesAdapter.ViewHolder>(SeriesDiffCallback()) {

    /**
     * ViewHolder que contiene la vista de un solo item de serie.
     * Reutiliza ItemMovieBinding.
     */
    inner class ViewHolder(val binding: ItemMovieBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(series: Series) {
            binding.tvMovieTitle.text = series.name
            // La diferencia principal es que cargamos la imagen desde 'series.cover'
            Glide.with(binding.ivMoviePoster.context)
                .load(series.cover)
                .placeholder(R.drawable.ic_series) // Usamos un placeholder específico para series
                .into(binding.ivMoviePoster)

            binding.root.setOnClickListener { onItemClick(series) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMovieBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

/**
 * Callback para que DiffUtil calcule las diferencias entre dos listas de series.
 */
class SeriesDiffCallback : DiffUtil.ItemCallback<Series>() {
    override fun areItemsTheSame(oldItem: Series, newItem: Series): Boolean {
        // Los items son los mismos si sus seriesId son iguales.
        return oldItem.seriesId == newItem.seriesId
    }

    override fun areContentsTheSame(oldItem: Series, newItem: Series): Boolean {
        // El contenido es el mismo si los objetos son idénticos.
        return oldItem == newItem
    }
}
