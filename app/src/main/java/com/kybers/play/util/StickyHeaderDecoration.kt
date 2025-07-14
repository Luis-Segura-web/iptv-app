package com.kybers.play.util

import android.graphics.Canvas
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kybers.play.adapter.StickyHeaderInterface

/**
 * Un ItemDecoration que crea el efecto de "cabeceras pegajosas" (sticky headers)
 * y maneja los clics en la cabecera.
 */
class StickyHeaderDecoration(
    private val listener: StickyHeaderInterface,
    private val recyclerView: RecyclerView
) : RecyclerView.ItemDecoration(), RecyclerView.OnItemTouchListener {

    private var stickyHeaderHeight: Int = 0
    private val gestureDetector: GestureDetector

    init {
        // Inicializamos un detector de gestos para capturar los clics en la cabecera.
        gestureDetector = GestureDetector(recyclerView.context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val topChild = recyclerView.getChildAt(0) ?: return false
                val topChildPosition = recyclerView.getChildAdapterPosition(topChild)
                if (topChildPosition != RecyclerView.NO_POSITION) {
                    // Si el toque ocurrió dentro del área de la cabecera, notificamos al listener.
                    if (e.y <= stickyHeaderHeight) {
                        listener.onHeaderClick(topChildPosition)
                        return true
                    }
                }
                return false
            }
        })
        // Añadimos este decorador como un listener de toques al RecyclerView.
        recyclerView.addOnItemTouchListener(this)
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)

        val topChild = parent.getChildAt(0) ?: return
        val topChildPosition = parent.getChildAdapterPosition(topChild)
        if (topChildPosition == RecyclerView.NO_POSITION) {
            return
        }

        val headerPosition = listener.getHeaderPositionForItem(topChildPosition)
        if (headerPosition == RecyclerView.NO_POSITION) return

        val currentHeader = getHeaderViewForItem(headerPosition, parent)
        fixLayoutSize(parent, currentHeader)

        val contactPoint = currentHeader.bottom
        val childInContact = getChildInContact(parent, contactPoint, topChildPosition)

        if (childInContact != null && listener.isHeader(parent.getChildAdapterPosition(childInContact))) {
            moveHeader(c, currentHeader, childInContact)
            return
        }

        drawHeader(c, currentHeader)
    }

    private fun getHeaderViewForItem(headerPosition: Int, parent: RecyclerView): View {
        val layoutResId = listener.getHeaderLayout(headerPosition)
        val header = LayoutInflater.from(parent.context).inflate(layoutResId, parent, false)
        listener.bindHeaderData(header, headerPosition)
        return header
    }

    private fun drawHeader(c: Canvas, header: View) {
        c.save()
        c.translate(0f, 0f)
        header.draw(c)
        c.restore()
    }

    private fun moveHeader(c: Canvas, currentHeader: View, nextHeader: View) {
        c.save()
        c.translate(0f, (nextHeader.top - currentHeader.height).toFloat())
        currentHeader.draw(c)
        c.restore()
    }

    private fun getChildInContact(parent: RecyclerView, contactPoint: Int, topChildPosition: Int): View? {
        var childInContact: View? = null
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)
            if (position > topChildPosition && child.bottom > contactPoint && child.top <= contactPoint) {
                childInContact = child
                break
            }
        }
        return childInContact
    }

    private fun fixLayoutSize(parent: ViewGroup, view: View) {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.UNSPECIFIED)

        view.measure(widthSpec, heightSpec)

        stickyHeaderHeight = view.measuredHeight
        view.layout(0, 0, view.measuredWidth, stickyHeaderHeight)
    }

    // --- Implementación de RecyclerView.OnItemTouchListener ---

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        // Pasamos el evento de toque a nuestro detector de gestos.
        // Si detecta un toque en la cabecera, devuelve 'true' para "consumir" el evento
        // y evitar que el clic llegue al item que está debajo.
        return gestureDetector.onTouchEvent(e)
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        // No necesitamos implementar esto.
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        // No necesitamos implementar esto.
    }
}
