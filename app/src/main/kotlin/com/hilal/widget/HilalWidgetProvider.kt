package com.hilal.widget

import android.appwidget.AppWidgetProvider
import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import java.io.File

class HilalWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val views: RemoteViews = RemoteViews(
                context.packageName,
                R.layout.widget
            ).apply {
                setTextViewText(R.id.hijri_text, getHijriDateText(context))
            }

            manager.updateAppWidget(appWidgetId, views)
        }
    }

    fun getHijriDateText(context: Context): String {
        val dateJson = File(context.filesDir, "dates.json")

        return dateJson.exists().toString()
    }
}
