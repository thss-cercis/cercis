package cn.edu.tsinghua.thss.cercis.viewmodel

import cn.edu.tsinghua.thss.cercis.util.ChatId

class ChatListItemData(
        val sessionId: ChatId,
        val avatar: String,
        val sessionName: String,
        val latestMessage: String,
        val lastUpdate: String,
        val unreadCount: Long,
)
