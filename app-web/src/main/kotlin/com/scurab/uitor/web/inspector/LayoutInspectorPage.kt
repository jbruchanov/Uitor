package com.scurab.uitor.web.inspector

import com.scurab.uitor.web.model.ViewNode
import com.scurab.uitor.web.ui.ColumnsLayout
import com.scurab.uitor.web.ui.IColumnsLayoutDelegate
import com.scurab.uitor.web.util.requireElementById
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLTableElement
import org.w3c.dom.get
import kotlin.browser.document
import kotlin.browser.window
import kotlin.dom.clear
import kotlin.js.Json
import kotlin.math.max
import kotlin.math.min

private const val ID_LEFT = "split-table-left"
private const val ID_MID = "split-table-mid"
private const val ID_RIGHT = "split-table-right"

class LayoutInspectorPage {
    private val root = document.requireElementById("root")
    private lateinit var columnsLayout: ColumnsLayout
    private lateinit var canvasView: CanvasView
    private lateinit var treeView: TreeView
    private lateinit var propertiesView: PropertiesView
    private val inspectorViewModel = InspectorViewModel()

    fun onStart() {
        root.clear()
        columnsLayout = ColumnsLayout(root, ColumnsLayoutDelegate(this)).attach()
        canvasView = CanvasView(columnsLayout.left, inspectorViewModel)
        treeView = TreeView(columnsLayout.middle.first(), inspectorViewModel).attach()
        propertiesView = PropertiesView(columnsLayout.right, inspectorViewModel)

        GlobalScope.launch {
            async {
                val item = ViewNode(load())
                canvasView.renderMouseCross = true
                inspectorViewModel.rootNode.post(item)
                columnsLayout.initColumnSizes()
            }
            async {
                canvasView.loadImage("/device.png")
            }
        }
    }

    private suspend fun load(): Json {
        val text = window
            .fetch("viewhierarchy.json")
            .asDeferred()
            .await()
            .text()
            .asDeferred()
            .await()

        println(text)
        return JSON.parse(text)
    }

    class ColumnsLayoutDelegate(val page: LayoutInspectorPage) : IColumnsLayoutDelegate {
        override val innerContentWidthEstimator: (Int) -> Double = { column ->
            when(column) {
                2 -> {
                    7.0 + ((page.treeView.element as? HTMLTableElement)?.rows?.get(0)?.getBoundingClientRect()?.width
                        ?: window.innerWidth / 4.0)
                }
                else -> max(500.0, min(window.innerWidth, window.screen.width) / 3.0)
            }
        }
    }
}