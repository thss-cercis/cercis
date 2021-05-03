package cn.edu.tsinghua.thss.cercis.viewmodel

import androidx.lifecycle.LiveData
import cn.edu.tsinghua.thss.cercis.entity.Message
import cn.edu.tsinghua.thss.cercis.entity.User
import cn.edu.tsinghua.thss.cercis.util.Resource

data class MessageViewModel(
        val message: Message,
        val user: LiveData<Resource<User>>,
)
