package cn.cercis.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import cn.cercis.entity.*
import cn.cercis.repository.AuthRepository
import cn.cercis.repository.MessageRepository
import cn.cercis.repository.UserRepository
import cn.cercis.util.helper.coroutineContext
import cn.cercis.util.livedata.asInitializedLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@FlowPreview
@ExperimentalCoroutinesApi
@HiltViewModel
class GroupInfoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    val groupInitData = savedStateHandle.get<Chat>("chat")!!
    val chatId = groupInitData.id
    val chat = messageRepository.getChat(chatId).filterNotNull()
        .asInitializedLiveData(coroutineContext, groupInitData)
    private val groupMemberListFlow = userRepository.run {
        viewModelScope.withFriends(messageRepository.getChatMemberList(chatId).flow()
            .map { it.data }.filterNotNull().flowOn(Dispatchers.IO), { userId }, false)
    }
    val groupMemberList = groupMemberListFlow.asInitializedLiveData(coroutineContext, listOf())
    val groupMemberCount = groupMemberList.map { it.size }
    val selfMember = groupMemberList.map { member -> member.firstOrNull { it.second?.id == authRepository.currentUserId } }
    val isGroupManager = selfMember.map { it?.let { it.first.permission >= GroupChatPermission.GROUP_ADMIN.value } ?: false }
}

fun Triple<ChatMember, User?, FriendEntry?>.toCommonListItemData(): CommonListItemData {
    val (chatMember, user, friendEntry) = this
    return CommonListItemData(
        displayName = chatMember.displayName.takeIf { !it.isNullOrEmpty() }
            ?: friendEntry?.displayName.takeIf { !it.isNullOrEmpty() } ?: user?.nickname ?: "",
        description = "",
        avatar = user?.avatar ?: "",
    )
}
