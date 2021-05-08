package cn.cercis.viewmodel

import android.view.View
import androidx.lifecycle.*
import cn.cercis.entity.UserDetail
import cn.cercis.repository.AuthRepository
import cn.cercis.repository.ProfileRepository
import cn.cercis.util.helper.coroutineContext
import cn.cercis.util.livedata.MappingLiveData
import cn.cercis.util.livedata.addResource
import cn.cercis.util.resource.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import javax.inject.Inject

@FlowPreview
@ExperimentalCoroutinesApi
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val currentUserResource = MappingLiveData(generateCurrentUserResource())

    val currentUser = MediatorLiveData<UserDetail>().apply {
        addResource(currentUserResource)
        addSource(profileRepository.profileChanged) {
            if (it == true) {
                currentUserResource.setSource(generateCurrentUserResource())
            }
        }
    }

    val currentUserLoading = Transformations.map(currentUserResource) {
        it?.let { it is Resource.Loading } ?: false
    }

    val currentUserLoadingStatus = Transformations.map(currentUserResource) {
        when (it) {
            is Resource.Error -> "Error: ${it.message}"
            is Resource.Loading -> "Loading, showing local cache"
            is Resource.Success -> "Success!"
            else -> "Preparing request"
        }
    }

    private fun generateCurrentUserResource(): LiveData<Resource<UserDetail>> {
        return profileRepository.getCurrentUserDetail().flow().asLiveData(coroutineContext)
    }

    fun onLogoutClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        viewModelScope.launch(Dispatchers.IO) {
            authRepository.logout()
        }
    }
}
