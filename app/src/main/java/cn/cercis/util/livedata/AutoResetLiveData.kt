package cn.cercis.util.livedata

import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

class AutoResetLiveData<T>(value: T) : MutableLiveData<T>() {
    private val initialValue = value

    init {
        this.value = value
    }

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        if (hasActiveObservers()) {
            Log.w(TAG, "Multiple observers registered but only one will be notified of changes.")
        }

        super.observe(owner) {
            if (value != initialValue) {
                observer.onChanged(it)
                value = initialValue
            }
        }
    }

    companion object {
        private val TAG = AutoResetLiveData::class.simpleName
    }
}
