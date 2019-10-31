package com.scurab.uitor.web.common

import com.scurab.uitor.web.inspector.InspectorViewModel
import com.scurab.uitor.web.model.PageViewModel
import com.scurab.uitor.web.ui.ColumnsLayout
import com.scurab.uitor.web.ui.IColumnsLayoutDelegate
import com.scurab.uitor.web.ui.table.TableViewDelegate
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import kotlin.browser.window
import kotlin.math.max

abstract class BaseViewPropertiesPage(pageViewModel: PageViewModel) : InspectorPage(InspectorViewModel(pageViewModel)) {
    final override var element: HTMLElement? = null; private set

    abstract val contentElement: HTMLElement?

    private var viewPropertiesTableView = ViewPropertiesTableView(TableViewDelegate.default(viewModel.clientConfig), viewModel.screenIndex)
    private val columnsLayoutDelegate = object : IColumnsLayoutDelegate {
        override val innerContentWidthEstimator: (Int) -> Double = { column ->
            val windowWidth = window.innerWidth - ColumnsLayout.UNKNOWNGAP
            val w = if (viewModel.selectedNode.item != null) max(windowWidth / 3.0, 600.0) else 0.0
            if (column == 0) windowWidth - w else w
        }
    }
    protected val columnsLayout = ColumnsLayout(columnsLayoutDelegate, 2)
    private var expandViewPropsColumn = true

    override fun onAttachToRoot(rootElement: Element) {
        super.onAttachToRoot(rootElement)
        columnsLayout.left.append(contentElement)
        viewPropertiesTableView.attachTo(columnsLayout.right)
    }

    override fun buildContent() {
        columnsLayout.buildContent()
        viewPropertiesTableView.buildContent()
        element = columnsLayout.element
    }

    override fun onAttached() {
        super.onAttached()
        columnsLayout.onAttached()
        viewModel.selectedNode.observe {
            viewPropertiesTableView.viewNode = it
            if (expandViewPropsColumn) {
                expandViewPropsColumn = false
                columnsLayout.setGridTemplateColumns("1fr 5px 600px")
                onColumnsResize(doubleArrayOf(columnsLayout.left.getBoundingClientRect().width, 5.0, 600.0))
            }
        }
        columnsLayout.onResize = this::onColumnsResize
        columnsLayout.initColumnSizes()
    }

    override fun onDetached() {
        columnsLayout.onResize = null
        super.onDetached()
    }

    protected open fun onColumnsResize(sizes: DoubleArray) {
        //subclass
    }
}