package com.hilal.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetProvider
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import kotlin.concurrent.thread
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.time.Period
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HilalWidgetProvider : AppWidgetProvider() {

    lateinit var calendar: Calendar

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        calendar = Calendar.getInstance()
        appWidgetIds.forEach { appWidgetId ->
            val views: RemoteViews = RemoteViews(
                context.packageName,
                R.layout.widget
            ).apply {
                setTextViewText(R.id.hijri_text, getHijriDate())

                val day = if (isPastSix()) {
                    "${
                        LocaleDate.now().minusDays(-1).getDayOfWeek().name()
                    }/${
                        LocaleDate.now().getDayOfWeek().name()
                    }"
                } else {
                    LocaleDate.now().getDayOfWeek().name()
                }
            }

            manager.updateAppWidget(appWidgetId, views)
        }
    }

    fun getDay(num: Int) = when (num) {
    }

    fun getMonth(num: String): String {
        val months = listOf(
            "Muharram",
            "Safar",
            "Rabi' Al-Awwal",
            "Rabi' Ath-Thaani",
            "Jumada Al-Ulaa",
            "Jumada Ath-Thaani",
            "Rajab",
            "Sha'baan",
            "Ramadaan",
            "Shawwaal",
            "Dhul Qa'dah",
            "Dhul Hijjah"
        )

        return months[num.toInt() - 1]
    }

    fun isPastSix(): Boolean {
        val hours = calendar.get(Calendar.HOUR_OF_DAY)
        return hours >= 18
    }

    fun getHijriDate(context: Context): String {
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
        val dates = json.getJSONObject(group)
        val years = dates.keys().asSequence().toList()
        val months = dates.getJSONObject(years[0])
        val monthKeys = months.keys().asSequence().toList().reversed()
        val latest = monthKeys[0]
        val date = months.getJSONObject(latest).getString("29")
        val parts = date.split("/")
        val current = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("dd/M/yyyy")
        val doubtDay = LocalDate.parse(date, formatter)
        val sub = if (isPastSix()) {
            1
        } else {
            0
        }

        val days = Period.between(current, doubtDay).days - sub

        return "${29 - days} ${getMonth(latest)}"
    }
}
