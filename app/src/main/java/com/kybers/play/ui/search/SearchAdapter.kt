package com.kybers.play.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kybers.play.R
import com.kybers.play.api.LiveStream
import com.kybers.play.api.Movie
import com.kybers.play.api.Series
import com.kybers.play.databinding.ItemContentBinding
import com.kybers.play.databinding.ItemMovieBinding

class SearchAdapter(
    private var items: List<Any>,
    private val onResultClick: (Any) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_LIVE_STREAM = 0
    private val TYPE_MOVIE = 1
    private val TYPE_SERIES = 2

    inner class LiveStreamViewHolder(val binding: ItemContentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LiveStream) {
            binding.tvName.text = item.name
            Glide.with(binding.ivIcon.context).load(item.streamIcon).placeholder(R.drawable.ic_tv).into(binding.ivIcon)
            binding.root.setOnClickListener { onResultClick(item) }
        }
    }

    inner class MediaViewHolder(val binding: ItemMovieBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Any) {
            when (item) {
                is Movie -> {
                    binding.tvMovieTitle.text = item.name
                    Glide.with(binding.ivMoviePoster.context).load(item.streamIcon).placeholder(R.drawable.ic_movie).into(binding.ivMoviePoster)
                }
                is Series -> {
                    binding.tvMovieTitle.text = item.name
                    Glide.with(binding.ivMoviePoster.context).load(item.cover).placeholder(R.drawable.ic_series).into(binding.ivMoviePoster)
                }
            }
            binding.root.setOnClickListener { onResultClick(item) }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is LiveStream -> TYPE_LIVE_STREAM
            is Movie -> TYPE_MOVIE
            is Series -> TYPE_SERIES
            else -> throw IllegalArgumentException("Tipo de item desconocido")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_LIVE_STREAM -> LiveStreamViewHolder(ItemContentBinding.inflate(inflater, parent, false))
            TYPE_MOVIE, TYPE_SERIES -> MediaViewHolder(ItemMovieBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Tipo de vista desconocido")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is LiveStreamViewHolder -> holder.bind(items[position] as LiveStream)
            is MediaViewHolder -> holder.bind(items[position])
        }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<Any>) {
        items = newItems
        notifyDataSetChanged()
    }
}
