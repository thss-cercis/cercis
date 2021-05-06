package cn.cercis.util.helper

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers

val ViewModel.coroutineContext
    get() = viewModelScope.coroutineContext + Dispatchers.IO

fun AndroidViewModel.getString(resId: Int)
    = getApplication<Application>().getString(resId)
