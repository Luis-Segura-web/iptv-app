package com.kybers.play.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kybers.play.R
import com.kybers.play.api.Category
import com.kybers.play.api.LiveStream
import com.kybers.play.databinding.ItemTvCategoryBinding
import com.kybers.play.databinding.ItemTvChannelBinding
import com.kybers.play.manager.LiveFavoritesManager

data class CategoryWithChannels(
    val category: Category,
    val channels: List<LiveStream>,
    var isExpanded: Boolean = false
)

class TvCategoryAdapter(
    private val onChannelClick: (LiveStream) -> Unit,
    private val onFavoriteClick: (LiveStream) -> Unit // Nuevo listener para favoritos
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
                // CORREGIDO: Usamos el nuevo layout para los canales
                val binding = ItemTvChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
            }
        }
    }

    // CORREGIDO: ViewHolder para usar el nuevo layout y manejar favoritos
    inner class ChannelViewHolder(private val binding: ItemTvChannelBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(channel: LiveStream) {
            binding.tvChannelName.text = channel.name
            Glide.with(itemView.context)
                .load(channel.streamIcon)
                .placeholder(R.drawable.ic_tv)
                .into(binding.ivChannelIcon)

            // Actualizar el icono de favorito
            val isFavorite = LiveFavoritesManager.isFavorite(itemView.context, channel)
            binding.btnFavorite.setImageResource(
                if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            )

            // Listener para el botón de favorito
            binding.btnFavorite.setOnClickListener {
                onFavoriteClick(channel)
            }

            // Listener para toda la fila (para reproducir)
            itemView.setOnClickListener {
                onChannelClick(channel)
            }
        }
    }
}
