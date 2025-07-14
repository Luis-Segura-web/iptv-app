package com.kybers.play.util

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * Una vista personalizada que detecta gestos del usuario sobre el reproductor.
 * Simplifica la lógica de control por gestos en la PlayerActivity.
 */
class GestureControlView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /**
     * Interfaz para notificar a la actividad sobre los gestos detectados.
     */
    interface GestureListener {
        fun onSingleTap()
        fun onDoubleTap(isLeft: Boolean)
        fun onScroll(distanceY: Float, isLeft: Boolean)
    }

    private var listener: GestureListener? = null
    private val gestureDetector: GestureDetector

    init {
        // Inicializamos el detector de gestos de Android.
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            // Es necesario devolver true en onDown para que se detecten otros gestos.
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            // Se llama cuando se confirma un solo toque.
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                listener?.onSingleTap()
                return true
            }

            // Se llama en un doble toque.
            override fun onDoubleTap(e: MotionEvent): Boolean {
                // Determinamos si el doble toque fue en la mitad izquierda o derecha de la pantalla.
                val isLeft = e.x < width / 2
                listener?.onDoubleTap(isLeft)
                return true
            }

            // Se llama cuando el usuario desliza el dedo.
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (e1 == null) return false
                // Solo nos interesan los gestos verticales para brillo y volumen.
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

    // Pasamos todos los eventos táctiles de esta vista a nuestro detector de gestos.
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }
}
