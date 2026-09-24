package com.orgzly.android.ui.tasks.model

/**
 * Picks which notebook quick-add should target.
 *
 * Pure so the fallback chain can be tested: a remembered notebook can be deleted or renamed
 * out from under us between sessions, and silently targeting the wrong notebook is worse than
 * most bugs here because it scatters captures across files.
 */
object NotebookSelection {

    /**
     * @param remembered id previously chosen by the user, if any
     * @param captureName Orgzly's own capture notebook name (AppPreferences.shareNotebook)
     */
    fun <T> resolve(
        notebooks: List<T>,
        remembered: Long?,
        captureName: String?,
        idOf: (T) -> Long,
        nameOf: (T) -> String,
    ): T? =
        notebooks.firstOrNull { idOf(it) == remembered }
            ?: captureName?.let { name -> notebooks.firstOrNull { nameOf(it) == name } }
            ?: notebooks.firstOrNull()
}
