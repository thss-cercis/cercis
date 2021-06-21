package cn.cercis.util.helper

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
inline fun <reified T1, reified T2> instantCombine(flow1: Flow<T1>, flow2: Flow<T2>) = channelFlow {
    var f1: T1? = null
    var f2: T2? = null

    launch {
        flow1.collectLatest { emittedElement ->
            f1 = emittedElement
            send(f1 to f2)
        }
    }
    launch {
        flow2.collectLatest { emittedElement ->
            f2 = emittedElement
            send(f1 to f2)
        }
    }
}
