@file:Suppress("MemberVisibilityCanBePrivate")

package com.scurab.uitor.web.model

import com.scurab.uitor.common.model.IViewNode
import com.scurab.uitor.common.model.ViewNodeFields
import com.scurab.uitor.common.render.Rect
import com.scurab.uitor.common.util.dlog
import com.scurab.uitor.common.util.forEachReversed
import com.scurab.uitor.web.util.getMap
import com.scurab.uitor.web.util.getTypedListOf
import com.scurab.uitor.web.util.jsonField
import com.scurab.uitor.web.util.optJsonField
import kotlin.js.Json

class ViewNode(json: Json) : IViewNode {

    override val idi: Int by jsonField(json, ViewNodeFields.IDi)
    override val ids: String? by optJsonField(json, ViewNodeFields.IDs)
    override val level: Int by jsonField(json, ViewNodeFields.Level)
    override val position: Int by jsonField(json, ViewNodeFields.Position)
    override val owner: String by jsonField(json, ViewNodeFields.Owner)

    val rawdata: Map<String, Any?> = json.getMap(ViewNodeFields.Data)
    override val data: Map<String, Any?> = rawdata
        .toMutableMap()
        .apply {
            //TODO do this on server
            this[ViewNodeFields.IDi] = idi
            this[ViewNodeFields.IDs] = ids
            this[ViewNodeFields.Level] = level
            this[ViewNodeFields.Position] = position
            this[ViewNodeFields.Owner] = owner
        }
        .filter { !it.key.startsWith("_") }

    val dataSortedKeys = data.keys.sortedWith(COMPARATOR)

    override val nodes: List<ViewNode> = json.getTypedListOf(ViewNodeFields.Nodes) {
        try {
            ViewNode(it)
        } catch (e: Exception) {
            dlog { "Unable to create view node for position:${it.asDynamic().Position}, error:${e.message}" }
            throw e
        }
    }

    override val rect: Rect by lazy {
        Rect(
            locationScreenX,
            locationScreenY,
            data.int(ViewNodeFields.Width),
            data.int(ViewNodeFields.Height)
        )
    }
    val locationScreenX: Int get() = data.int(ViewNodeFields.LocationScreenX)
    val locationScreenY: Int get() = data.int(ViewNodeFields.LocationScreenY)
    val typeSimple: String by lazy { data.string(ViewNodeFields.Type).substringAfterLast(".") }
    val type: String get() = data.string(ViewNodeFields.Type)

    override fun findFrontVisibleView(x: Int, y: Int, ignore: Set<IViewNode>): ViewNode? {
        //disabled for now, this makes views inactive actitivies "invisible" for search
        if (false && (data.int(ViewNodeFields.InternalVisibility)) != 0) {//not visible
            return null;
        }

        if (rect.contains(x, y)) {
            //post-order
            nodes.forEachReversed { node ->
                val candidate = node.findFrontVisibleView(x, y, ignore)
                if (candidate != null) {
                    return candidate
                }
            }
            if (ignore.contains(this)) {
                return null
            }
            return this
        }
        return null
    }

    fun forEachIndexed(block: (Int, ViewNode) -> Unit) {
        block(position, this)
        nodes.forEach { it.forEachIndexed(block) }
    }

    private fun Map<String, Any?>.int(key: String): Int {
        try {
            return this[key] as Int
        } catch (e: Exception) {
            println("Unable to get Int of key:$key in $this")
            throw e
        }
    }

    private fun Map<String, Any?>.string(key: String): String {
        try {
            return this[key] as String
        } catch (e: Exception) {
            println("Unable to get String of key:$key in $this")
            throw e
        }
    }

    companion object {
        val COMPARATOR: Comparator<String> = object : Comparator<String> {
            private val orderMap = mapOf(
                // @formatter:off
                Pair(ViewNodeFields.Type,       "001"),
                Pair(ViewNodeFields.IDi,        "002"),
                Pair(ViewNodeFields.IDs,        "003"),
                Pair(ViewNodeFields.Level,      "004"),
                Pair(ViewNodeFields.Position,   "005"),
                Pair("Groovy Console",          "006"),
                Pair(ViewNodeFields.Owner,      "007"),
                Pair("Inheritance",             "008"),
                Pair("Context:",                "009"),
                Pair("StringValue",             "010")
                // @formatter:on
            )
            override fun compare(a: String, b: String): Int {
                val a = (orderMap[a] ?: "999") + a
                val b = (orderMap[b] ?: "999") + b
                return a.compareTo(b)
            }
        }
    }
}



