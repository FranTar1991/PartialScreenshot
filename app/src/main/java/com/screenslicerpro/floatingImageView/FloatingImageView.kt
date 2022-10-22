package com.screenslicerpro.floatingImageView

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import com.screenslicerpro.utils.OnMoveCropWindowListener
import com.github.chrisbanes.photoview.PhotoView

class FloatingImageView @JvmOverloads constructor(context: Context,
                                    attrs: AttributeSet? = null,
                                    defStyleAttr: Int = 0): PhotoView(context,attrs, defStyleAttr) {
    private var isMultiTouch: Boolean = false
    private var mActivePointerId: Int = 0
    private lateinit var callBackForWindowManager: OnMoveCropWindowListener
    private lateinit var doubleTapGesture: GestureDetector

init {
    attacher.setZoomable(false)
    attacher.setOnDoubleTapListener(object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {

            if (attacher.isZoomEnabled){
                attacher.setZoomable(false)
                isMultiTouch = false
            }
            return true
        }
    })

    attacher.setOnScaleChangeListener { scaleFactor, focusX, focusY ->
        if (scale< 1f){
            attacher.setZoomable(false)
            isMultiTouch = false
        }
    }

    setGestureDetector()
}
    private fun setGestureDetector() {

        doubleTapGesture = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

            override fun onDoubleTap(e: MotionEvent): Boolean {

                if (!attacher.isZoomEnabled){
                    callBackForWindowManager.onClose()
                }

                return super.onDoubleTap(e)
            }
        })
    }
    fun setWindowManagerCallback(onVIewCropWindowListener: OnMoveCropWindowListener){
        this.callBackForWindowManager = onVIewCropWindowListener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        doubleTapGesture.onTouchEvent(event)
        val action = event.actionMasked

            if (event.pointerCount > 1){
                isMultiTouch = true
            }
            mActivePointerId = event.getPointerId(0)

        when(action){
            MotionEvent.ACTION_DOWN -> {
                callBackForWindowManager.onMove(event)
            }
            MotionEvent.ACTION_UP-> {
                isMultiTouch = false
            }
            MotionEvent.ACTION_MOVE -> {
                event.actionIndex.also { pointerIndex ->
                    if(pointerIndex == event.findPointerIndex(mActivePointerId)){
                        if (isMultiTouch){
                                attacher.setZoomable(true)
                        }else{
                            callBackForWindowManager.onMove(event)
                        }
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP-> {
                isMultiTouch = false
            }

        }
        return !isMultiTouch

    }
}