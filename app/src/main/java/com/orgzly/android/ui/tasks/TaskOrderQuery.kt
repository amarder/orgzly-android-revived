package com.orgzly.android.ui.tasks

import com.orgzly.android.db.OrgzlyDatabase
import com.orgzly.android.ui.tasks.model.TaskOrderValues

/**
 * Reads every task's hand-picked order in one go.
 *
 * A raw query rather than DataRepository.getNoteProperties, which answers for a single note:
 * the list needs the value for every row, and one indexed query beats one query per task.
 * note_properties is indexed on name, so this stays cheap however many notes exist.
 *
 * Blocking, like every other database call in this package; callers stay off the main thread.
 */
fun readTaskOrders(database: OrgzlyDatabase): Map<Long, Long> {
    val orders = mutableMapOf<Long, Long>()

    database.query(
        "SELECT note_id, value FROM note_properties WHERE name = ? COLLATE NOCASE",
        arrayOf<Any>(TaskOrderValues.PROPERTY),
    ).use { cursor ->
        while (cursor.moveToNext()) {
            // A property hand-edited into something that is not a number is ignored rather
            // than guessed at: the task simply sorts as though it had never been dragged.
            TaskOrderValues.parse(cursor.getString(1))?.let { orders[cursor.getLong(0)] = it }
        }
    }

    return orders
}
