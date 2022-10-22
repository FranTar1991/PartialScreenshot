package com.screenslicerpro.floatingCropWindow

import android.app.Service
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.*
import com.screenslicerpro.MainActivity
import com.screenslicerpro.R
import com.screenslicerpro.floatingCropWindow.cropWindow.CropView
import com.screenslicerpro.floatingCropWindow.cropWindow.ScreenShotTaker
import com.screenslicerpro.gestures.action.CustomConstraintLayout
import com.screenslicerpro.gestures.action.database.AppItem
import com.screenslicerpro.gestures.view.GesturesFragmentSettings
import com.screenslicerpro.gestures.view.viewmodel.GestureSettingsViewModel
import com.screenslicerpro.notification_utils.setUpNotification
import com.screenslicerpro.utils.*
import tourguide.tourguide.TourGuide


class CropViewFloatingWindowService: Service() {


    private var floatingGestureView: CustomConstraintLayout? = null
    private var newDrawableOnSwitch: Int = R.drawable.ic_toggle_off
    private val SHOW_SECOND_TOUR: String ="show_second_tour"
    private var showTourGuide: Boolean = true

    private var mTourGuideHandler: TourGuide? = null
    private var floatingView: CropView? = null
    private var mData: Intent? = null
    var screenShotTaker: ScreenShotTaker? = null
    private var isCropWindowOn = false
    private var isGestureWindowOn = false
    private var sharedPreferences: SharedPreferences? = null
    private var editor: SharedPreferences.Editor? = null
    private var gestureSettingsViewModel: GestureSettingsViewModel? = null

    private var manager: WindowManager? = null



    private var takeScreenShotServiceCallback: FloatingWindowListener? = null
    private val binder: IBinder = LocalBinder()




    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): CropViewFloatingWindowService = this@CropViewFloatingWindowService
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder;
    }

    override fun onCreate() {
        super.onCreate()
        manager = WindowManagerClass.getMyWindowManager(applicationContext)
    }

    fun setServiceCallBacks(floatingAndTakeScreenShotServiceCallback: FloatingWindowListener?, from: String){

        takeScreenShotServiceCallback = floatingAndTakeScreenShotServiceCallback
        gestureSettingsViewModel = (takeScreenShotServiceCallback as MainActivity).getGestureViewModel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startSharedPreferences()
        newDrawableOnSwitch = getLaunchingDrawable()
        val drawableInIntentExtra = getIntentExtra(intent)
        val newPosition = getNewPosition(intent)

        if(drawableInIntentExtra == -1) {
           if (!isGestureWindowOn && newDrawableOnSwitch == R.drawable.ic_toggle_on){
               setUpGestureWidget()
           }

           if (!isCropWindowOn){

               setUpNotification(applicationContext, newDrawableOnSwitch, this)
               setUpFloatingWidget(newPosition)

               screenShotTaker = ScreenShotTaker(applicationContext,
                   this,
                   floatingView,
                   takeScreenShotServiceCallback as MainActivity)

               mData = takeScreenShotServiceCallback?.getDataToRecordScreen()


           } else {
               floatingView?.apply {
                   removeMyWaitDrawable()
                   manager?.removeMyView(this,
                       ViewGroup.LayoutParams.WRAP_CONTENT,
                       newPosition.first?.toInt() ?: 0,
                       newPosition.second?.toInt() ?: 0)

                   setNewPositionOfSecondRect(newPosition.first?.toInt() ?: 0,
                       newPosition.second?.toInt() ?: 0)
                   shakeItBaby(applicationContext)
               }
           }

       }else {
            if(checkIfHasPermission()){
                newDrawableOnSwitch = getNextDrawable(drawableInIntentExtra)
                saveToSharedPreferences()
               setUpNotification(applicationContext,newDrawableOnSwitch, this)
                addOrRemoveServiceView(newDrawableOnSwitch)
            }else {
                callSettingsActivity()
            }
        }


        return START_NOT_STICKY
    }

    private fun callSettingsActivity() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun getIntentExtra(intent: Intent?): Int {
        return intent?.getIntExtra(MY_INTENT_EXTRA, -1) ?: -1
    }

    private fun getNewPosition(intent: Intent?): Pair<Float?,Float?> {

        return Pair(intent?.getFloatExtra(NEW_POSITION_X, 0f),
            intent?.getFloatExtra(NEW_POSITION_Y, 0f))

    }

    private fun addOrRemoveServiceView(currentDrawable: Int) {

        when(currentDrawable){
            R.drawable.ic_toggle_on ->{
                destroyCurrentCropAndOptionsWindow()
                setUpGestureWidget()
            }
            R.drawable.ic_toggle_off ->{ manager?.removeView(floatingGestureView)}
        }
    }

    private fun getNextDrawable(currentDrawable: Int): Int {

        return if (currentDrawable == R.drawable.ic_toggle_off){
            R.drawable.ic_toggle_on
        } else{
            R.drawable.ic_toggle_off
        }
    }

    private fun saveToSharedPreferences(){

        if (newDrawableOnSwitch == R.drawable.ic_toggle_on){
            editor?.putBoolean(MY_VIEW_ID, true)

        } else{
            editor?.putBoolean(MY_VIEW_ID, false)
        }

        editor?.commit()


    }

    private fun checkIfHasPermission(): Boolean {

        val viewModel = (takeScreenShotServiceCallback as MainActivity).getGestureViewModel()
        return (viewModel?.checkIfHasPermission(applicationContext) == true)
    }

    private fun setUpGestureWidget() {
        floatingGestureView = LayoutInflater.from(this).inflate(R.layout.surface_for_gestures_layout, null) as CustomConstraintLayout
        val paramsF = WindowManager.LayoutParams(
            96,
            96,
            layoutFlag,
            allFlags,
            PixelFormat.TRANSLUCENT
        )

        paramsF.gravity = Gravity.TOP or Gravity.START

        manager?.addView(floatingGestureView, paramsF)
        floatingGestureView?.addOnAttachStateChangeListener(object: View.OnAttachStateChangeListener{
            override fun onViewAttachedToWindow(p0: View) {
                isGestureWindowOn = true
                floatingGestureView?.apply{
                    setListOfAppsInException(gestureSettingsViewModel?.apps?.value)
                }
            }

            override fun onViewDetachedFromWindow(p0: View) {
                isGestureWindowOn = false
            }

        }
        )
        floatingGestureView?.setUpGesturesViewModel(gestureSettingsViewModel)

    }

    private fun startSharedPreferences() {
        sharedPreferences = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE)
        showTourGuide =  sharedPreferences?.getBoolean(SHOW_SECOND_TOUR, true) ?: true
        editor = sharedPreferences?.edit()

        println("new drawable: ${sharedPreferences?.getBoolean(MY_VIEW_ID, false)}")
    }

    private fun getLaunchingDrawable(): Int {
      return  if(sharedPreferences?.getBoolean(MY_VIEW_ID, false) == false){
            R.drawable.ic_toggle_off
        } else{
            R.drawable.ic_toggle_on
        }
    }

    fun hideCropView(visibility: Int){
        floatingView?.visibility = visibility
        screenShotTaker?.optionsWindowView?.hideOptionsView(visibility)
    }


    override fun onDestroy() {
        super.onDestroy()
       destroyCurrentCropAndOptionsWindow()
        destroyCurrentGesturesWindow()
    }

    private fun destroyCurrentCropAndOptionsWindow() {

        if(floatingView?.isShown == true){
            manager?.removeView(floatingView)
            screenShotTaker?.optionsWindowView?.destroyView()
        }
    }

    private fun destroyCurrentGesturesWindow(){
        if (floatingGestureView?.isShown == true){
            manager?.removeView(floatingGestureView)
        }
    }

    private fun setUpFloatingWidget(newPosition: Pair<Float?, Float?>) {

        floatingView  = LayoutInflater.from(this).inflate(R.layout.crop_view, null) as CropView
        floatingView?.setOnRequestTakeScreenShotListener(object: OnRequestTakeScreenShotListener {
            override fun onRequestScreenShot(rect: Rect) {
                screenShotTaker?.getStartIntent(applicationContext, -1, mData)?.let {
                    screenShotTaker?.setUpScreenCapture(it, rect)
                }
            }

            override fun cleanUpMyTourGuide() {
               closeTourGuide(SHOW_SECOND_TOUR)
            }
        })

        floatingView?.addOnAttachStateChangeListener(object: View.OnAttachStateChangeListener{
            override fun onViewAttachedToWindow(p0: View) {
                isCropWindowOn = true

            }

            override fun onViewDetachedFromWindow(p0: View) {
              isCropWindowOn = false
            }

        }
        )


         manager?.addMyCropView(floatingView, ViewGroup.LayoutParams.WRAP_CONTENT,
         newPosition.first?.toInt() ?: 0, newPosition.second?.toInt() ?: 0)



         if(showTourGuide){
             mTourGuideHandler=  setMyTourGuide(takeScreenShotServiceCallback as MainActivity, getString(R.string.title_crop_view_tut),
                 getString(R.string.description_cropt_view_tut),
                 Gravity.END or Gravity.BOTTOM,
                 floatingView as View )
         }



    }

     fun closeTourGuide(key: String) {
        mTourGuideHandler?.cleanUp()
        if(showTourGuide){
            editor?.putBoolean(key, false)
            editor?.apply()
        }
    }


    fun setCroppedImage(croppedBitmap: Bitmap?) {
        floatingView?.croppedImage = croppedBitmap
    }

    fun setNewExceptionList(listOfAppsInException: List<AppItem>) {

        floatingGestureView?.apply {
            setListOfAppsInException(listOfAppsInException)
        }

    }
}




