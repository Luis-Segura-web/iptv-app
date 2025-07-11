package com.kybers.play.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kybers.play.R
import com.kybers.play.api.LiveStream
import com.kybers.play.databinding.ItemContentBinding

class ContentAdapter(
    private val streams: List<LiveStream>,
    // El listener ahora siempre devuelve la posición (Int) del item
    private val onItemClick: (LiveStream, Int) -> Unit
) : RecyclerView.Adapter<ContentAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemContentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(stream: LiveStream, position: Int) {
            binding.tvName.text = stream.name
            Glide.with(binding.ivIcon.context)
                .load(stream.streamIcon)
                .placeholder(R.mipmap.ic_launcher)
                .into(binding.ivIcon)

            binding.root.setOnClickListener { onItemClick(stream, position) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemContentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(streams[position], position)
    }

    override fun getItemCount() = streams.size
}
