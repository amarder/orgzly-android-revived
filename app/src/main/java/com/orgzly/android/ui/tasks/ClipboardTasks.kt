package com.orgzly.android.ui.tasks

import android.content.Context
import com.orgzly.android.ui.util.getClipboardManager

/**
 * Turning a block of pasted text into one task per line.
 *
 * Split from the clipboard read so the parsing - the only part with rules worth arguing
 * about - can be tested as a plain function.
 */
object ClipboardTasks {

    /**
     * A leading list marker: "-", "*", "+" and friends, "1." or "1)" numbering, or a markdown
     * checkbox. Matching end-of-line as well as whitespace is what lets an empty list item -
     * the bullet an editor leaves behind on the last line - be recognised and dropped.
     */
    private val MARKER = Regex("""^\s*(?:[-*+\u2022\u00b7]|\d+[.)]|\[[ xX]])(?:\s+|$)""")

    /**
     * One task per line, with the noise a human-written list carries stripped off.
     *
     * Markers are removed repeatedly rather than once. Pasted lists really do arrive with
     * more than one - "- [ ] thing" is two, and an editor continuing a list while the text
     * was being written produces "- 1. thing" - and a task called "- Book flights" is not
     * something anybody wanted. A line that is nothing but markers is dropped entirely.
     */
    fun parseTaskLines(text: String): List<String> =
        text.split('\n')
            .map { stripMarkers(it.trim()) }   // trim also drops the \r of a \r\n line ending
            .filter { it.isNotEmpty() }

    private fun stripMarkers(line: String): String {
        var stripped = line

        while (true) {
            val next = stripped.replaceFirst(MARKER, "").trim()
            if (next == stripped) return stripped
            stripped = next
        }
    }

    /**
     * Whatever is on the system clipboard, or "" when it is empty or holds no text.
     *
     * Mirrors the app's one other clipboard read, TemplateExpander.clipboardText.
     */
    fun clipboardText(context: Context): String =
        context.getClipboardManager()
            .primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            .orEmpty()
}
