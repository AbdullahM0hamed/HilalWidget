package com.hilal.widget

import android.appwidget.AppWidgetProvider
import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import java.io.File

class HilalWidgetProvider : AppWidgetProvider {

    public fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val views: RemoteViews = RemoteViews(
                context.packageName,
                R.layout.widget
            ).apply {
                setTextViewText(R.id.hijri_text, getHijriDateText())
            }
        }
    }

    fun getHijriDateText(): String {
        val dateJson = File(this.filesDir, "dates.json")
        android.widget.Toast.makeText(this, dateJson.exists().toString(), 5)
        return "1/9/1444"
    }
}
