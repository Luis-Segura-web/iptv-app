package com.kybers.play.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kybers.play.R
import com.kybers.play.api.Movie
import com.kybers.play.databinding.ItemMovieBinding

class MoviesAdapter(
    private var movies: MutableList<Movie>, // Cambiado a MutableList
    private val onItemClick: (Movie) -> Unit
) : RecyclerView.Adapter<MoviesAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemMovieBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(movie: Movie) {
            binding.tvMovieTitle.text = movie.name
            Glide.with(binding.ivMoviePoster.context)
                .load(movie.streamIcon)
                .placeholder(R.drawable.ic_movie)
                .into(binding.ivMoviePoster)

            binding.root.setOnClickListener { onItemClick(movie) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMovieBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(movies[position])
    }

    override fun getItemCount() = movies.size

    // CORREGIDO: Usamos DiffUtil para actualizaciones eficientes
    fun updateMovies(newMoviesList: List<Movie>) {
        val diffCallback = MovieDiffCallback(this.movies, newMoviesList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.movies.clear()
        this.movies.addAll(newMoviesList)
        diffResult.dispatchUpdatesTo(this)
    }
}

// Clase para calcular las diferencias entre la lista vieja y la nueva
class MovieDiffCallback(
    private val oldList: List<Movie>,
    private val newList: List<Movie>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Comparamos por ID, que es único para cada película
        return oldList[oldItemPosition].streamId == newList[newItemPosition].streamId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Comparamos el contenido completo del objeto
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
