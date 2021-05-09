package cn.cercis.util.livedata

import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import cn.cercis.common.LOG_TAG

class AutoResetLiveData<T>(value: T) : MutableLiveData<T>() {
    private val initialValue = value

    init {
        this.value = value
    }

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        if (hasActiveObservers()) {
            Log.w(LOG_TAG, "Multiple observers registered but only one will be notified of changes.")
        }

        super.observe(owner) {
            if (value != initialValue) {
                observer.onChanged(it)
                value = initialValue
            }
        }
    }
}
