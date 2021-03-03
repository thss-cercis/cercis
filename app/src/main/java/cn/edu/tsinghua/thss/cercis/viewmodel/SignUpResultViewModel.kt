package cn.edu.tsinghua.thss.cercis.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cn.edu.tsinghua.thss.cercis.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SignUpResultViewModel @Inject constructor(): ViewModel() {
    val userId: MutableLiveData<Long> by lazy {
        MutableLiveData<Long>(0)
    }
}
