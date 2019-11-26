package com.scurab.uitor.web

import com.scurab.uitor.common.util.ise
import com.scurab.uitor.common.util.ref
import com.scurab.uitor.web.common.Page
import com.scurab.uitor.web.common.ServerApi
import com.scurab.uitor.web.filebrowser.FileBrowserPage
import com.scurab.uitor.web.groovy.GroovyPage
import com.scurab.uitor.web.inspector.LayoutInspectorPage
import com.scurab.uitor.web.model.ClientConfig
import com.scurab.uitor.web.model.PageViewModel
import com.scurab.uitor.web.model.Snapshot
import com.scurab.uitor.web.resources.ResourcesPage
import com.scurab.uitor.web.screen.ScreenComponentsPage
import com.scurab.uitor.web.threed.ThreeDPage
import com.scurab.uitor.web.tree.TidyTreePage
import com.scurab.uitor.web.ui.launchWithProgressBar
import com.scurab.uitor.web.util.browserDownload
import com.scurab.uitor.web.util.lazyLifecycled
import com.scurab.uitor.web.util.readAsText
import com.scurab.uitor.web.util.removeAll
import com.scurab.uitor.web.util.requireElementById
import kotlinx.coroutines.GlobalScope
import kotlinx.html.InputType
import kotlinx.html.TABLE
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.option
import kotlinx.html.select
import kotlinx.html.style
import kotlinx.html.table
import kotlinx.html.td
import kotlinx.html.tr
import kotlinx.html.unsafe
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import kotlin.browser.window
import kotlin.dom.clear

private const val ID_SCREEN_INDEX = "main-screen-index"
private const val DEVICE_INFO = "main-screen-device-info"

class MainPage(private var clientConfig: ClientConfig) : Page() {

    override var element: HTMLElement? = null
        private set

    private val screensSelect by lazyLifecycled { element.ref.requireElementById<HTMLSelectElement>(ID_SCREEN_INDEX) }
    private val serverApi get() = App.serverApi
    private val screenIndexOptional: Int?
        get() = screensSelect.selectedIndex.takeIf { it >= 0 }
    private val screenIndex: Int
        get() = screenIndexOptional ?: ise("Invalid screen selection")

    override fun buildContent() {
        //let anyone who is bound to this to reset the state
        onDetachObservable.post(this)
        val el = element ?: document.create.div()
        element = el
        el.clear()
        document.create.div {
            style = "text-align: center; padding:10px;"
            div(classes = DEVICE_INFO) {
                unsafe { raw(clientConfig.deviceInfo.replace("\n", "<br/>")) }
            }
            select {
                id = ID_SCREEN_INDEX
            }
            table {
                style = "margin-left:auto;margin-right:auto;"
                createPageButton("LayoutInspectorPage", "Layout Inspector")
                { LayoutInspectorPage(PageViewModel(screenIndex)) }
                createPageButton("ThreeDPage", "3D Inspector")
                { ThreeDPage(PageViewModel(screenIndex)) }
                createPageButton("TidyTreePage", "View Hierarchy")
                { TidyTreePage(PageViewModel(screenIndex)) }
                createPageButton("ResourcesPage", "Resources", true)
                { ResourcesPage(PageViewModel(screenIndexOptional ?: -1)) }
                createPageButton("FileBrowserPage", "File Browser", true)
                { FileBrowserPage(PageViewModel(screenIndexOptional ?: -1)) }
                createPageButton("WindowsPage", "Windows") {
                    ScreenComponentsPage(
                        PageViewModel(
                            screenIndexOptional ?: -1
                        )
                    )
                }
                createLinkButton("WindowsDetailedPage", "Windows Detailed") { "screenstructure" }
                createLinkButton("ScreenshotPage", "Screenshot") { ServerApi.screenShotUrl(screenIndex) }
                createLinkButton("LogCatPage", "LogCat") { "logcat" }
                createPageButton("GroovyPage", "Groovy", true) {
                    GroovyPage(
                        PageViewModel(screenIndexOptional ?: -1),
                        null
                    )
                }
                createButton("", "Save") {
                    launchWithProgressBar {
                        val obj = serverApi.snapshot(screenIndex)
                        browserDownload(JSON.stringify(obj), "snapshot.json", "application/json")
                    }
                }
                input {
                    type = InputType.file
                    accept = "application/json"
                    onChangeFunction = { event ->
                        val files = (event.target as? HTMLInputElement)?.files
                        val file = files
                            ?.takeIf { it.length > 0 }
                            ?.item(0)
                            ?.let { file ->
                                GlobalScope.launchWithProgressBar {
                                    val snapshot = JSON.parse<Snapshot>(file.readAsText())
                                    App.setSnapshot(snapshot)
                                    clientConfig = App.clientConfig
                                    buildContent()
                                    reloadScreens()
                                }
                            }

                    }
                }
            }
        }.apply {
            el.append(this)
        }
    }

    override fun onAttached() {
        super.onAttached()
        reloadScreens()
    }

    override fun stateDescription(): String? = null

    private fun reloadScreens() {
        launchWithProgressBar {
            screensSelect.disabled = true
            screensSelect.options.removeAll()
            val activeScreens = serverApi.activeScreens()
            activeScreens.forEach {
                screensSelect.add(document.create.option { text(it) })
            }
            screensSelect.selectedIndex = activeScreens.size - 1
            screensSelect.disabled = false
        }
    }


    private fun TABLE.createPageButton(key: String, title: String, demoDisabled: Boolean = false, block: () -> Page) {
        createButton(key, title) {
            if (demoDisabled && DEMO) {
                alert("Sorry, $title is not supported in demo!")
                return@createButton
            }
            try {
                Navigation.open(block(), true)
            } catch (e: Throwable) {
                alert(e)
            }
        }
    }

    private fun TABLE.createLinkButton(key: String, title: String, block: () -> String) {
        createButton(key, title) {
            try {
                window.open(block(), "_blank", "")
            } catch (e: Throwable) {
                alert(e)
            }
        }
    }

    private fun TABLE.createButton(key: String, title: String, clickAction: () -> Unit) {
        if (key.isNotEmpty() && !clientConfig.pages.contains(key)) {
            return
        }
        tr {
            td {
                button {
                    style = "width:100%"
                    text(title)
                    onClickFunction = { clickAction() }
                }
            }
        }
    }
}
