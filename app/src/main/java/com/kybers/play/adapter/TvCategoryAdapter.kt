package com.kybers.play.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kybers.play.R
import com.kybers.play.api.Category
import com.kybers.play.api.LiveStream
import com.kybers.play.databinding.ItemContentBinding
import com.kybers.play.databinding.ItemTvCategoryBinding

// Modelo de datos para el adaptador
data class CategoryWithChannels(
    val category: Category,
    val channels: List<LiveStream>,
    var isExpanded: Boolean = false
)

class TvCategoryAdapter(
    private val onCategoryClick: (CategoryWithChannels) -> Unit,
    private val onChannelClick: (LiveStream) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<Any>()
    private var categoriesWithChannels = listOf<CategoryWithChannels>()

    companion object {
        private const val TYPE_CATEGORY = 0
        private const val TYPE_CHANNEL = 1
    }

    fun submitList(list: List<CategoryWithChannels>) {
        categoriesWithChannels = list
        rebuildItemList()
    }

    private fun rebuildItemList() {
        items.clear()
        categoriesWithChannels.forEach { categoryItem ->
            items.add(categoryItem)
            if (categoryItem.isExpanded) {
                items.addAll(categoryItem.channels)
            }
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is CategoryWithChannels -> TYPE_CATEGORY
            is LiveStream -> TYPE_CHANNEL
            else -> throw IllegalArgumentException("Invalid type of data at position $position")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_CATEGORY -> {
                val binding = ItemTvCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                CategoryViewHolder(binding)
            }
            TYPE_CHANNEL -> {
                val binding = ItemContentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ChannelViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CategoryViewHolder -> holder.bind(items[position] as CategoryWithChannels)
            is ChannelViewHolder -> holder.bind(items[position] as LiveStream)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class CategoryViewHolder(private val binding: ItemTvCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(categoryItem: CategoryWithChannels) {
            binding.tvCategoryName.text = categoryItem.category.categoryName
            binding.ivExpandArrow.rotation = if (categoryItem.isExpanded) 180f else 0f
            itemView.setOnClickListener {
                categoryItem.isExpanded = !categoryItem.isExpanded
                rebuildItemList()
                onCategoryClick(categoryItem)
            }
        }
    }

    inner class ChannelViewHolder(private val binding: ItemContentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(channel: LiveStream) {
            binding.tvName.text = channel.name
            Glide.with(itemView.context)
                .load(channel.streamIcon)
                .placeholder(R.drawable.ic_tv)
                .into(binding.ivIcon)
            itemView.setOnClickListener {
                onChannelClick(channel)
            }
        }
    }
}
