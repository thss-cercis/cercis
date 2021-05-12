package cn.cercis.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import cn.cercis.common.NO_USER
import cn.cercis.common.UserId
import cn.cercis.entity.User
import cn.cercis.repository.MessageRepository
import cn.cercis.repository.UserRepository
import cn.cercis.util.helper.coroutineContext
import cn.cercis.util.resource.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@FlowPreview
@ExperimentalCoroutinesApi
@HiltViewModel
class UserInfoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    userRepository: UserRepository,
    messageRepository: MessageRepository,
) : ViewModel() {
    private val userId = savedStateHandle.get<UserId>("user_id") ?: NO_USER
    private val userInfoFlow = flow {
        // if no initial value is passed, only use DataSource as the data source
        savedStateHandle.get<User>("user")?.let {
            emit(Resource.Loading(it))
        }
        (userRepository.getUser(userId).flow()).collectLatest {
            emit(it)
        }
    }
    val userInfo = userInfoFlow.asLiveData(coroutineContext)

    fun openChat() {
    }
}
