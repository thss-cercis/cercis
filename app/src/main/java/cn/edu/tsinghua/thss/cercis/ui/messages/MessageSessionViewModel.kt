package cn.edu.tsinghua.thss.cercis.ui.messages

import androidx.databinding.ObservableField
import androidx.databinding.ObservableLong

data class MessageSessionViewModel(
        var avatar: ObservableField<String>,
        var sessionName: ObservableField<String>,
        var latestMessage: ObservableField<String>,
        var lastUpdate: ObservableField<String>,
        var unreadCount: ObservableLong
)