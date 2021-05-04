package cn.cercis.viewmodel

import cn.cercis.util.ChatId

class ChatListItemData(
        val chatId: ChatId,
        val avatar: String,
        val chatName: String,
        val latestMessage: String,
        val lastUpdate: String,
        val unreadCount: Long,
)