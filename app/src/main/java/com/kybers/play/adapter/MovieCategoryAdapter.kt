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
import com.kybers.play.databinding.PlaceholderListItemBinding // Importar el binding del placeholder

class MovieCategoryAdapter(
    private val onMovieClick: (Movie) -> Unit,
    // Callback para cuando una categoría se expande y necesita cargar sus películas
    private val onCategoryNeedsLoad: (CategoryWithMovies, Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), StickyHeaderInterface {

    private val dataList = mutableListOf<Any>()
    private var sourceList = mutableListOf<CategoryWithMovies>() // Lista original de categorías
    private var filteredList = mutableListOf<CategoryWithMovies>() // Lista de categorías filtradas para mostrar

    companion object {
        const val TYPE_CATEGORY = 0
        const val TYPE_MOVIE = 1
        const val TYPE_LOADING_PLACEHOLDER = 2
    }

    fun submitList(list: List<CategoryWithMovies>) {
        sourceList.clear()
        sourceList.addAll(list)
        filteredList.clear()
        // Al enviar la lista inicial, las películas no están cargadas, así que movies = null
        filteredList.addAll(list.map { it.copy(movies = null, isExpanded = false) })
        rebuildDataList()
        notifyDataSetChanged()
    }

    // CORREGIDO: Ahora el filtro recibe la lista completa de todas las películas
    fun filter(query: String?, allMovies: List<Movie>) {
        val lowerCaseQuery = query?.lowercase()?.trim()
        if (lowerCaseQuery.isNullOrEmpty()) {
            // Si la búsqueda está vacía, volvemos a mostrar todas las categorías sin expandir
            filteredList.clear()
            filteredList.addAll(sourceList.map { it.copy(movies = null, isExpanded = false) })
        } else {
            // Filtramos todas las películas para encontrar las que coinciden
            val matchingFlatMovies = allMovies.filter {
                it.name.lowercase().contains(lowerCaseQuery)
            }

            val resultCategories = mutableListOf<CategoryWithMovies>()
            // Recorremos las categorías originales para mantener el orden
            sourceList.forEach { category ->
                val categoryMatchesQuery = category.category.categoryName.lowercase().contains(lowerCaseQuery)
                val moviesInThisCategoryThatMatch = matchingFlatMovies.filter {
                    it.categoryId == category.category.categoryId // Filtramos las películas que pertenecen a esta categoría
                }

                // Si el nombre de la categoría coincide O si hay películas que coinciden dentro de esta categoría
                if (categoryMatchesQuery || moviesInThisCategoryThatMatch.isNotEmpty()) {
                    resultCategories.add(
                        category.copy(
                            movies = moviesInThisCategoryThatMatch, // Solo mostramos las películas que coinciden en esta categoría
                            isExpanded = true, // Expandimos la categoría si hay coincidencias
                            isLoading = false // Aseguramos que no esté en estado de carga
                        )
                    )
                }
            }
            filteredList.clear()
            filteredList.addAll(resultCategories)
        }
        rebuildDataList()
        notifyDataSetChanged()
    }

    // Método para que el fragmento notifique al adaptador que las películas se han cargado
    fun onCategoryLoaded(categoryId: String, loadedMovies: List<Movie>) {
        // Actualizar tanto la lista original como la filtrada
        val categoryToUpdateInSource = sourceList.find { it.category.categoryId == categoryId }
        categoryToUpdateInSource?.apply {
            movies = loadedMovies
            isLoading = false
        }

        val categoryToUpdateInFiltered = filteredList.find { it.category.categoryId == categoryId }
        categoryToUpdateInFiltered?.apply {
            movies = loadedMovies
            isLoading = false
        }
        rebuildDataList()
        notifyDataSetChanged()
    }

    private fun rebuildDataList() {
        dataList.clear()
        filteredList.forEach { categoryItem ->
            dataList.add(categoryItem)
            if (categoryItem.isExpanded) {
                if (categoryItem.isLoading) {
                    dataList.add(LoadingPlaceholder) // Añadir el placeholder de carga
                } else if (!categoryItem.movies.isNullOrEmpty()) {
                    dataList.addAll(categoryItem.movies!!)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (dataList[position]) {
            is CategoryWithMovies -> TYPE_CATEGORY
            is Movie -> TYPE_MOVIE
            is LoadingPlaceholder -> TYPE_LOADING_PLACEHOLDER
            else -> throw IllegalArgumentException("Invalid type of data at position $position")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_CATEGORY -> CategoryViewHolder(ItemTvCategoryBinding.inflate(inflater, parent, false))
            TYPE_MOVIE -> MovieViewHolder(ItemMovieBinding.inflate(inflater, parent, false))
            TYPE_LOADING_PLACEHOLDER -> LoadingPlaceholderViewHolder(PlaceholderListItemBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CategoryViewHolder -> holder.bind(dataList[position] as CategoryWithMovies)
            is MovieViewHolder -> holder.bind(dataList[position] as Movie)
            is LoadingPlaceholderViewHolder -> holder.bind()
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
        val categoryItem = dataList[headerPosition] as CategoryWithMovies
        header.findViewById<TextView>(R.id.tv_category_name).text = categoryItem.category.categoryName
        header.findViewById<View>(R.id.iv_expand_arrow).rotation = if (categoryItem.isExpanded) 180f else 0f
    }

    override fun isHeader(itemPosition: Int): Boolean {
        if (itemPosition < 0 || itemPosition >= dataList.size) return false
        return dataList[itemPosition] is CategoryWithMovies
    }

    override fun onHeaderClick(position: Int) {
        val headerPosition = getHeaderPositionForItem(position)
        if (headerPosition != RecyclerView.NO_POSITION) {
            val categoryItem = dataList[headerPosition] as CategoryWithMovies
            categoryItem.isExpanded = !categoryItem.isExpanded

            // Si la categoría se acaba de expandir y no tiene películas cargadas,
            // notificamos al fragmento para que las cargue.
            if (categoryItem.isExpanded && categoryItem.movies == null && !categoryItem.isLoading) {
                categoryItem.isLoading = true // Marcar como en carga para mostrar placeholder
                onCategoryNeedsLoad(categoryItem, headerPosition)
            }
            rebuildDataList()
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

    inner class LoadingPlaceholderViewHolder(binding: PlaceholderListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            // No necesita lógica de binding, ya que es un layout estático de placeholder.
        }
    }

    object LoadingPlaceholder
}
