package cn.cercis.util.livedata

import androidx.annotation.MainThread
import androidx.lifecycle.*
import cn.cercis.util.resource.Resource
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.CoroutineContext

fun <T> MediatorLiveData<T>.addSource(source: LiveData<T>) {
    addSource(source) {
        value = it
    }
}

fun <T> MediatorLiveData<T>.addResource(source: LiveData<Resource<T>>) {
    addSource(source) {
        value = it?.data
    }
}

fun <T> LiveData<Resource<T>>.unwrapResource(): LiveData<T> {
    return MediatorLiveData<T>().also {
        it.addResource(this)
    }
}

fun <T> MediatorLiveData<T>.addMultipleSource(
    vararg sources: LiveData<*>,
    onChanged: () -> Unit,
) {
    sources.forEach {
        addSource(it) { onChanged() }
    }
}

fun <T> generateMediatorLiveData(
    vararg sources: LiveData<*>,
    updateValue: () -> T,
) = MediatorLiveData<T>().apply {
    addMultipleSource(*sources) {
        value = updateValue()
    }
}

/**
 * Converts a flow into a livedata, with initial value provided.
 *
 * * NOTE: this method should only be called from the main thread
 */
@MainThread
fun <T> Flow<T>.asInitializedLiveData(
    coroutineContext: CoroutineContext,
    initialValue: T,
): LiveData<T> {
    return this.asLiveData(coroutineContext).apply {
        (this as MutableLiveData).value = initialValue
    }
}

fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<in T>) {
    this.observe(lifecycleOwner, object : Observer<T> {
        override fun onChanged(t: T) {
            removeObserver(this)
            observer.onChanged(t)
        }
    })
}

fun <T> LiveData<T>.observeFilterFirst(
    lifecycleOwner: LifecycleOwner,
    until: (T) -> Boolean,
    observer: Observer<in T>,
) {
    this.observe(lifecycleOwner, object : Observer<T> {
        override fun onChanged(t: T) {
            if (until(t)) {
                removeObserver(this)
                observer.onChanged(t)
            }
        }
    })
}
