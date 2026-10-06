package com.rowanrisk.ledger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Redraws every widget when the date, time or time zone changes, so "today" rolls over on time. */
class DateChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                LedgerWidget().updateAll(context)
            } finally {
                pending.finish()
            }
        }
    }
}
