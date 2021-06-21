package cn.cercis.viewmodel

import android.net.Uri
import android.util.Log
import android.view.View
import androidx.core.net.toFile
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.cercis.Constants.STATIC_BASE
import cn.cercis.common.LOG_TAG
import cn.cercis.entity.UserDetail
import cn.cercis.repository.AuthRepository
import cn.cercis.repository.ProfileRepository
import cn.cercis.repository.UserRepository
import cn.cercis.util.helper.FileUploadUtils
import cn.cercis.util.livedata.AutoResetLiveData
import cn.cercis.util.livedata.generateMediatorLiveData
import cn.cercis.util.resource.NetworkResponse
import cn.cercis.util.validation.BIO_MAX_LENGTH
import cn.cercis.util.validation.EMAIL_REGEX
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

@FlowPreview
@ExperimentalCoroutinesApi
@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val profileRepository: ProfileRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val fileUploadUtils: FileUploadUtils,
) : ViewModel() {
    enum class NavAction {
        STAY, BACK
    }

    val navAction = MutableLiveData(NavAction.STAY)

    private val initialUserDetail = savedStateHandle.get<UserDetail>("user")!!
    val avatarUrl = MutableLiveData(initialUserDetail.avatar)
    val avatarUploading = MutableLiveData(false)
    val avatarUploadResult = AutoResetLiveData<Pair<Uri, NetworkResponse<String>>?>(null)
    val nickname = MutableLiveData(initialUserDetail.nickname)
    val email = MutableLiveData(initialUserDetail.email)
    val bio = MutableLiveData(initialUserDetail.bio)

    val error = MutableLiveData<String?>(null)

    private val isBusy = MutableLiveData(false)

    val canSubmit = generateMediatorLiveData(nickname, email, bio, isBusy) {
        nickname.value!!.isNotEmpty()
                && email.value!! matches EMAIL_REGEX
                && bio.value!!.length <= BIO_MAX_LENGTH
                && isBusy.value == false
    }

    fun uploadAvatar(uri: Uri) {
        avatarUploading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                fileUploadUtils.uploadFile(uri.toFile()).let {
                    avatarUploadResult.postValue(uri to it)
                    Log.d(this@ProfileEditViewModel.LOG_TAG, "uploaded avatar: $it")
                    if (it is NetworkResponse.Success) {
                        avatarUrl.postValue(STATIC_BASE + it.data)
                    }
                    userRepository.getUser(authRepository.currentUserId).fetchAndSave()
                }
            } finally {
                avatarUploading.postValue(false)
            }
        }
    }

    fun onSubmit(@Suppress("UNUSED_PARAMETER") view: View) {
        viewModelScope.launch(Dispatchers.IO) {
            error.postValue(null)
            isBusy.postValue(true)
            try {
                val response = profileRepository.updateCurrentUserDetail(
                    nickname = nickname.value,
                    avatar = avatarUrl.value,
                    bio = bio.value,
                    email = email.value,
                )
                when (response) {
                    is NetworkResponse.Success -> {
                        profileRepository.profileChanged.postValue(true)
                        navAction.postValue(NavAction.BACK)
                    }
                    is NetworkResponse.Reject,
                    is NetworkResponse.NetworkError,
                    -> {
                        error.postValue(response.message)
                    }
                }
                userRepository.getUser(authRepository.currentUserId).fetchAndSave()
            } finally {
                isBusy.postValue(false)
            }
        }
    }
}
