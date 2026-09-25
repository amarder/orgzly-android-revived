package com.orgzly.android.ui.tasks.screen

import androidx.annotation.AttrRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import com.orgzly.android.ui.compose.theme.OrgzlyFontSize

/**
 * Reads a legacy note-list text-size attr (e.g. `item_head_title_text_size`) the same way
 * [com.orgzly.android.ui.compose.theme.adjustForTheme] reads `font_small`/`medium`/`large`.
 *
 * Tasks used `MaterialTheme.typography.bodyMedium` for row titles, but that tracks
 * `font_medium` (18sp default), a full 2sp above the note list's own `item_head_title_text_size`
 * (16sp default) - a different attr family Compose's Typography never reads. Reading the exact
 * attr is what makes a task row match a note row pixel-for-pixel instead of approximating it.
 */
@Composable
fun rememberNoteListTextSize(@AttrRes attr: Int): TextUnit {
    val context = LocalContext.current
    val density = LocalDensity.current
    val fontSize = OrgzlyFontSize.current

    return remember(context, density, fontSize, attr) {
        val style = context.obtainStyledAttributes(fontSize.resource, intArrayOf(attr))
        val px = style.getDimensionPixelSize(0, 0)
        style.recycle()

        with(density) { px.toSp() }
    }
}
