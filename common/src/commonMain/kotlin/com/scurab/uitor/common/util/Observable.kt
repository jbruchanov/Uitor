package com.scurab.uitor.common.util

private val NULL = Unit

class Observable<T> {
    private val observers = mutableListOf<(T?) -> Unit>()
    @Suppress("UNCHECKED_CAST")
    private var _item: T? = NULL as T?
    val item: T? get() = _item.takeIf { it != NULL }

    fun post(item: T?) {
        _item = item
        notifyObservers()
    }

    fun observe(observer: (T?) -> Unit) {
        observers.add(observer)
        observer(item)
    }

    private fun notifyObservers() {
        if (_item != NULL) {
            observers.forEach {
                it(item)
            }
        }
    }
}