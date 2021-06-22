package cn.cercis.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cn.cercis.repository.AuthRepository
import cn.cercis.util.helper.FileUploadUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import javax.inject.Inject

@FlowPreview
@ExperimentalCoroutinesApi
@HiltViewModel
class ActivityNewViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val fileUploadUtils: FileUploadUtils,
) : ViewModel() {
    val canSubmit = MutableLiveData(true)

    fun submit() {
    }
}
