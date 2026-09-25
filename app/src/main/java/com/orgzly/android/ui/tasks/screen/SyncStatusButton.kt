package com.orgzly.android.ui.tasks.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.orgzly.R
import com.orgzly.android.ui.tasks.SyncStatus

/**
 * Sync state and the sync action in one control, replacing the overflow menu that used to
 * hold a lone "Sync now".
 *
 * Tapping always syncs, even when everything is already in sync: that is the only way to
 * pull remote changes, and a button that did nothing would be worse than one that repeats
 * itself. While a sync is running the same tap cancels it, mirroring the drawer's button.
 */
@Composable
fun SyncStatusButton(status: SyncStatus, onClick: () -> Unit) {
    val iconRes = when (status) {
        SyncStatus.SYNCED -> R.drawable.ic_cloud_outline
        SyncStatus.PENDING -> R.drawable.ic_cloud_upload_outline
        SyncStatus.SYNCING -> R.drawable.ic_sync
        SyncStatus.FAILED -> R.drawable.ic_sync_problem
    }

    val descriptionRes = when (status) {
        SyncStatus.SYNCED -> R.string.tasks_sync_synced
        SyncStatus.PENDING -> R.string.tasks_sync_pending
        SyncStatus.SYNCING -> R.string.tasks_sync_syncing
        SyncStatus.FAILED -> R.string.tasks_sync_failed
    }

    // The drawables carry android:tint="?colorControlNormal", which Compose does not resolve,
    // so the tint has to be passed explicitly or the icon comes out the wrong colour.
    val tint = if (status == SyncStatus.FAILED) {
        MaterialTheme.colorScheme.error
    } else {
        LocalContentColor.current
    }

    IconButton(onClick = onClick) {
        Icon(
            painterResource(iconRes),
            contentDescription = stringResource(descriptionRes),
            tint = tint,
            modifier = if (status == SyncStatus.SYNCING) Modifier.spinning() else Modifier,
        )
    }
}

@Composable
private fun Modifier.spinning(): Modifier {
    val transition = rememberInfiniteTransition(label = "sync")

    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sync-rotation",
    )

    return rotate(angle)
}
