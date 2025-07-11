package com.kybers.play

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule

@GlideModule
class MyAppGlideModule : AppGlideModule() {
    // Puedes dejar esta clase vacía. Su sola presencia con la anotación
    // es suficiente para que Glide genere el código optimizado.
    // También puedes sobreescribir métodos aquí para configurar Glide globalmente.
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // Por ejemplo, puedes configurar aquí el caché de disco o de memoria.
        super.applyOptions(context, builder)
    }
}
