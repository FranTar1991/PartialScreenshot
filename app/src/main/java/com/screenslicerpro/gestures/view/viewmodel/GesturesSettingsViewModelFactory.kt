package com.screenslicerpro.gestures.view.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.screenslicerpro.gestures.action.database.AppsDAO

class GesturesSettingsViewModelFactory (
    private val dataSource: AppsDAO,
    private val sharedPreferences: SharedPreferences?,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GestureSettingsViewModel::class.java)) {
            return GestureSettingsViewModel(dataSource, sharedPreferences, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}