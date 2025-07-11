package com.kybers.play.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kybers.play.R
import com.kybers.play.api.Series
import com.kybers.play.databinding.ItemMovieBinding // Reutilizamos el mismo layout de película

class SeriesAdapter(
    private var seriesList: MutableList<Series>, // Cambiado a MutableList
    private val onItemClick: (Series) -> Unit
) : RecyclerView.Adapter<SeriesAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemMovieBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(series: Series) {
            binding.tvMovieTitle.text = series.name
            Glide.with(binding.ivMoviePoster.context)
                .load(series.cover)
                .placeholder(R.drawable.ic_series)
                .into(binding.ivMoviePoster)

            binding.root.setOnClickListener { onItemClick(series) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMovieBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(seriesList[position])
    }

    override fun getItemCount() = seriesList.size

    // CORREGIDO: Usamos DiffUtil para actualizaciones eficientes
    fun updateSeries(newSeriesList: List<Series>) {
        val diffCallback = SeriesDiffCallback(this.seriesList, newSeriesList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.seriesList.clear()
        this.seriesList.addAll(newSeriesList)
        diffResult.dispatchUpdatesTo(this)
    }
}

// Clase para calcular las diferencias entre la lista vieja y la nueva
class SeriesDiffCallback(
    private val oldList: List<Series>,
    private val newList: List<Series>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Comparamos por ID, que es único para cada serie
        return oldList[oldItemPosition].seriesId == newList[newItemPosition].seriesId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Comparamos el contenido completo del objeto
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}

