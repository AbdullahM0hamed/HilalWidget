package com.hilal.widget

import android.appwidget.AppWidgetProvider
import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import kotlin.concurrent.thread
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
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

        if (!dateJson.exists()) {
            thread {
                val client = OkHttpClient.Builder().build()
                val request = Request.Builder()
                    .url("http://localhost:8000/hilal-months.json")
                    .build()
                val response = client.newCall(request).execute()
                dateJson.writeText(response!!.body!!.string())
            }

            //Yh yh, find out how to update widget stuff in thread
            while (!dateJson.exists()) {
                runBlocking { delay(1000L) }
            }
        }

        val text = dateJson.readText()
        val json = JSONObject(text)
        val groups = json.getJSONArray("groups")
        val group = groups.getString(0)
        return group
    }
}
