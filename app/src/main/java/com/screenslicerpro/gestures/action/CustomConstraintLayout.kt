package com.screenslicerpro.gestures.action

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.screenslicerpro.floatingCropWindow.CropViewFloatingWindowService
import com.screenslicerpro.utils.*

import android.app.usage.UsageStats

import android.app.usage.UsageStatsManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_META_DATA
import com.screenslicerpro.gestures.action.database.AppItem
import com.screenslicerpro.gestures.view.viewmodel.GestureSettingsViewModel
import java.util.*
import android.net.Uri
import com.screenslicerpro.notification_utils.setUpNotification


class CustomConstraintLayout @JvmOverloads constructor(
    private val ct: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0): ConstraintLayout(ct,attrs, defStyleAttr) {


    private var listOfAppsInExceptionList: List<AppItem>? = null

    private var audioManager: AudioManager
    private lateinit var doubleTapGesture: GestureDetector
    private lateinit var swipeWithTwoFingerDetector: SimpleTwoFingerSwipeDetector
    private var wm: WindowManager? = null
    private var gesturesViewModel: GestureSettingsViewModel? = null


    private lateinit var mHandler: Handler
    private lateinit var mRunnable: Runnable
    private var mTime: Long = ViewConfiguration.getScrollDefaultDelay().toLong()


    var notFocusableFlag =  WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

    init {
        wm = WindowManagerClass.getMyWindowManager(context)
        setSwipeWithTwoFingerDetector()
        setHandler()
         audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val packageManager = context.packageManager
        val packageName = getCurrentPackageName()

        saveCurrentAppInForeground(packageName, packageManager)

        val appItemInList = checkIfPackageNameIsInList(packageName)

        val isInList = appItemInList != null
        val isAllowed = appItemInList?.isAllowed

        println("isInList: $isInList and isAllowed: $isAllowed and the list: $listOfAppsInExceptionList")

        if ((isInList && isAllowed == true) || !isInList){

            startHandler()
            changeFlags(notFocusableFlag, MATCH_PARENT)
            event?.let {
                swipeWithTwoFingerDetector.onTouchEvent(event)
            }
        }

        return false

    }

    private fun checkIfPackageNameIsInList(packageName: String): AppItem ?{

        return  listOfAppsInExceptionList?.firstOrNull() {
                it.packageName == packageName
        }

    }



    @SuppressLint("WrongConstant")
    private fun saveCurrentAppInForeground(packageName: String, packageManager: PackageManager) {


        val appInfo: ApplicationInfo? = getAppInfo(packageManager, packageName)

        appInfo?.let {
            val appName = appInfo.loadLabel(packageManager) as String
            val appIconUri = getIconUri(appInfo, packageName)
            val appItem = AppItem( packageName = packageName, appName = appName, appIconUri = appIconUri)
            gesturesViewModel?.onInsertNewApp(appItem)
        }

    }

    private fun getIconUri(appInfo: ApplicationInfo, packageName: String): String? {

       return  if (appInfo.icon != 0) {
          Uri.parse("android.resource://" + packageName + "/" + appInfo.icon).toString()
       } else{
            null
       }

    }

    private fun getAppInfo(packageManager: PackageManager, packageName: String): ApplicationInfo? {
       return try {
            packageManager.getApplicationInfo(packageName, GET_META_DATA)
        } catch (nameNotFoundException: PackageManager.NameNotFoundException){
            Log.e("NameNotFound","$nameNotFoundException")
           null
        }
    }

    @SuppressLint("WrongConstant")
    private fun getCurrentPackageName(): String {

        var currentAppPackageName = ""
        val usm = context.getSystemService("usagestats") as UsageStatsManager
        val time = System.currentTimeMillis()
        val appList =
            usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time)
        if (appList != null && appList.size > 0) {
            val mySortedMap: SortedMap<Long, UsageStats> = TreeMap()
            for (usageStats in appList) {
                mySortedMap[usageStats.lastTimeUsed] = usageStats
            }
            if (!mySortedMap.isEmpty()) {
                currentAppPackageName = mySortedMap[mySortedMap.lastKey()]?.packageName ?: "Not detected"
            }
        }
        return currentAppPackageName
    }

    fun setUpGesturesViewModel(gestureVM: GestureSettingsViewModel?){
        this.gesturesViewModel = gestureVM
    }
    fun setListOfAppsInException(list: List<AppItem>?){
        listOfAppsInExceptionList = list
    }


    private fun setGestureDetector() {

//         doubleTapGesture =
//            object : SimpleTwoFingerDoubleTapDetector() {
//                override fun onTwoFingerDoubleTap(event: MotionEvent) {
//                    Toast.makeText(context,"double tap on ${event.x} and ${event.y}",Toast.LENGTH_SHORT).show()
//                    changeFlags(allFlags)
//                    stopHandler()
//                }
//            }

        doubleTapGesture = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

            override fun onSingleTapUp(event: MotionEvent): Boolean {
                changeFlags(allFlags, 96)
                stopHandler()
                return super.onSingleTapUp(event)
            }
        })
    }

    private fun setSwipeWithTwoFingerDetector(){
                 swipeWithTwoFingerDetector =
            object : SimpleTwoFingerSwipeDetector(context) {
                override fun onTwoFingerSwipeDetector(event: MotionEvent?) {
                    stopHandler()
                    val intent = Intent(context, CropViewFloatingWindowService::class.java)
                    intent.putExtra(NEW_POSITION_X,event?.x)
                    intent.putExtra(NEW_POSITION_Y,event?.y)
                   context.startService(intent)

                    INITIAL_POINT_X = event?.x?.toInt() ?: 0
                    INITIAL_POINT_Y = event?.y?.toInt() ?: 0
                    changeFlags(allFlags, 96)

                }
            }
    }

    private fun setHandler(){
        // Initializing the handler and the runnable
        mHandler = Handler(Looper.getMainLooper())
        mRunnable = Runnable {
            changeFlags(allFlags, 96)
        }
    }

    // start handler function
    private fun startHandler(){
        mHandler.postDelayed(mRunnable, mTime)
    }

    // stop handler function
    private fun stopHandler(){
        mHandler.removeCallbacks(mRunnable)
    }

    fun changeFlags(flag: Int, mode: Int){

        wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
        val paramsF = WindowManager.LayoutParams(
            mode,
            mode,
            layoutFlag,
            flag,
            PixelFormat.TRANSLUCENT
        )
        paramsF.gravity = Gravity.TOP or Gravity.START

        try {
            wm?.updateViewLayout(this, paramsF)
        } catch (e: IllegalArgumentException){
            Log.i("MyViewError","error: $e")
        }





    }

}