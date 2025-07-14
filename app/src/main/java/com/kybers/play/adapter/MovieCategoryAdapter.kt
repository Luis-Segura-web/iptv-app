package com.kybers.play.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kybers.play.R
import com.kybers.play.api.Movie
import com.kybers.play.databinding.ItemMovieBinding
import com.kybers.play.databinding.ItemTvCategoryBinding

class MovieCategoryAdapter(
    private val onMovieClick: (Movie) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), StickyHeaderInterface {

    private val displayList = mutableListOf<Any>()
    private var sourceList = mutableListOf<CategoryWithMovies>()
    private var filteredList = mutableListOf<CategoryWithMovies>()

    companion object {
        private const val TYPE_CATEGORY = 0
        private const val TYPE_MOVIE = 1
    }

    fun submitList(list: List<CategoryWithMovies>) {
        sourceList.clear()
        sourceList.addAll(list)
        filteredList.clear()
        filteredList.addAll(list)
        rebuildDisplayList()
        notifyDataSetChanged()
    }

    fun filter(query: String?) {
        val lowerCaseQuery = query?.lowercase()?.trim()
        if (lowerCaseQuery.isNullOrEmpty()) {
            filteredList.clear()
            filteredList.addAll(sourceList)
        } else {
            val result = sourceList.mapNotNull { category ->
                val matchingMovies = category.movies.filter {
                    it.name.lowercase().contains(lowerCaseQuery)
                }
                if (matchingMovies.isNotEmpty() || category.category.categoryName.lowercase().contains(lowerCaseQuery)) {
                    category.copy(movies = matchingMovies)
                } else {
                    null
                }
            }
            filteredList.clear()
            filteredList.addAll(result)
        }
        rebuildDisplayList()
        notifyDataSetChanged()
    }

    private fun rebuildDisplayList() {
        displayList.clear()
        filteredList.forEach { categoryItem ->
            displayList.add(categoryItem)
            if (categoryItem.isExpanded) {
                displayList.addAll(categoryItem.movies)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (displayList[position]) {
            is CategoryWithMovies -> TYPE_CATEGORY
            is Movie -> TYPE_MOVIE
            else -> throw IllegalArgumentException("Invalid type of data at position $position")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_CATEGORY -> CategoryViewHolder(ItemTvCategoryBinding.inflate(inflater, parent, false))
            TYPE_MOVIE -> MovieViewHolder(ItemMovieBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CategoryViewHolder -> holder.bind(displayList[position] as CategoryWithMovies)
            is MovieViewHolder -> holder.bind(displayList[position] as Movie)
        }
    }

    override fun getItemCount(): Int = displayList.size

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
        val categoryItem = displayList[headerPosition] as CategoryWithMovies
        header.findViewById<TextView>(R.id.tv_category_name).text = categoryItem.category.categoryName
        header.findViewById<View>(R.id.iv_expand_arrow).rotation = if (categoryItem.isExpanded) 180f else 0f
    }

    override fun isHeader(itemPosition: Int): Boolean {
        if (itemPosition < 0 || itemPosition >= displayList.size) return false
        return displayList[itemPosition] is CategoryWithMovies
    }

    override fun onHeaderClick(position: Int) {
        val headerPosition = getHeaderPositionForItem(position)
        if (headerPosition != RecyclerView.NO_POSITION) {
            val categoryItem = displayList[headerPosition] as CategoryWithMovies
            categoryItem.isExpanded = !categoryItem.isExpanded
            rebuildDisplayList()
            notifyDataSetChanged()
        }
    }

    inner class CategoryViewHolder(private val binding: ItemTvCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onHeaderClick(position)
                }
            }
        }

        fun bind(categoryItem: CategoryWithMovies) {
            binding.tvCategoryName.text = categoryItem.category.categoryName
            binding.ivExpandArrow.rotation = if (categoryItem.isExpanded) 180f else 0f
        }
    }

    inner class MovieViewHolder(private val binding: ItemMovieBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(movie: Movie) {
            binding.tvMovieTitle.text = movie.name
            Glide.with(itemView.context)
                .load(movie.streamIcon)
                .placeholder(R.drawable.ic_movie)
                .error(R.drawable.ic_movie)
                .into(binding.ivMoviePoster)
            itemView.setOnClickListener { onMovieClick(movie) }
        }
    }
}
