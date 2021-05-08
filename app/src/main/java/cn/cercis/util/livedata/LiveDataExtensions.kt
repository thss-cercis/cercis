package cn.cercis.util.livedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import cn.cercis.util.resource.Resource

fun <T> MediatorLiveData<T>.addSource(source: LiveData<T>) {
    addSource(source) {
        value = it
    }
}

fun <T> MediatorLiveData<T>.addResource(source: LiveData<Resource<T>>) {
    addSource(source) {
        value = it.data
    }
}

fun <T> MediatorLiveData<T>.addMultipleSource(
    vararg sources: LiveData<*>,
    onChanged: () -> Unit
) {
    sources.forEach {
        addSource(it) { onChanged() }
    }
}

fun <T> generateMediatorLiveData(
    vararg sources: LiveData<*>,
    updateValue: () -> T
) = MediatorLiveData<T>().apply {
    addMultipleSource(*sources) {
        value = updateValue()
    }
}
