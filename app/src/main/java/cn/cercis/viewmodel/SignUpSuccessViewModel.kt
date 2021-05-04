package cn.cercis.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import cn.cercis.common.NO_USER
import cn.cercis.common.UserId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SignUpSuccessViewModel @Inject constructor(savedStateHandle: SavedStateHandle) : ViewModel() {
    val userId: UserId = savedStateHandle.get<Long>("userId") ?: NO_USER
}
