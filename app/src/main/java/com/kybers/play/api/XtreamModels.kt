package com.kybers.play.api

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

// --- Modelos para la Respuesta de la API de Xtream Codes ---

data class UserInfoResponse(
    @SerializedName("user_info") val userInfo: UserInfo
)

data class UserInfo(
    val username: String,
    val status: String,
    val auth: Int
)

data class Category(
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("category_name") val categoryName: String,
    @SerializedName("parent_id") val parentId: Int
)

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

/**
 * Modelo para una película (VOD).
 */
@Entity(tableName = "movies")
data class Movie(
    @PrimaryKey
    @SerializedName("stream_id") val streamId: Int,
    val name: String,
    @SerializedName("stream_icon") val streamIcon: String,
    @SerializedName("rating") val rating: String?,
    @SerializedName("container_extension") val containerExtension: String,
    // CORREGIDO: Se añade el campo category_id que faltaba.
    @SerializedName("category_id") val categoryId: String?
) : PlayableContent {
    override fun getContentId(): String = streamId.toString()
    override fun getTitle(): String = name
    override fun getCoverUrl(): String = streamIcon
    override fun getType(): String = "movie"
}

@Entity(tableName = "series")
data class Series(
    @PrimaryKey
    @SerializedName("series_id") val seriesId: Int,
    val name: String,
    val cover: String,
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    @SerializedName("releaseDate") val releaseDate: String?,
    val rating: String?
) : PlayableContent {
    override fun getContentId(): String = seriesId.toString()
    override fun getTitle(): String = name
    override fun getCoverUrl(): String = cover
    override fun getType(): String = "series"
}

data class SeriesInfoResponse(
    val seasons: List<Season>,
    val info: Series,
    val episodes: Map<String, List<Episode>>
)

data class Season(
    @SerializedName("season_number") val seasonNumber: Int,
    val name: String
)

data class Episode(
    val id: String,
    @SerializedName("episode_num") val episodeNum: Int,
    val title: String,
    @SerializedName("container_extension") val containerExtension: String,
    val info: EpisodeInfo?
)

data class EpisodeInfo(
    val plot: String?
)
