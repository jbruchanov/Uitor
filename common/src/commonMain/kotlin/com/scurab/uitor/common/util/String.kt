@file:Suppress("SpellCheckingInspection", "NAME_SHADOWING")

package com.scurab.uitor.common.util

private val EMPTY = IntArray(0)

fun String.isMatchingIndexes(expr: String, ignoreCase: Boolean = true): Boolean {
    val thiz = if (ignoreCase) toLowerCase() else this
    val expr = if (ignoreCase) expr.toLowerCase() else expr
    return expr
        .let { expr.trim() }
        .takeIf { it.isNotEmpty() }
        ?.let { se ->
            var minIndex = -1
            se.forEachIndexed { _, c ->
                val mi = thiz.indexOf(c, minIndex + 1)
                if (mi > minIndex) {
                    minIndex = mi
                } else {
                    return false
                }
            }
            true
        } ?: false
}

/**
 * Return matching indexes.
 * Example:
 * 'HelloWorld'
 * - param 'eoo' will return [1, 4, 6]
 * - param 'HWq' will return [] as the 'q' is not found
 *
 */
fun String.matchingIndexes(expr: String, ignoreCase: Boolean = true): IntArray {
    val thiz = if (ignoreCase) toLowerCase() else this
    val expr = if (ignoreCase) expr.toLowerCase() else expr
    return expr
        .let { expr.trim() }
        .takeIf { it.isNotEmpty() }
        ?.let { se ->
            val result = IntArray(se.length)
            var minIndex = -1
            se.forEachIndexed { index, c ->
                val mi = thiz.indexOf(c, minIndex + 1)
                if (mi > minIndex) {
                    result[index] = mi
                    minIndex = mi
                } else {
                    return EMPTY
                }
            }
            result
        } ?: EMPTY
}