package cn.edu.tsinghua.thss.cercis.ui.signup

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cn.edu.tsinghua.thss.cercis.R

class SignUpResultViewModel : ViewModel() {
    val userId: MutableLiveData<Long> by lazy {
        MutableLiveData<Long>(0)
    }
}
