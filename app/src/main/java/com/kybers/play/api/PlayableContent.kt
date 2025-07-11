package com.kybers.play.api
interface PlayableContent {
    fun getContentId(): String
    fun getTitle(): String
    fun getCoverUrl(): String
    fun getType(): String
}