package cn.cercis.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer

// https://stackoverflow.com/a/53628300
class PairLiveData<A, B>(first: LiveData<A>, second: LiveData<B>) :
    MediatorLiveData<Pair<A?, B?>>() {
    init {
        addSource(first) { value = it to second.value }
        addSource(second) { value = first.value to it }
    }
}

class TripleLiveData<A, B, C>(first: LiveData<A>, second: LiveData<B>, third: LiveData<C>) :
    MediatorLiveData<Triple<A?, B?, C?>>() {
    init {
        addSource(first) { value = Triple(it, second.value, third.value) }
        addSource(second) { value = Triple(first.value, it, third.value) }
        addSource(third) { value = Triple(first.value, second.value, it) }
    }
}

class SourceReplaceableLiveData<T> : MediatorLiveData<T>() {
    private var currentSource: LiveData<*>? = null

    fun <S> setSource(liveData: LiveData<S>, observer: Observer<in S>) {
        currentSource.let {
            it?.let { removeSource(it) }
            currentSource = liveData
            addSource(liveData, observer)
        }
    }
}