package com.kybers.play.util

import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kybers.play.adapter.StickyHeaderInterface

class StickyHeaderDecoration(private val listener: StickyHeaderInterface) : RecyclerView.ItemDecoration() {

    private var currentHeader: View? = null
    private var currentHeaderPosition: Int = -1

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)

        val topChild = parent.getChildAt(0) ?: return
        val topChildPosition = parent.getChildAdapterPosition(topChild)
        if (topChildPosition == RecyclerView.NO_POSITION) {
            return
        }

        val headerPosition = listener.getHeaderPositionForItem(topChildPosition)
        if (headerPosition != currentHeaderPosition) {
            currentHeaderPosition = headerPosition
            val headerLayout = listener.getHeaderLayout(headerPosition)
            currentHeader = LayoutInflater.from(parent.context).inflate(headerLayout, parent, false)
            listener.bindHeaderData(currentHeader!!, headerPosition)
        }

        val header = currentHeader ?: return

        var contactPoint = header.bottom
        val childInContact = getChildInContact(parent, contactPoint)

        if (childInContact != null && listener.isHeader(parent.getChildAdapterPosition(childInContact))) {
            moveHeader(c, header, childInContact)
            return
        }

        drawHeader(c, header)
    }

    private fun getChildInContact(parent: RecyclerView, contactPoint: Int): View? {
        var childInContact: View? = null
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child.bottom > contactPoint) {
                if (child.top <= contactPoint) {
                    childInContact = child
                    break
                }
            }
        }
        return childInContact
    }

    private fun moveHeader(c: Canvas, currentHeader: View, nextHeader: View) {
        c.save()
        c.translate(0f, (nextHeader.top - currentHeader.height).toFloat())
        currentHeader.draw(c)
        c.restore()
    }

    private fun drawHeader(c: Canvas, header: View) {
        c.save()
        c.translate(0f, 0f)
        header.draw(c)
        c.restore()
    }
}
