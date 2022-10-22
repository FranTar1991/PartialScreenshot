package com.screenslicerpro.floatingImageView

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.view.*
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager.LayoutParams.*
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.screenslicerpro.R
import com.screenslicerpro.utils.OnMoveCropWindowListener
import com.screenslicerpro.utils.flags
import com.screenslicerpro.utils.layoutFlag
import android.widget.Toast

import com.screenslicerpro.MainActivity





class FloatingImageViewService: Service() {

    private lateinit var floatingImage: FloatingImageView
    private var mImageUri: Uri? = null

    private lateinit var manager: WindowManager
    private val binder = LocalBinder()
    private var isFloatingWindowOn = false
    private lateinit var doubleTapGesture: GestureDetector

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): FloatingImageViewService = this@FloatingImageViewService
    }

    override fun onBind(p0: Intent?): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        setGestureDetector()
        if(!isFloatingWindowOn){
            setUpFloatingWidget()
        } else {
            floatingImage.setImageURI(mImageUri)
        }

        return START_NOT_STICKY
    }

    fun setImageUri(uri: Uri){
        mImageUri = uri
    }






    private fun setGestureDetector() {

        doubleTapGesture = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {

            override fun onDoubleTap(e: MotionEvent): Boolean {
                manager.removeView(floatingImage)
                return super.onDoubleTap(e)
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpFloatingWidget() {

        manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        floatingImage  = LayoutInflater.from(this).inflate(R.layout.floating_image_view, null) as FloatingImageView

        val params = WindowManager.LayoutParams(
            WRAP_CONTENT,
            WRAP_CONTENT,
            layoutFlag,
            flags,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START    //Initially view will be added to top-left corner
        params.x = 0
        params.y = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            params.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        floatingImage.addOnAttachStateChangeListener(object: View.OnAttachStateChangeListener{
            override fun onViewAttachedToWindow(p0: View) {
                floatingImage.setImageURI(mImageUri)
                isFloatingWindowOn = true
            }

            override fun onViewDetachedFromWindow(p0: View) {
                isFloatingWindowOn = false
            }

        }
        )


        floatingImage.setWindowManagerCallback(object : OnMoveCropWindowListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            override fun onMove(event: MotionEvent?) {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {

                        //remember the initial position.
                        initialX = params.x
                        initialY = params.y

                        //get the touch location
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                    }
                    MotionEvent.ACTION_UP -> {

                    }
                    MotionEvent.ACTION_MOVE -> {
                        //Calculate the X and Y coordinates of the view.
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()

                        manager.updateViewLayout(floatingImage, params);


                    }
                }

            }
            override fun onClose() {
                manager.removeView(floatingImage)
            }

        })

        manager.addView(floatingImage, params)


    }
}