package com.mneme.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.mneme.app.data.MnemeDatabase
import com.mneme.app.MainActivity
import com.mneme.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Privacy-preserving summary of confirmed follow-ups, refreshed on app resume/widget update. */
class RetenqoWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        updateAll(context)
    }

    override fun onEnabled(context: Context) {
        updateAll(context)
    }

    companion object {
        private val refreshScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun updateAll(context: Context) {
            val app = context.applicationContext
            val manager = AppWidgetManager.getInstance(app)
            val component = ComponentName(app, RetenqoWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return

            refreshScope.launch {
                val activeCount = runCatching {
                    MnemeDatabase.get(app).notes().activeWidgetCount(System.currentTimeMillis())
                }.getOrDefault(0)
                withContext(Dispatchers.Main.immediate) {
                    val currentManager = AppWidgetManager.getInstance(app)
                    ids.forEach { id -> update(app, currentManager, id, activeCount) }
                }
            }
        }

        internal fun update(context: Context, manager: AppWidgetManager, id: Int, activeCount: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_retenqo)
            val summary = if (activeCount == 0) {
                context.getString(R.string.widget_no_followups)
            } else {
                context.resources.getQuantityString(R.plurals.widget_followups_count, activeCount, activeCount)
            }
            views.setTextViewText(R.id.widget_summary_1, summary)
            views.setTextViewText(R.id.widget_summary_2, context.getString(R.string.widget_open_tasks))
            views.setTextViewText(R.id.widget_summary_3, context.getString(R.string.widget_privacy_hint))
            val openBrief = PendingIntent.getActivity(
                    context,
                    id,
                    Intent(context, MainActivity::class.java).addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
                    ),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            views.setOnClickPendingIntent(R.id.widget_root, openBrief)
            manager.updateAppWidget(id, views)
        }
    }
}
