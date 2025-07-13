package com.kybers.play.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
    private var allCategoriesWithChannels = mutableListOf<CategoryWithChannels>()
    private var nowPlayingStreamId: Int? = null
    private var activeCategoryId: String? = null
    private val favoritesCategoryId = "favorites"

    companion object {
        const val TYPE_CATEGORY = 0
        const val TYPE_CHANNEL = 1
        const val TYPE_EMPTY = 2
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

    // CORREGIDO: Lógica de favoritos para no recargar toda la lista.
    fun toggleFavoriteStatus(channel: LiveStream) {
        val isCurrentlyFavorite = LiveFavoritesManager.isFavorite(context, channel)

        // 1. Actualiza el estado persistente en el manager.
        if (isCurrentlyFavorite) {
            LiveFavoritesManager.removeFavorite(context, channel)
        } else {
            LiveFavoritesManager.addFavorite(context, channel)
        }

        // 2. Actualiza el icono de la estrella en TODAS las apariciones visibles del canal.
        dataList.forEachIndexed { index, item ->
            if (item is LiveStream && item.streamId == channel.streamId) {
                notifyItemChanged(index)
            }
        }

        // 3. Añade o quita el canal de la lista de la categoría "Favoritos" de forma precisa.
        updateFavoritesCategoryList(isCurrentlyFavorite, channel)
    }

    private fun updateFavoritesCategoryList(wasFavorite: Boolean, channel: LiveStream) {
        val favoritesCategory = allCategoriesWithChannels.firstOrNull { it.category.categoryId == favoritesCategoryId } ?: return

        if (wasFavorite) {
            // El canal ERA favorito, ahora hay que QUITARLO de la lista de favoritos.
            val indexInFavorites = favoritesCategory.channels.indexOfFirst { it.streamId == channel.streamId }
            if (indexInFavorites != -1) {
                // Lo quitamos de la lista de datos del adaptador si está visible.
                if (favoritesCategory.isExpanded) {
                    val headerPos = dataList.indexOf(favoritesCategory)
                    val globalPos = headerPos + 1 + indexInFavorites
                    if (globalPos < dataList.size && dataList[globalPos] is LiveStream) {
                        dataList.removeAt(globalPos)
                        notifyItemRemoved(globalPos)
                    }
                }
                // Lo quitamos de la lista de canales interna de la categoría.
                favoritesCategory.channels.removeAt(indexInFavorites)

                // Si la categoría de favoritos queda vacía y está expandida, mostramos el mensaje.
                if (favoritesCategory.channels.isEmpty() && favoritesCategory.isExpanded) {
                    val headerPos = dataList.indexOf(favoritesCategory)
                    if (headerPos != -1) {
                        dataList.add(headerPos + 1, EmptyState(favoritesCategoryId))
                        notifyItemInserted(headerPos + 1)
                    }
                }
            }
        } else {
            // El canal NO ERA favorito, ahora hay que AÑADIRLO.
            if (!favoritesCategory.channels.any { it.streamId == channel.streamId }) {
                val wasEmpty = favoritesCategory.channels.isEmpty()
                favoritesCategory.channels.add(0, channel) // Añadir al principio de la lista de favoritos.

                if (favoritesCategory.isExpanded) {
                    val headerPos = dataList.indexOf(favoritesCategory)
                    if (headerPos != -1) {
                        // Si estaba vacía, quitamos el mensaje de "vacío".
                        if (wasEmpty) {
                            val emptyPos = headerPos + 1
                            if (emptyPos < dataList.size && dataList[emptyPos] is EmptyState) {
                                dataList.removeAt(emptyPos)
                                notifyItemRemoved(emptyPos)
                            }
                        }
                        // Insertamos el nuevo canal favorito en la lista visible.
                        dataList.add(headerPos + 1, channel)
                        notifyItemInserted(headerPos + 1)
                    }
                }
            }
        }
    }

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
        return dataList.indexOfFirst { it is LiveStream && it.streamId == streamId }.takeIf { it != -1 }
    }

    private fun findPositionByCategoryId(categoryId: String?): Int? {
        if (categoryId == null) return null
        return dataList.indexOfFirst { it is CategoryWithChannels && it.category.categoryId == categoryId }.takeIf { it != -1 }
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
                if (categoryItem.channels.isEmpty()) {
                    dataList.add(EmptyState(categoryItem.category.categoryId))
                } else {
                    dataList.addAll(categoryItem.channels)
                }
            }
        }
    }

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
            is EmptyState -> TYPE_EMPTY
            else -> throw IllegalArgumentException("Invalid type of data at position $position")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_CATEGORY -> CategoryViewHolder(ItemTvCategoryBinding.inflate(inflater, parent, false))
            TYPE_CHANNEL -> ChannelViewHolder(ItemTvChannelBinding.inflate(inflater, parent, false))
            TYPE_EMPTY -> EmptyViewHolder(inflater.inflate(R.layout.item_tv_empty, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CategoryViewHolder -> holder.bind(dataList[position] as CategoryWithChannels)
            is ChannelViewHolder -> holder.bind(dataList[position] as LiveStream)
            is EmptyViewHolder -> holder.bind(dataList[position] as EmptyState)
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
        val tvCategoryName = header.findViewById<TextView>(R.id.tv_category_name)
        val ivExpandArrow = header.findViewById<android.widget.ImageView>(R.id.iv_expand_arrow)
        tvCategoryName.text = categoryItem.category.categoryName
        ivExpandArrow.rotation = if (categoryItem.isExpanded) 180f else 0f
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

    data class EmptyState(val parentCategoryId: String)
    inner class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(state: EmptyState) {
            val messageView = itemView.findViewById<TextView>(R.id.tv_empty_message)
            messageView.text = if (state.parentCategoryId == favoritesCategoryId) {
                "Añade canales a favoritos para verlos aquí."
            } else {
                "No hay canales en esta categoría."
            }
        }
    }
}
