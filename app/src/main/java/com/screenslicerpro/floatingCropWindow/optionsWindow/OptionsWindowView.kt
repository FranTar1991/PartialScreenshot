package com.screenslicerpro.floatingCropWindow.optionsWindow


import android.content.Context
import android.content.Context.WINDOW_SERVICE

import android.graphics.PixelFormat

import android.view.*
import com.screenslicerpro.R
import android.view.WindowManager

import android.view.Gravity

import android.view.LayoutInflater
import android.view.MotionEvent.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import com.screenslicerpro.databinding.OptionsViewBinding
import com.screenslicerpro.floatingCropWindow.cropWindow.CropView
import com.screenslicerpro.utils.*

private val downColor = R.color.primaryLightColor
private val upColor = R.color.primaryColor
private val disabledColor = R.color.Gray
private var isActionMove = false

class OptionsWindowView (private val context: Context,
                         private val cropView: CropView?) : View.OnTouchListener {

    private lateinit var params: WindowManager.LayoutParams
    private var mWindowManager: WindowManager? = null
    private var mFloatingView: OptionsViewBinding? = null
    private var saveButton: ImageView? = null
    private var deleteButton: ImageView? = null
    private var shareButton: ImageView? = null
    private var minMaxButton: ImageView? = null
    private var editButton: ImageView? = null
    private var saveButtonBc: ImageView? = null
    private var deleteButtonBc: ImageView? = null
    private var shareButtonBc: ImageView? = null
    private var minMaxButtonBk: ImageView? = null
    private var editButtonBc: ImageView? = null
    private var extractTxtBc: ImageView? = null
    private var extractTxtButton: ImageView? = null
    private var copyTextBtn: ImageView? = null
    private var copyTextBk: ImageView? = null

    private var progressBar: ProgressBar? = null
    private var extractedTextEt: EditText? = null
    private var extractedTextContainer: ConstraintLayout? = null


    private var onOptionsWindowSelectedListener: OnOptionsWindowSelectedListener? = null
    private var mainContainer: ConstraintLayout? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f


    fun createView() {

        setFloatingView()
        setAllVariables()
        reEnableButtons()

    }

    private fun setAllVariables() {

        saveButton = mFloatingView?.save?.apply { setOnTouchListener(this@OptionsWindowView) }
        deleteButton =  mFloatingView?.delete?.apply { setOnTouchListener(this@OptionsWindowView) }
        shareButton = mFloatingView?.share?.apply { setOnTouchListener(this@OptionsWindowView) }
        minMaxButton = mFloatingView?.minMax?.apply { setOnTouchListener(this@OptionsWindowView) }
        editButton = mFloatingView?.edit?.apply { setOnTouchListener(this@OptionsWindowView) }
        extractTxtButton = mFloatingView?.extractText?.apply { setOnTouchListener(this@OptionsWindowView) }
        copyTextBtn = mFloatingView?.copyText?.apply { setOnTouchListener(this@OptionsWindowView) }

        mainContainer = mFloatingView?.mainBtnContainer

        saveButtonBc = mFloatingView?.saveBk
        deleteButtonBc =  mFloatingView?.deleteBk
        shareButtonBc = mFloatingView?.shareBk
        minMaxButtonBk = mFloatingView?.minMaxBk
        editButtonBc= mFloatingView?.editBk
        extractTxtBc = mFloatingView?.extractTextBk
        progressBar = mFloatingView?.progressBar3
        extractedTextEt = mFloatingView?.extractedTextEt
        extractTxtBc = mFloatingView?.extractTextBk
        copyTextBk = mFloatingView?.copyTextBk

        extractedTextContainer = mFloatingView?.extractedTextContainer
        extractedTextContainer?.visibility = View.GONE
        cropView?.thisOptionsView = this

        extractedTextEt?.setOnFocusChangeListener { view, b ->
            if (b){
                setFocusOnThisView(true)
            }else {
                setFocusOnThisView(false)
            }
        }

    }

    private fun setFocusOnThisView(setFocusToThisView: Boolean) {

        val flag = if (setFocusToThisView) WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
         val initialX = params.x
        val initialY = params.y
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            flag,
            PixelFormat.TRANSLUCENT
        )
        params.x = initialX
        params.y = initialY
        params.gravity =
            Gravity.CENTER //Initially view will be added to top-left corner

        mWindowManager?.updateViewLayout(mFloatingView?.root, params)
    }

    private fun setFloatingView() {

        mFloatingView = OptionsViewBinding.inflate(context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)
                as LayoutInflater)

        //Add the view to the window.
         params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        //Specify the view position
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE

        //Specify the view position
        params.gravity =
            Gravity.CENTER  //Initially view will be added to top-left corner

        //Add the view to the window

        //Add the view to the window
        mWindowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
        mWindowManager?.addView(mFloatingView?.root, params)
    }

    fun destroyView() {
        mFloatingView?.root?.let {
            if(it.isAttachedToWindow){
                if (mFloatingView != null) {
                    mWindowManager?.removeView(mFloatingView?.root)

                }
            }
        }


    }

    fun setOnOnOptionsWindowSelected(onOptionsWindowSelectedListener: OnOptionsWindowSelectedListener){
        this.onOptionsWindowSelectedListener = onOptionsWindowSelectedListener
    }



    override fun onTouch(v: View?, event: MotionEvent?): Boolean {

        when(event?.actionMasked){
            ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isActionMove = false
                setChangeColor(v?.id, ACTION_DOWN)
            }
            ACTION_MOVE -> {
                params.x = initialX + getMovementInX(event)
                params.y = initialY + getMovementInY(event)
                isActionMove = true
                mWindowManager?.updateViewLayout(mFloatingView?.root, params)
                return true
            }

            ACTION_UP -> {
                setChangeColor(v?.id, ACTION_UP)
                if (getMovementInX(event) == 0 || getMovementInY(event) == 0) onTouchButton(v?.id)
            }
        }

        return true
    }
    private fun getMovementInY(event: MotionEvent): Int {
        return  (event.rawY - initialTouchY).toInt()
    }

    private fun getMovementInX(event: MotionEvent): Int {
        return  (event.rawX - initialTouchX).toInt()
    }

    private fun setChangeColor(id: Int?, action: Int) {

        if (action == ACTION_DOWN){

            when(id){
                R.id.save ->{ setTint(saveButtonBc!!, downColor) }
                R.id.delete ->{   setTint(deleteButtonBc!!, downColor)}
                R.id.share ->{  setTint(shareButtonBc!!, downColor)}
                R.id.edit ->{  setTint(editButtonBc!!, downColor)}
                R.id.min_max ->{   setTint(minMaxButtonBk!!, downColor)}
                R.id.extract_text ->{setTint(extractTxtBc!!, downColor)}
                R.id.copy_text ->{setTint(copyTextBk!!, downColor)}

            }
        } else{
            when(id){
                R.id.save ->{ setTint(saveButtonBc!!, upColor) }
                R.id.delete ->{   setTint(deleteButtonBc!!, upColor)}
                R.id.share ->{  setTint(shareButtonBc!!, upColor)}
                R.id.edit ->{  setTint(editButtonBc!!, upColor)}
                R.id.min_max ->{   setTint(minMaxButtonBk!!, upColor)}
                R.id.extract_text ->{setTint(extractTxtBc!!, upColor)}
                R.id.copy_text->{setTint(copyTextBk!!, upColor)}
            }
        }
    }

    private fun setTint(button: ImageView, color: Int) {
        DrawableCompat.setTint(
            DrawableCompat.wrap(button.drawable),
            ContextCompat.getColor(context, color)
        )
    }

     private fun onTouchButton(id: Int?) {

        when(id){
            R.id.save ->{ onOptionsWindowSelectedListener?.onSaveScreenshotSelected()}
            R.id.delete ->{onOptionsWindowSelectedListener?.onDeleteScreenshotSelected()}
            R.id.share ->{onOptionsWindowSelectedListener?.onShareScreenshotSelected()}
            R.id.edit ->{onOptionsWindowSelectedListener?.onEditScreenshotSelected()}
            R.id.min_max  ->{

                setMinMax()
                onOptionsWindowSelectedListener?.onMinimizeCropView()
            }
            R.id.extract_text ->{
                onOptionsWindowSelectedListener?.onExtractTextSelected()
            }
            R.id.copy_text -> getTextFromImage()
        }
    }

    private fun getTextFromImage() {
        copyTextToClipboard(context, extractedTextEt?.text.toString())

        extractedTextEt?.setText("", TextView.BufferType.EDITABLE)
        extractedTextContainer?.visibility = GONE

       setFocusOnThisView(false)

    }

    private fun setMinMax() {
        if (mainContainer?.isVisible == true){
            mainContainer?.visibility = View.GONE
            extractedTextContainer?.visibility = GONE
            minMaxButton?.setImageResource(R.drawable.ic_max)
        } else {
            mainContainer?.visibility = View.VISIBLE

           // extractedTextContainer?.visibility = if (extractedTextEt?.text?.isNotBlank() == true) VISIBLE else GONE
            minMaxButton?.setImageResource(R.drawable.ic_min)
        }
    }

    fun hideOptionsView(visibility: Int){
        mFloatingView?.root?.visibility = visibility
    }

    fun showExtractedText(text: String) {
        hideProgressBar()
        extractedTextEt?.setText(text, TextView.BufferType.EDITABLE)
        extractedTextContainer?.visibility = VISIBLE


    }

    fun showProgressBar() {
        progressBar?.visibility = VISIBLE
    }

    fun hideProgressBar() {
        progressBar?.visibility = GONE
    }

    fun disableActionButtons() {

        extractedTextContainer?.visibility = View.GONE

        saveButton?.setEnabledFeature(false, null)
        saveButtonBc?.setEnabledFeature(false, disabledColor)

        deleteButton?.setEnabledFeature(false, null)
        deleteButtonBc?.setEnabledFeature(false, disabledColor)

        shareButton?.setEnabledFeature(false, null)
        shareButtonBc?.setEnabledFeature(false, disabledColor)

        editButton?.setEnabledFeature(false, null)
        editButtonBc?.setEnabledFeature(false, disabledColor)

        extractTxtButton?.setEnabledFeature(false, null)
        extractTxtBc?.setEnabledFeature(false, disabledColor)
    }

    fun reEnableButtons(){
        saveButton?.setEnabledFeature(true, null)
        saveButtonBc?.setEnabledFeature(true, upColor)

        deleteButton?.setEnabledFeature(true, null)
        deleteButtonBc?.setEnabledFeature(true, upColor)

        shareButton?.setEnabledFeature(true, null)
        shareButtonBc?.setEnabledFeature(true, upColor)

        editButton?.setEnabledFeature(true, null)
        editButtonBc?.setEnabledFeature(true, upColor)

        extractTxtButton?.setEnabledFeature(true, null)
        extractTxtBc?.setEnabledFeature(true, upColor)

        minMaxButton?.setEnabledFeature(true, null)
        minMaxButtonBk?.setEnabledFeature(true, upColor)
    }
}

fun ImageView.setEnabledFeature(enabled: Boolean, backgroundColor: Int? ){
    isEnabled = enabled

    backgroundColor?.let {
        DrawableCompat.setTint(
            DrawableCompat.wrap(drawable),
            ContextCompat.getColor(context, backgroundColor)
        )
    }


}