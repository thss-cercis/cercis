package cn.cercis.util.helper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers

val ViewModel.coroutineContext
    get() = viewModelScope.coroutineContext + Dispatchers.IO
