package com.screenslicerpro.utils

import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.screenslicerpro.R
import com.screenslicerpro.database.ScreenshotItem
import com.screenslicerpro.gestures.action.database.AppItem
import com.screenslicerpro.gestures.view.viewmodel.GestureSettingsViewModel
import com.screenslicerpro.main_fragment.MainFragmentViewModel
import java.io.InputStream
import java.lang.Exception

@BindingAdapter("screenshotUri", "mainFragmentViewModel")
fun ImageView.setScreenshotUri(item: ScreenshotItem?,mainFragmentViewModel: MainFragmentViewModel?) {
    item?.let {

         val uriExist: Boolean = try {
             context.contentResolver.openInputStream(Uri.parse(item.uri))
             true
        }catch (e: Exception){
            Log.e("MyDeleteRequest", "Exception $e")
             false
        }

        if(uriExist){
            setImageURI(Uri.parse(item.uri))
        }

        if (drawable == null) {
            setBackgroundColor(Color.rgb(0, 0, 0))
            setImageResource(R.drawable.ic_baseline_broken_image_24)
            mainFragmentViewModel?.onDeleteListWithUri(listOf(item.uri))
        }
        }


}


@BindingAdapter("setIcon", "viewModel")
fun ImageView.setIcon(item: AppItem?, viewModel: GestureSettingsViewModel) {
    item?.let {

        val uriExist: Boolean = try {
            context.contentResolver.openInputStream(Uri.parse(item.appIconUri))
            true
        } catch (e: Exception) {
            Log.e("MyDeleteRequest", "Exception $e")
            false
        }

        if (uriExist) {
            setImageURI(Uri.parse(item.appIconUri))
        } else{
            viewModel.onDelete(item)
        }

    }
}