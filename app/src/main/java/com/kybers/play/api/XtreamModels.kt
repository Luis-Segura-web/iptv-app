package com.kybers.play.api

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

// Modelo para la información del usuario al autenticarse
data class UserInfoResponse(
    @SerializedName("user_info") val userInfo: UserInfo // CORREGIDO
)
data class UserInfo(val username: String, val status: String, val auth: Int)

// Modelo genérico para las categorías (canales, películas, series)
data class Category(
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("category_name") val categoryName: String,
    @SerializedName("parent_id") val parentId: Int
)

// Modelo para los streams de TV en vivo
@Entity(tableName = "live_streams")
data class LiveStream(
    @PrimaryKey
    @SerializedName("stream_id") val streamId: Int,
    val name: String,
    @SerializedName("stream_icon") val streamIcon: String,
    @SerializedName("category_id") val categoryId: String
) : PlayableContent {
    override fun getContentId(): String = streamId.toString()
    override fun getTitle(): String = name
    override fun getCoverUrl(): String = streamIcon
    override fun getType(): String = "live"
}

// Modelo para las películas (VOD)
@Entity(tableName = "movies")
data class Movie(
    @PrimaryKey
    @SerializedName("stream_id") val streamId: Int,
    val name: String,
    @SerializedName("stream_icon") val streamIcon: String,
    @SerializedName("rating") val rating: String?,
    @SerializedName("container_extension") val containerExtension: String
) : PlayableContent {
    override fun getContentId(): String = streamId.toString()
    override fun getTitle(): String = name
    override fun getCoverUrl(): String = streamIcon
    override fun getType(): String = "movie"
}

// Modelo para una serie en la lista principal
@Entity(tableName = "series")
data class Series(
    @PrimaryKey
    @SerializedName("series_id") val seriesId: Int,
    val name: String,
    val cover: String,
    @SerializedName("plot") val plot: String?,
    @SerializedName("cast") val cast: String?,
    @SerializedName("director") val director: String?,
    @SerializedName("genre") val genre: String?,
    @SerializedName("releaseDate") val releaseDate: String?,
    @SerializedName("rating") val rating: String?
) : PlayableContent {
    override fun getContentId(): String = seriesId.toString()
    override fun getTitle(): String = name
    override fun getCoverUrl(): String = cover
    override fun getType(): String = "series"
}

// Modelo para la información detallada de una serie (respuesta de la API)
data class SeriesInfoResponse(
    @SerializedName("seasons") val seasons: List<Season>,
    @SerializedName("info") val info: Series,
    @SerializedName("episodes") val episodes: Map<String, List<Episode>>
)

// Modelo para una temporada
data class Season(
    @SerializedName("season_number") val seasonNumber: Int,
    val name: String
)

// Modelo para un episodio
data class Episode(
    val id: String,
    @SerializedName("episode_num") val episodeNum: Int,
    val title: String,
    @SerializedName("container_extension") val containerExtension: String,
    val info: EpisodeInfo
)

// Modelo para información extra de un episodio
data class EpisodeInfo(
    val plot: String?
)
