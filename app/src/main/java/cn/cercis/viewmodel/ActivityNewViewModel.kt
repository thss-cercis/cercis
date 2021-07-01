package cn.cercis.viewmodel

import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import cn.cercis.Constants
import cn.cercis.R
import cn.cercis.common.LOG_TAG
import cn.cercis.repository.ActivityRepository
import cn.cercis.util.getString
import cn.cercis.util.helper.FileUploadUtils
import cn.cercis.util.livedata.AutoResetLiveData
import cn.cercis.util.livedata.generateMediatorLiveData
import cn.cercis.util.resource.NetworkResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@FlowPreview
@ExperimentalCoroutinesApi
@HiltViewModel
class ActivityNewViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val fileUploadUtils: FileUploadUtils,
) : ViewModel() {
    val text = MutableLiveData("")
    private val imageUrls = mutableListOf<String>()
    private val imageListUpdated = AutoResetLiveData(false)

    private val isBusy = MutableLiveData(false)

    val canSubmit = generateMediatorLiveData(text, imageListUpdated, isBusy) {
        isBusy.value == false && (text.value!!.isNotEmpty() || imageUrls.isNotEmpty())
    }
    val canAddImage = generateMediatorLiveData(imageListUpdated, isBusy) {
        isBusy.value == false && imageUrls.size < 9
    }

    val hint = imageListUpdated.map {
        getString(R.string.hint_image_count).format(imageUrls.size)
    }
    val isHintVisible = imageListUpdated.map {
        when {
            imageUrls.isEmpty() -> View.GONE
            else -> View.VISIBLE
        }
    }

    fun addImage(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = fileUploadUtils.uploadFile(file)
            if (response is NetworkResponse.Success) {
                val url = Constants.STATIC_BASE + response.data
                imageUrls.add(url)
                imageListUpdated.postValue(true)
            }
        }
    }

    fun submit(onSuccess: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            isBusy.postValue(true)
            try {
                Log.d(LOG_TAG, "$imageUrls")
                val response = activityRepository.publishNormalActivity(
                    text = text.value!!,
                    imageUrls = imageUrls
                )
                launch(Dispatchers.Main) {
                    if (response is NetworkResponse.Success) {
                        onSuccess()
                    } else {
                        onFailure()
                    }
                }
            } finally {
                isBusy.postValue(false)
            }
        }
    }
}
