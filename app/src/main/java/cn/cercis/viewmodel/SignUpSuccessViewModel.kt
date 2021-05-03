package cn.cercis.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import cn.cercis.util.UserId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SignUpSuccessViewModel @Inject constructor(savedStateHandle: SavedStateHandle) : ViewModel() {
    val userId: UserId = savedStateHandle.get<Long>("userId") ?: -1
}
