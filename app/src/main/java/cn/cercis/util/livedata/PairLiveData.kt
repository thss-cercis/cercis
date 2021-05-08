package cn.cercis.util.livedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

class PairLiveData<A, B>(
    first: LiveData<A>,
    second: LiveData<B>
) : MediatorLiveData<Pair<A?, B?>>() {
    init {
        addMultipleSource(first, second) {
            value = first.value to second.value
        }
    }
}
