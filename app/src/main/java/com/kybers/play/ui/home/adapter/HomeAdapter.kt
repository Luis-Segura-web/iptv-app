package com.kybers.play.ui.home.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kybers.play.R
import com.kybers.play.adapter.MoviesAdapter
import com.kybers.play.adapter.SeriesAdapter
import com.kybers.play.api.Movie
import com.kybers.play.api.Series
import com.kybers.play.databinding.ItemContinueWatchingBinding
import com.kybers.play.databinding.ItemHomeCategoryBinding
import com.kybers.play.manager.HistoryItem
import com.kybers.play.ui.home.HomeCategory

/**
 * Adaptador principal para la lista vertical de categorías en la pantalla de inicio.
 * Cada item de este adaptador es una fila horizontal de contenido.
 */
class HomeAdapter(
    private val onMovieClick: (Movie) -> Unit,
    private val onSeriesClick: (Series) -> Unit,
    private val onHistoryItemClick: (HistoryItem) -> Unit
) : ListAdapter<HomeCategory, HomeAdapter.ViewHolder>(HomeCategoryDiffCallback()) {

    /**
     * ViewHolder para una fila completa (título + RecyclerView horizontal).
     */
    inner class ViewHolder(private val binding: ItemHomeCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(homeCategory: HomeCategory) {
            binding.tvCategoryTitle.text = homeCategory.title
            binding.recyclerViewHorizontal.layoutManager = LinearLayoutManager(binding.root.context, LinearLayoutManager.HORIZONTAL, false)
            binding.recyclerViewHorizontal.setHasFixedSize(true)

            // Decide qué adaptador usar para la lista horizontal según el tipo de contenido.
            when {
                homeCategory.title == "Continuar Viendo" -> {
                    val historyItems = homeCategory.items.filterIsInstance<HistoryItem>()
                    val continueWatchingAdapter = ContinueWatchingAdapter(onHistoryItemClick)
                    binding.recyclerViewHorizontal.adapter = continueWatchingAdapter
                    continueWatchingAdapter.submitList(historyItems)
                }
                homeCategory.items.all { it is Series } -> {
                    val series = homeCategory.items.filterIsInstance<Series>()
                    val seriesAdapter = SeriesAdapter(onSeriesClick)
                    binding.recyclerViewHorizontal.adapter = seriesAdapter
                    seriesAdapter.submitList(series)
                }
                homeCategory.items.all { it is Movie } -> {
                    val movies = homeCategory.items.filterIsInstance<Movie>()
                    val moviesAdapter = MoviesAdapter(onMovieClick)
                    binding.recyclerViewHorizontal.adapter = moviesAdapter
                    moviesAdapter.submitList(movies)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

/**
 * Adaptador especializado para la fila "Continuar Viendo".
 */
class ContinueWatchingAdapter(
    private val onClick: (HistoryItem) -> Unit
) : ListAdapter<HistoryItem, ContinueWatchingAdapter.ViewHolder>(HistoryItemDiffCallback()) {

    inner class ViewHolder(val binding: ItemContinueWatchingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HistoryItem) {
            Glide.with(binding.ivPoster.context)
                .load(item.content.getCoverUrl())
                .placeholder(R.drawable.ic_movie)
                .into(binding.ivPoster)

            if (item.duration > 0) {
                binding.progressBar.visibility = View.VISIBLE
                binding.progressBar.max = item.duration.toInt()
                binding.progressBar.progress = item.lastPosition.toInt()
            } else {
                binding.progressBar.visibility = View.GONE
            }
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemContinueWatchingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

// --- Callbacks de DiffUtil para un rendimiento eficiente ---

class HomeCategoryDiffCallback : DiffUtil.ItemCallback<HomeCategory>() {
    override fun areItemsTheSame(oldItem: HomeCategory, newItem: HomeCategory): Boolean {
        return oldItem.title == newItem.title
    }
    override fun areContentsTheSame(oldItem: HomeCategory, newItem: HomeCategory): Boolean {
        // CORREGIDO: Comparar el contenido de las listas, no las listas en sí, es más robusto.
        return oldItem.items.size == newItem.items.size && oldItem.items.toSet() == newItem.items.toSet()
    }
}

class HistoryItemDiffCallback : DiffUtil.ItemCallback<HistoryItem>() {
    override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
        return oldItem.content.getContentId() == newItem.content.getContentId() && oldItem.content.getType() == newItem.content.getType()
    }
    override fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
        return oldItem == newItem
    }
}
