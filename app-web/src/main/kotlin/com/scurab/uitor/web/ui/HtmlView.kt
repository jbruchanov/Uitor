package com.scurab.uitor.web.ui

import com.scurab.uitor.common.util.Observable
import com.scurab.uitor.web.util.DocumentWrapper
import com.scurab.uitor.web.util.HasLifecycle
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import kotlin.dom.clear

abstract class HtmlView : HasLifecycle {
    abstract val element: HTMLElement?

    @Suppress("MemberVisibilityCanBePrivate")
    protected var parentElement: Element? = null
    private var attached: Boolean = false
    override val onDetachObservable = Observable<HasLifecycle>()
    protected var document = DocumentWrapper()

    abstract fun buildContent()

    fun attachTo(rootElement: Element) {
        check(!attached) { "This view has been already attached" }
        attached = true
        parentElement = rootElement
        if (element == null) {
            buildContent()
        }
        onAttachToRoot(rootElement)
        onAttached()
    }

    protected open fun onAttachToRoot(rootElement: Element) {
        element?.let { rootElement.appendChild(it) }
    }

    fun detach() {
        document.dispose()
        onDetachObservable.let {
            it.post(this)
            it.removeObservers()
        }
        parentElement?.clear()
        onDetached()
        attached = false
    }

    open fun onAttached() {
        //let subclass to do something
    }


    open fun onDetached() {
        //let subclass to do something
    }
}