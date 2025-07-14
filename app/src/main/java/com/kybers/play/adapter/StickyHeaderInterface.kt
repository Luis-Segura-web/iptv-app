package com.kybers.play.adapter

import android.view.View

/**
 * Interfaz para la funcionalidad de cabeceras pegajosas (Sticky Headers).
 * Cualquier adaptador que quiera tener cabeceras pegajosas debe implementar esta interfaz.
 */
interface StickyHeaderInterface {
    fun getHeaderPositionForItem(itemPosition: Int): Int
    fun getHeaderLayout(headerPosition: Int): Int
    fun bindHeaderData(header: View, headerPosition: Int)
    fun isHeader(itemPosition: Int): Boolean
    fun onHeaderClick(position: Int)
}
