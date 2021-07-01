package cn.cercis.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import cn.cercis.entity.UserDetail
import cn.cercis.http.EmptyNetworkResponse
import cn.cercis.repository.AuthRepository
import cn.cercis.repository.ProfileRepository
import cn.cercis.util.helper.coroutineContext
import cn.cercis.util.livedata.MappingLiveData
import cn.cercis.util.livedata.addResource
import cn.cercis.util.resource.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
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

    suspend fun changePassword(originalPassword: String, newPassword: String): EmptyNetworkResponse {
        return profileRepository.changePassword(originalPassword, newPassword)
    }

    private fun generateCurrentUserResource(): LiveData<Resource<UserDetail>> {
        return profileRepository.getCurrentUserDetail().asLiveData(coroutineContext)
    }
}
