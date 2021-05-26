package cn.cercis.viewmodel

import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.*
import cn.cercis.common.LOG_TAG
import cn.cercis.common.UserId
import cn.cercis.entity.Chat
import cn.cercis.entity.ChatMember
import cn.cercis.entity.FriendEntry
import cn.cercis.entity.User
import cn.cercis.repository.MessageRepository
import cn.cercis.repository.UserRepository
import cn.cercis.util.helper.coroutineContext
import cn.cercis.util.livedata.asInitializedLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@FlowPreview
@ExperimentalCoroutinesApi
@HiltViewModel
class GroupInfoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
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
