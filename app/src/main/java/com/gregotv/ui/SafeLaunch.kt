package com.gregotv.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val TAG = "GregoTV"

/**
 * A fire-and-forget coroutine that can never take the process down with it.
 *
 * Most of these launches write to Room or DataStore — saving progress, toggling
 * a favorite, marking a channel dead. A `viewModelScope.launch` with no handler
 * lets an exception (a disk error, a SQLite constraint) propagate to the
 * thread's default uncaught handler, which kills the app. For a background save
 * that is never the right outcome: log it and carry on.
 *
 * The CrashReporter still records anything that slips past this, so a genuine
 * bug is not hidden — it just no longer closes the app mid-playback.
 */
fun ViewModel.safeLaunch(
    what: String,
    block: suspend () -> Unit
): Job {
    val handler = CoroutineExceptionHandler { _, error ->
        Log.e(TAG, "safeLaunch failed: $what", error)
    }
    return viewModelScope.launch(handler) { block() }
}
