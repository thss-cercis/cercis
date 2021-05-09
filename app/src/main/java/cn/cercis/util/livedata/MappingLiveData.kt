package cn.cercis.util.livedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer

open class MappingLiveData<T> : MediatorLiveData<T> {
    private var currentSource: LiveData<*>? = null
    private var savedObserver: Observer<in T>

    constructor(observer: MappingLiveData<T>.(T) -> Unit) {
        savedObserver = Observer { observer(it) }
    }

    constructor() {
        savedObserver = Observer { value = it }
    }

    constructor(source: LiveData<T>) : this() {
        setSource(source)
    }

    constructor(source: LiveData<T>, observer: MappingLiveData<T>.(T) -> Unit) : this(observer) {
        setSource(source)
    }

    fun setSource(source: LiveData<T>) {
        setSource(source, savedObserver)
    }

    fun <S> setSource(source: LiveData<S>, observer: Observer<in S>) {
        currentSource?.let { removeSource(it) }
        currentSource = source
        super.addSource(source, observer)
    }

    override fun <S> addSource(source: LiveData<S>, onChanged: Observer<in S>) {
        throw UnsupportedOperationException()
    }
}
