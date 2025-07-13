package com.kybers.play.util

import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kybers.play.adapter.StickyHeaderInterface

class StickyHeaderDecoration(private val listener: StickyHeaderInterface) : RecyclerView.ItemDecoration() {

    private var stickyHeaderHeight: Int = 0

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)

        // Obtenemos la primera vista visible en el RecyclerView.
        val topChild = parent.getChildAt(0) ?: return
        val topChildPosition = parent.getChildAdapterPosition(topChild)
        if (topChildPosition == RecyclerView.NO_POSITION) {
            return
        }

        // Obtenemos la posición del encabezado que corresponde al item superior.
        val headerPosition = listener.getHeaderPositionForItem(topChildPosition)
        if (headerPosition == RecyclerView.NO_POSITION) return

        // Obtenemos la vista del encabezado actual y la dibujamos.
        val currentHeader = getHeaderViewForItem(headerPosition, parent)
        fixLayoutSize(parent, currentHeader)

        // Verificamos si el siguiente encabezado está a punto de empujar al actual.
        val contactPoint = currentHeader.bottom
        val childInContact = getChildInContact(parent, contactPoint)

        // Si el siguiente item es un encabezado, calculamos el desplazamiento.
        if (childInContact != null && listener.isHeader(parent.getChildAdapterPosition(childInContact))) {
            moveHeader(c, currentHeader, childInContact)
            return
        }

        // Si no, simplemente dibujamos el encabezado en la parte superior.
        drawHeader(c, currentHeader)
    }

    /**
     * Infla y bindea la vista del encabezado para una posición dada.
     */
    private fun getHeaderViewForItem(headerPosition: Int, parent: RecyclerView): View {
        val layoutResId = listener.getHeaderLayout(headerPosition)
        val header = LayoutInflater.from(parent.context).inflate(layoutResId, parent, false)
        listener.bindHeaderData(header, headerPosition)
        return header
    }

    /**
     * Dibuja el encabezado en la parte superior de la vista.
     */
    private fun drawHeader(c: Canvas, header: View) {
        c.save()
        c.translate(0f, 0f)
        header.draw(c)
        c.restore()
    }

    /**
     * Dibuja el encabezado con un desplazamiento vertical para dar el efecto de "empuje".
     */
    private fun moveHeader(c: Canvas, currentHeader: View, nextHeader: View) {
        c.save()
        c.translate(0f, (nextHeader.top - currentHeader.height).toFloat())
        currentHeader.draw(c)
        c.restore()
    }

    /**
     * Busca la vista que está en contacto con el borde inferior del encabezado pegajoso.
     */
    private fun getChildInContact(parent: RecyclerView, contactPoint: Int): View? {
        var childInContact: View? = null
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child.bottom > contactPoint && child.top <= contactPoint) {
                childInContact = child
                break
            }
        }
        return childInContact
    }

    /**
     * Mide y establece las dimensiones correctas para la vista del encabezado.
     * Esto es CRUCIAL para que se dibuje correctamente.
     */
    private fun fixLayoutSize(parent: ViewGroup, view: View) {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.UNSPECIFIED)

        view.measure(widthSpec, heightSpec)

        stickyHeaderHeight = view.measuredHeight
        view.layout(0, 0, view.measuredWidth, stickyHeaderHeight)
    }
}
