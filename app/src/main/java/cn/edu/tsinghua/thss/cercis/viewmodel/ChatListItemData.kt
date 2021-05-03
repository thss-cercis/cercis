package cn.edu.tsinghua.thss.cercis.viewmodel

import cn.edu.tsinghua.thss.cercis.util.ChatId

class ChatListItemData(
        val chatId: ChatId,
        val avatar: String,
        val chatName: String,
        val latestMessage: String,
        val lastUpdate: String,
        val unreadCount: Long,
)
