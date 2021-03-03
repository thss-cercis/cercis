package cn.edu.tsinghua.thss.cercis.viewmodel

import android.text.TextUtils
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import javax.inject.Inject

class LoginViewModel @Inject constructor() : ViewModel() {
    val userId = MutableLiveData("")
    val password = MutableLiveData("")
    val isValid by lazy {
        val liveData = MediatorLiveData<Boolean>()
        liveData.addSource(userId) {
            liveData.value = !TextUtils.isEmpty(userId.value) && !TextUtils.isEmpty(password.value)
        }
        liveData.addSource(password) {
            liveData.value = !TextUtils.isEmpty(userId.value) && !TextUtils.isEmpty(password.value)
        }
        liveData
    }
}
