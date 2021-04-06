package cn.edu.tsinghua.thss.cercis.viewmodel

import androidx.lifecycle.ViewModel
import cn.edu.tsinghua.thss.cercis.entity.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

class MessageViewModel(val side: Side, val message: Message) {
    enum class Side {
        THIS, OTHER
    }
}
