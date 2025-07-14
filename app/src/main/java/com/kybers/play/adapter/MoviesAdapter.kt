package com.kybers.play.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kybers.play.R
import com.kybers.play.api.Movie
import com.kybers.play.databinding.ItemMovieBinding

/**
 * Adaptador para mostrar una lista de películas en un RecyclerView.
 * Utiliza ListAdapter con DiffUtil para manejar eficientemente las actualizaciones de la lista.
 */
class MoviesAdapter(
    private val onItemClick: (Movie) -> Unit
) : ListAdapter<Movie, MoviesAdapter.ViewHolder>(MovieDiffCallback()) {

    /**
     * ViewHolder que contiene la vista de un solo item de película.
     */
    inner class ViewHolder(val binding: ItemMovieBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(movie: Movie) {
            binding.tvMovieTitle.text = movie.name
            Glide.with(binding.ivMoviePoster.context)
                .load(movie.streamIcon)
                .placeholder(R.drawable.ic_movie) // Imagen por defecto mientras carga
                .into(binding.ivMoviePoster)

            binding.root.setOnClickListener { onItemClick(movie) }
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
 * Callback para que DiffUtil calcule las diferencias entre dos listas de películas.
 * Esto permite animaciones y actualizaciones eficientes sin tener que recargar toda la lista.
 */
class MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
    override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean {
        // Los items son los mismos si sus IDs son iguales.
        return oldItem.streamId == newItem.streamId
    }

    override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean {
        // El contenido es el mismo si los objetos son idénticos.
        return oldItem == newItem
    }
}
