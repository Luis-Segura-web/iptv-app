package com.kybers.play.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kybers.play.R
import com.kybers.play.api.Category
import com.kybers.play.api.LiveStream
import com.kybers.play.databinding.ItemTvCategoryBinding
import com.kybers.play.databinding.ItemTvChannelBinding
import com.kybers.play.manager.LiveFavoritesManager

// Interfaz para que el adaptador se comunique con el exterior
interface StickyHeaderInterface {
    fun getHeaderPositionForItem(itemPosition: Int): Int
    fun getHeaderLayout(headerPosition: Int): Int
    fun bindHeaderData(header: View, headerPosition: Int)
    fun isHeader(itemPosition: Int): Boolean
}

data class CategoryWithChannels(
    val category: Category,
    val channels: List<LiveStream>,
    var isExpanded: Boolean = false
)

class TvCategoryAdapter(
    private val onChannelClick: (LiveStream) -> Unit,
    private val onFavoriteClick: (LiveStream) -> Unit,
    private val onCategoryToggled: (position: Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), StickyHeaderInterface {

    private val dataList = mutableListOf<Any>()
    private var categoriesWithChannels = mutableListOf<CategoryWithChannels>()

    companion object {
        const val TYPE_CATEGORY = 0
        const val TYPE_CHANNEL = 1
    }

    fun submitList(list: List<CategoryWithChannels>) {
        categoriesWithChannels.clear()
        categoriesWithChannels.addAll(list)
        rebuildAndNotify()
    }

    private fun rebuildAndNotify() {
        val oldDataSize = dataList.size
        dataList.clear()
        notifyItemRangeRemoved(0, oldDataSize)

        categoriesWithChannels.forEach { categoryItem ->
            dataList.add(categoryItem)
            if (categoryItem.isExpanded) {
                dataList.addAll(categoryItem.channels)
            }
        }
        notifyItemRangeInserted(0, dataList.size)
    }

    override fun getItemViewType(position: Int): Int {
        return when (dataList[position]) {
            is CategoryWithChannels -> TYPE_CATEGORY
            is LiveStream -> TYPE_CHANNEL
            else -> throw IllegalArgumentException("Invalid type of data at position $position")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_CATEGORY -> CategoryViewHolder(ItemTvCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            TYPE_CHANNEL -> ChannelViewHolder(ItemTvChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CategoryViewHolder -> holder.bind(dataList[position] as CategoryWithChannels)
            is ChannelViewHolder -> holder.bind(dataList[position] as LiveStream)
        }
    }

    override fun getItemCount(): Int = dataList.size

    // --- Implementación de StickyHeaderInterface ---
    override fun getHeaderPositionForItem(itemPosition: Int): Int {
        var headerPosition = 0
        var currentPosition = itemPosition
        do {
            if (isHeader(currentPosition)) {
                headerPosition = currentPosition
                break
            }
            currentPosition -= 1
        } while (currentPosition >= 0)
        return headerPosition
    }

    override fun getHeaderLayout(headerPosition: Int): Int = R.layout.item_tv_category

    override fun bindHeaderData(header: View, headerPosition: Int) {
        val categoryItem = dataList[headerPosition] as CategoryWithChannels
        header.findViewById<android.widget.TextView>(R.id.tv_category_name).text = categoryItem.category.categoryName
    }

    override fun isHeader(itemPosition: Int): Boolean {
        if (itemPosition < 0 || itemPosition >= dataList.size) return false
        return dataList[itemPosition] is CategoryWithChannels
    }

    // --- ViewHolders ---
    inner class CategoryViewHolder(private val binding: ItemTvCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(categoryItem: CategoryWithChannels) {
            binding.tvCategoryName.text = categoryItem.category.categoryName
            binding.ivExpandArrow.rotation = if (categoryItem.isExpanded) 180f else 0f

            itemView.setOnClickListener {
                val clickedPosition = bindingAdapterPosition
                if (clickedPosition == RecyclerView.NO_POSITION) return@setOnClickListener

                val currentlyExpanded = categoryItem.isExpanded

                // Cerrar cualquier otra categoría que esté abierta
                var previouslyExpandedPosition = -1
                categoriesWithChannels.forEachIndexed { index, item ->
                    if (item.isExpanded) {
                        previouslyExpandedPosition = dataList.indexOf(item)
                        item.isExpanded = false
                    }
                }

                // Expandir o contraer la categoría actual
                categoryItem.isExpanded = !currentlyExpanded

                rebuildAndNotify()
                onCategoryToggled(clickedPosition)
            }
        }
    }

    inner class ChannelViewHolder(private val binding: ItemTvChannelBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(channel: LiveStream) {
            binding.tvChannelName.text = channel.name
            Glide.with(itemView.context).load(channel.streamIcon).placeholder(R.drawable.ic_tv).into(binding.ivChannelIcon)
            val isFavorite = LiveFavoritesManager.isFavorite(itemView.context, channel)
            binding.btnFavorite.setImageResource(if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
            binding.btnFavorite.setOnClickListener { onFavoriteClick(channel) }
            itemView.setOnClickListener { onChannelClick(channel) }
        }
    }
}
