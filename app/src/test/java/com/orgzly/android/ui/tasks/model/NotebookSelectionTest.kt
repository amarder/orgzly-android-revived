package com.orgzly.android.ui.tasks.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotebookSelectionTest {

    private data class Book(val id: Long, val name: String)

    private val books = listOf(
        Book(1, "Work"),
        Book(2, "Inbox"),
        Book(3, "Home"),
    )

    private fun resolve(remembered: Long?, captureName: String? = "Inbox") =
        NotebookSelection.resolve(books, remembered, captureName, Book::id, Book::name)

    @Test
    fun `a remembered notebook wins`() {
        assertEquals(Book(3, "Home"), resolve(remembered = 3))
    }

    @Test
    fun `falls back to the capture notebook when nothing is remembered`() {
        assertEquals(Book(2, "Inbox"), resolve(remembered = null))
    }

    @Test
    fun `falls back to the capture notebook when the remembered one is gone`() {
        // The user deleted notebook 99 since last time.
        assertEquals(Book(2, "Inbox"), resolve(remembered = 99))
    }

    @Test
    fun `falls back to the first notebook when the capture notebook does not exist`() {
        assertEquals(Book(1, "Work"), resolve(remembered = null, captureName = "Nonexistent"))
    }

    @Test
    fun `falls back to the first notebook when there is no capture name at all`() {
        assertEquals(Book(1, "Work"), resolve(remembered = null, captureName = null))
    }

    @Test
    fun `returns null when there are no notebooks`() {
        assertNull(
            NotebookSelection.resolve(
                emptyList<Book>(), remembered = 1, captureName = "Inbox",
                idOf = Book::id, nameOf = Book::name,
            )
        )
    }
}
