package com.orgzly.android.ui.tasks

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pasting a list somebody wrote for humans. Every case here was seen in a real paste: the
 * markers are noise the user would otherwise have to delete by hand, one line at a time.
 */
class ClipboardTasksTest {

    @Test
    fun `plain lines become tasks`() {
        assertEquals(
            listOf("Call the bank", "Book flights"),
            ClipboardTasks.parseTaskLines("Call the bank\nBook flights"),
        )
    }

    @Test
    fun `blank lines and surrounding space are dropped`() {
        assertEquals(
            listOf("Call the bank", "Book flights"),
            ClipboardTasks.parseTaskLines("\n  Call the bank  \n\n\tBook flights\n\n"),
        )
    }

    @Test
    fun `windows line endings do not leave a carriage return behind`() {
        assertEquals(
            listOf("Call the bank", "Book flights"),
            ClipboardTasks.parseTaskLines("Call the bank\r\nBook flights\r\n"),
        )
    }

    @Test
    fun `bullets are stripped`() {
        assertEquals(
            listOf("Dash", "Star", "Plus", "Bullet"),
            ClipboardTasks.parseTaskLines("- Dash\n* Star\n+ Plus\n• Bullet"),
        )
    }

    @Test
    fun `numbering is stripped`() {
        assertEquals(
            listOf("First", "Second"),
            ClipboardTasks.parseTaskLines("1. First\n2) Second"),
        )
    }

    @Test
    fun `markdown checkboxes are stripped, checked or not`() {
        assertEquals(
            listOf("Todo", "Done"),
            ClipboardTasks.parseTaskLines("- [ ] Todo\n- [x] Done"),
        )
    }

    @Test
    fun `a bullet is only stripped from the front`() {
        assertEquals(
            listOf("Buy 2 - 3 apples"),
            ClipboardTasks.parseTaskLines("- Buy 2 - 3 apples"),
        )
    }

    @Test
    fun `a single line with no newline is one task`() {
        assertEquals(listOf("Call the bank"), ClipboardTasks.parseTaskLines("Call the bank"))
    }

    @Test
    fun `empty clipboard yields nothing`() {
        assertEquals(emptyList<String>(), ClipboardTasks.parseTaskLines(""))
        assertEquals(emptyList<String>(), ClipboardTasks.parseTaskLines("   \n\n  "))
    }

    /** A hyphen with no space after it is a word, not a bullet. */
    @Test
    fun `a bare dash is not a bullet`() {
        assertEquals(listOf("-hyphenated"), ClipboardTasks.parseTaskLines("-hyphenated"))
    }

    /**
     * Found on a device: copying out of an editor that continues lists for you brings back a
     * trailing bullet with nothing after it, which used to become a task called "-".
     */
    @Test
    fun `a line that is only a bullet is dropped`() {
        assertEquals(
            listOf("Call the bank"),
            ClipboardTasks.parseTaskLines("- Call the bank\n-\n- \n*\n1."),
        )
    }

    /** Same source: the editor's bullet lands in front of whatever was already there. */
    @Test
    fun `stacked markers are all stripped`() {
        assertEquals(
            listOf("Book flights", "Renew passport", "Pay rent", "Todo"),
            ClipboardTasks.parseTaskLines(
                "- - Book flights\n- * Renew passport\n- 1. Pay rent\n- [ ] Todo"
            ),
        )
    }

    @Test
    fun `text that is nothing but markers yields nothing`() {
        assertEquals(emptyList<String>(), ClipboardTasks.parseTaskLines("- * +\n1.\n[ ]"))
    }
}
