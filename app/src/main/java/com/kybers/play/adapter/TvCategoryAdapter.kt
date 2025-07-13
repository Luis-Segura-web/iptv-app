package com.kybers.play.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kybers.play.R
import com.kybers.play.api.Category
import com.kybers.play.api.LiveStream
import com.kybers.play.databinding.ItemTvCategoryBinding
import com.kybers.play.databinding.ItemTvChannelBinding
import com.kybers.play.manager.LiveFavoritesManager

interface StickyHeaderInterface {
    fun getHeaderPositionForItem(itemPosition: Int): Int
    fun getHeaderLayout(headerPosition: Int): Int
    fun bindHeaderData(header: View, headerPosition: Int)
    fun isHeader(itemPosition: Int): Boolean
}

data class CategoryWithChannels(
    val category: Category,
    val channels: MutableList<LiveStream>,
    var isExpanded: Boolean = false
)

class TvCategoryAdapter(
    private val context: Context,
    private val onChannelClick: (LiveStream) -> Unit,
    private val onCategoryToggled: (position: Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), StickyHeaderInterface {

    private val dataList = mutableListOf<Any>()
    private var categoriesWithChannels = mutableListOf<CategoryWithChannels>()
    private var allCategoriesWithChannels = mutableListOf<CategoryWithChannels>() // Copia de seguridad para el filtro
    private var nowPlayingStreamId: Int? = null
    private var activeCategoryId: String? = null // AÑADIDO: Para resaltar la categoría
    private val favoritesCategoryId = "favorites"

    companion object {
        const val TYPE_CATEGORY = 0
        const val TYPE_CHANNEL = 1
    }

    fun handleCategoryClick(clickedPosition: Int) {
        if (clickedPosition < 0 || clickedPosition >= dataList.size) return
        val clickedItem = dataList[clickedPosition] as? CategoryWithChannels ?: return
        val wasExpanded = clickedItem.isExpanded
        categoriesWithChannels.forEach { it.isExpanded = false }
        if (!wasExpanded) clickedItem.isExpanded = true
        rebuildDataList()
        notifyDataSetChanged()
        val newPosition = dataList.indexOf(clickedItem)
        if (newPosition != -1) onCategoryToggled(newPosition)
    }

    fun toggleFavoriteStatus(channel: LiveStream) {
        val isCurrentlyFavorite = LiveFavoritesManager.isFavorite(context, channel)
        if (isCurrentlyFavorite) LiveFavoritesManager.removeFavorite(context, channel) else LiveFavoritesManager.addFavorite(context, channel)
        val originalPosition = dataList.indexOfFirst { it is LiveStream && it.streamId == channel.streamId }
        if (originalPosition != -1) notifyItemChanged(originalPosition)
        var favoritesCategory = categoriesWithChannels.firstOrNull { it.category.categoryId == favoritesCategoryId }
        if (isCurrentlyFavorite) {
            favoritesCategory?.let { favCat ->
                val indexInFavorites = favCat.channels.indexOfFirst { it.streamId == channel.streamId }
                if (indexInFavorites != -1) {
                    val globalPosition = dataList.indexOf(favCat.channels[indexInFavorites])
                    favCat.channels.removeAt(indexInFavorites)
                    dataList.removeAt(globalPosition)
                    notifyItemRemoved(globalPosition)
                    if (favCat.channels.isEmpty()) {
                        val categoryIndex = categoriesWithChannels.indexOf(favCat)
                        categoriesWithChannels.removeAt(categoryIndex)
                        dataList.removeAt(0)
                        notifyItemRemoved(0)
                    }
                }
            }
        } else {
            if (favoritesCategory == null) {
                val favCategoryInfo = Category(categoryId = favoritesCategoryId, categoryName = "⭐ Favoritos", parentId = 0)
                favoritesCategory = CategoryWithChannels(favCategoryInfo, mutableListOf(), isExpanded = true)
                categoriesWithChannels.add(0, favoritesCategory)
                dataList.add(0, favoritesCategory)
                notifyItemInserted(0)
            }
            if (!favoritesCategory.channels.any { it.streamId == channel.streamId }) {
                favoritesCategory.channels.add(0, channel)
                dataList.add(1, channel)
                notifyItemInserted(1)
            }
        }
    }

    // CORREGIDO: Ahora acepta el ID de la categoría para resaltarla.
    fun setNowPlaying(streamId: Int?, categoryId: String?) {
        val previousPlayingId = nowPlayingStreamId
        val previousCategoryId = activeCategoryId
        nowPlayingStreamId = streamId
        activeCategoryId = categoryId

        findPositionByStreamId(previousPlayingId)?.let { notifyItemChanged(it) }
        findPositionByStreamId(nowPlayingStreamId)?.let { notifyItemChanged(it) }

        findPositionByCategoryId(previousCategoryId)?.let { notifyItemChanged(it) }
        findPositionByCategoryId(activeCategoryId)?.let { notifyItemChanged(it) }
    }

    private fun findPositionByStreamId(streamId: Int?): Int? {
        if (streamId == null) return null
        val position = dataList.indexOfFirst { it is LiveStream && it.streamId == streamId }
        return if (position != -1) position else null
    }

    // AÑADIDO: Función de ayuda para encontrar la posición de una categoría.
    private fun findPositionByCategoryId(categoryId: String?): Int? {
        if (categoryId == null) return null
        val position = dataList.indexOfFirst { it is CategoryWithChannels && it.category.categoryId == categoryId }
        return if (position != -1) position else null
    }

    fun submitList(list: List<CategoryWithChannels>) {
        allCategoriesWithChannels.clear()
        allCategoriesWithChannels.addAll(list.map { it.copy(channels = it.channels.toMutableList()) })
        categoriesWithChannels.clear()
        categoriesWithChannels.addAll(allCategoriesWithChannels)
        rebuildDataList()
        notifyDataSetChanged()
    }

    private fun rebuildDataList() {
        dataList.clear()
        categoriesWithChannels.forEach { categoryItem ->
            dataList.add(categoryItem)
            if (categoryItem.isExpanded) {
                dataList.addAll(categoryItem.channels)
            }
        }
    }

    // AÑADIDO: Función de filtro.
    fun filter(query: String?) {
        val lowerCaseQuery = query?.lowercase()?.trim()

        if (lowerCaseQuery.isNullOrEmpty()) {
            categoriesWithChannels.clear()
            categoriesWithChannels.addAll(allCategoriesWithChannels)
        } else {
            val filteredList = allCategoriesWithChannels.mapNotNull { category ->
                val matchingChannels = category.channels.filter {
                    it.name.lowercase().contains(lowerCaseQuery)
                }.toMutableList()

                if (matchingChannels.isNotEmpty() || category.category.categoryName.lowercase().contains(lowerCaseQuery)) {
                    category.copy(channels = matchingChannels)
                } else {
                    null
                }
            }
            categoriesWithChannels.clear()
            categoriesWithChannels.addAll(filteredList)
        }
        rebuildDataList()
        notifyDataSetChanged()
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

    override fun getHeaderPositionForItem(itemPosition: Int): Int {
        var tempPosition = itemPosition
        var headerPosition = RecyclerView.NO_POSITION
        do {
            if (isHeader(tempPosition)) {
                headerPosition = tempPosition
                break
            }
            tempPosition -= 1
        } while (tempPosition >= 0)
        return headerPosition
    }

    override fun getHeaderLayout(headerPosition: Int): Int = R.layout.item_tv_category

    override fun bindHeaderData(header: View, headerPosition: Int) {
        val categoryItem = dataList[headerPosition] as CategoryWithChannels
        val tvCategoryName = header.findViewById<android.widget.TextView>(R.id.tv_category_name)
        val ivExpandArrow = header.findViewById<android.widget.ImageView>(R.id.iv_expand_arrow)
        tvCategoryName.text = categoryItem.category.categoryName
        ivExpandArrow.rotation = if (categoryItem.isExpanded) 180f else 0f

        // AÑADIDO: Resaltar también el encabezado pegajoso.
        if (categoryItem.category.categoryId == activeCategoryId) {
            header.setBackgroundColor(ContextCompat.getColor(context, R.color.now_playing_category_background))
        } else {
            header.setBackgroundColor(ContextCompat.getColor(context, R.color.dark_surface))
        }
    }

    override fun isHeader(itemPosition: Int): Boolean {
        if (itemPosition < 0 || itemPosition >= dataList.size) return false
        return dataList[itemPosition] is CategoryWithChannels
    }

    inner class CategoryViewHolder(private val binding: ItemTvCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    handleCategoryClick(bindingAdapterPosition)
                }
            }
        }

        fun bind(categoryItem: CategoryWithChannels) {
            binding.tvCategoryName.text = categoryItem.category.categoryName
            binding.ivExpandArrow.rotation = if (categoryItem.isExpanded) 180f else 0f

            // AÑADIDO: Lógica para resaltar el fondo de la categoría.
            if (categoryItem.category.categoryId == activeCategoryId) {
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.now_playing_category_background))
            } else {
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.dark_surface))
            }
        }
    }

    inner class ChannelViewHolder(private val binding: ItemTvChannelBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(channel: LiveStream) {
            binding.tvChannelName.text = channel.name
            Glide.with(itemView.context).load(channel.streamIcon).placeholder(R.drawable.ic_tv).into(binding.ivChannelIcon)
            val isFavorite = LiveFavoritesManager.isFavorite(itemView.context, channel)
            binding.btnFavorite.setImageResource(if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
            binding.btnFavorite.setOnClickListener { toggleFavoriteStatus(channel) }
            itemView.setOnClickListener { onChannelClick(channel) }

            if (channel.streamId == nowPlayingStreamId) {
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.now_playing_background))
            } else {
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.transparent))
            }
        }
    }
}
