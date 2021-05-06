package cn.cercis.viewmodel

import androidx.lifecycle.LiveData
import cn.cercis.entity.Message
import cn.cercis.entity.User
import cn.cercis.util.resource.Resource

data class MessageViewModel(
    val message: Message,
    val user: LiveData<Resource<User>>,
)
