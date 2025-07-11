package com.kybers.play.ui.home.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kybers.play.R
import com.kybers.play.adapter.ContentAdapter
import com.kybers.play.adapter.MoviesAdapter
import com.kybers.play.adapter.SeriesAdapter
import com.kybers.play.api.LiveStream
import com.kybers.play.api.Movie
import com.kybers.play.api.Series
import com.kybers.play.databinding.ItemContinueWatchingBinding
import com.kybers.play.databinding.ItemHomeCategoryBinding
import com.kybers.play.manager.HistoryItem
import com.kybers.play.ui.home.HomeCategory

class HomeAdapter(
    private val context: Context,
    private val categories: List<HomeCategory>,
    private val onAnyItemClick: (Any) -> Unit, // Un solo listener para cualquier tipo de item
    private val onHistoryItemClick: (HistoryItem) -> Unit
) : RecyclerView.Adapter<HomeAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemHomeCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(homeCategory: HomeCategory) {
            binding.tvCategoryTitle.text = homeCategory.title
            binding.recyclerViewHorizontal.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

            // Decidimos qué adaptador usar según el tipo de contenido de la categoría
            when {
                homeCategory.title == "Continuar Viendo" -> {
                    val historyItems = homeCategory.items.filterIsInstance<HistoryItem>()
                    binding.recyclerViewHorizontal.adapter = ContinueWatchingAdapter(historyItems, onHistoryItemClick)
                }
                homeCategory.items.all { it is Series } -> {
                    val series = homeCategory.items.filterIsInstance<Series>()
                    binding.recyclerViewHorizontal.adapter = SeriesAdapter(series.toMutableList()) { onAnyItemClick(it) }
                }
                homeCategory.items.all { it is Movie } -> {
                    val movies = homeCategory.items.filterIsInstance<Movie>()
                    binding.recyclerViewHorizontal.adapter = MoviesAdapter(movies.toMutableList()) { onAnyItemClick(it) }
                }
                homeCategory.items.all { it is LiveStream } -> {
                    val streams = homeCategory.items.filterIsInstance<LiveStream>()
                    binding.recyclerViewHorizontal.adapter = ContentAdapter(streams) { stream, _ -> onAnyItemClick(stream) }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount() = categories.size
}

/**
 * Adaptador interno, especializado para el carrusel de "Continuar Viendo".
 * Muestra una barra de progreso sobre la portada del contenido.
 */
class ContinueWatchingAdapter(
    private val items: List<HistoryItem>,
    private val onClick: (HistoryItem) -> Unit
) : RecyclerView.Adapter<ContinueWatchingAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemContinueWatchingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HistoryItem) {
            Glide.with(binding.ivPoster.context)
                .load(item.content.getCoverUrl())
                .placeholder(R.drawable.ic_movie) // Usamos un placeholder genérico
                .into(binding.ivPoster)

            // Mostramos la barra de progreso solo si hay una duración válida
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
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}
