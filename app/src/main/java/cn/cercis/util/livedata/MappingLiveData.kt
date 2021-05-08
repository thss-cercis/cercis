package cn.cercis.util.livedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import java.lang.UnsupportedOperationException

class MappingLiveData<T>() : MediatorLiveData<T>() {
    private var currentSource: LiveData<*>? = null
    private lateinit var savedObserver: Observer<in T>

    constructor(source: LiveData<T>, observer: MappingLiveData<T>.(T) -> Unit) : this() {
        currentSource = source
        savedObserver = Observer { observer(it) }
        setSource(source)
    }

    constructor(source: LiveData<T>) : this(source, { value = it })

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
