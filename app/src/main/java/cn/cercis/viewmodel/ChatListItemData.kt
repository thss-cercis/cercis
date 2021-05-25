package cn.cercis.viewmodel

import cn.cercis.common.ChatId

class ChatListItemData(
    val chatId: ChatId,
    val avatar: String,
    val chatName: CharSequence,
    val latestMessage: CharSequence,
    val lastUpdate: String,
    val unreadCount: Long,
)
