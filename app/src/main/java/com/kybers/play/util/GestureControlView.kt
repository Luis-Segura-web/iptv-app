package com.kybers.play.util

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class GestureControlView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface GestureListener {
        fun onSingleTap()
        fun onDoubleTap(isLeft: Boolean)
        fun onScroll(distanceY: Float, isLeft: Boolean)
    }

    private var listener: GestureListener? = null
    private val gestureDetector: GestureDetector

    init {
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                listener?.onSingleTap()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                val isLeft = e.x < width / 2
                listener?.onDoubleTap(isLeft)
                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (e1 == null) return false
                // Solo nos interesan los gestos verticales para brillo y volumen
                if (abs(distanceY) > abs(distanceX)) {
                    val isLeft = e1.x < width / 2
                    listener?.onScroll(distanceY, isLeft)
                }
                return true
            }
        })
    }

    fun setGestureListener(listener: GestureListener) {
        this.listener = listener
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }
}
