package cn.edu.tsinghua.thss.cercis.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SessionListItemViewModel @Inject constructor() : ViewModel() {
    val avatar = MutableLiveData<String>()
    val sessionName = MutableLiveData<String>()
    val latestMessage = MutableLiveData<String>()
    val lastUpdate = MutableLiveData<String>()
    val unreadCount = MutableLiveData<Long>()
}
