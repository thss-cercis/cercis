package cn.cercis.viewmodel

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.cercis.repository.ProfileRepository
import cn.cercis.util.livedata.generateMediatorLiveData
import cn.cercis.util.resource.NetworkResponse
import cn.cercis.util.validation.BIO_MAX_LENGTH
import cn.cercis.util.validation.EMAIL_REGEX
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import javax.inject.Inject

@FlowPreview
@ExperimentalCoroutinesApi
@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    enum class NavAction {
        STAY, BACK
    }
    val navAction = MutableLiveData(NavAction.STAY)

    val nickname = MutableLiveData(savedStateHandle.get<String>("nickname"))
    val email = MutableLiveData(savedStateHandle.get<String>("email"))
    val bio = MutableLiveData(savedStateHandle.get<String>("bio"))

    val error = MutableLiveData<String?>(null)

    private val isBusy = MutableLiveData(false)

    val canSubmit = generateMediatorLiveData(nickname, email, bio, isBusy) {
        nickname.value!!.isNotEmpty()
            && email.value!! matches EMAIL_REGEX
            && bio.value!!.length <= BIO_MAX_LENGTH
            && isBusy.value == false
    }

    fun onSubmit(@Suppress("UNUSED_PARAMETER") view: View) {
        viewModelScope.launch(Dispatchers.IO) {
            error.postValue(null)
            isBusy.postValue(true)
            try {
                val response = profileRepository.updateCurrentUserDetail(
                    nickname = nickname.value,
                    avatar = null,
                    bio = bio.value,
                    email = email.value,
                )
                when (response) {
                    is NetworkResponse.Success -> {
                        profileRepository.profileChanged.postValue(true)
                        navAction.postValue(NavAction.BACK)
                    }
                    is NetworkResponse.Reject,
                    is NetworkResponse.NetworkError -> {
                        error.postValue(response.message)
                    }
                }
            } finally {
                isBusy.postValue(false)
            }
        }
    }
}