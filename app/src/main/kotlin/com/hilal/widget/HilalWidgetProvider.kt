package com.hilal.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetProvider
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import android.widget.TextView
import com.hilal.widget.R
import kotlin.concurrent.thread
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.LocalDate
import java.time.Period
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HilalWidgetProvider : AppWidgetProvider() {

    lateinit var calendar: Calendar
    lateinit var prefs: SharedPreferences

    override fun onReceive( 
        context: Context, 
        intent: Intent 
    ) { 
        val action = intent.getAction() 
        if (action == "com.hilal.widget.action.UPDATE") {
            val widgetIds = AppWidgetManager
                    .getInstance(context)
                    .getAppWidgetIds(
                        ComponentName(context, HilalWidgetProvider::class.java)
                    )
            onUpdate(context, AppWidgetManager.getInstance(context), widgetIds)
        } else {
            super.onReceive(context, intent)
        }
    }

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val selectedColor = prefs.getInt("color", 0xFFFAFAFA.toInt())
        calendar = Calendar.getInstance()
        appWidgetIds.forEach { appWidgetId ->
            val views: RemoteViews = RemoteViews(
                context.packageName,
                R.layout.widget
            ).apply {
                setTextViewText(R.id.hijri_text, getHijriDate(context))
                setTextColor(R.id.hijri_text, selectedColor)
                setTextColor(R.id.ampm_text, selectedColor)

                val showGroup = prefs.getBoolean("show_group", false)
                if (showGroup) {
                    setViewVisibility(R.id.sighting_group, View.VISIBLE)
                    val json = getDateJson(context)
                    val groups = json.getJSONArray("groups")
                    val group = groups.getString(prefs.getInt("groups", 0))
                    setTextViewText(R.id.sighting_group, group)
                    setTextColor(R.id.sighting_group, selectedColor)
                } else {
                    setViewVisibility(R.id.sighting_group, View.GONE)
                }

                val today = LocalDate.now().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault())
                val tomorrow = LocalDate.now().minusDays(-1).getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault())
                val day = if (isPastSix()) {
                    "$tomorrow/$today"
                } else {
                    today
                }
                setTextViewText(R.id.week_text, day)
                setTextColor(R.id.week_text, selectedColor)
                setTextColor(R.id.date_text, selectedColor)
                setTextColor(R.id.clock_time, selectedColor)

                val intent = Intent(context, HilalConfigActivity::class.java)
                intent.action = "android.appwidget.action.APPWIDGET_CONFIGURE"
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    0
                )
                setOnClickPendingIntent(R.id.digital_clock, pendingIntent)
                setOnClickFillInIntent(R.id.digital_clock, intent)
            }

            manager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onDeleted(
        context: Context,
        appWidgetIds: IntArray
    ) {
    }

    override fun onEnabled(context: Context) {
    }

    fun getMonth(context: Context, num: String): String {
        return context.resources.getStringArray(R.array.months)[num.toInt() - 1]
    }

    fun isPastSix(): Boolean {
        val hours = calendar.get(Calendar.HOUR_OF_DAY)
        return hours >= 18
    }

    companion object {
        fun getDateJson(context: Context): JSONObject {
            val dateJson = File(context.filesDir, "dates.json")

            if (!dateJson.exists()) {
                thread {
                    val client = OkHttpClient.Builder().build()
                    val request = Request.Builder()
                    .url("https://raw.githubusercontent.com/AbdullahM0hamed/HilalMonths/master/hilal-months.json")
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
            return JSONObject(text)
        }

        fun getGroups(json: JSONObject): JSONArray {
            return json.getJSONArray("groups")
        }
    }

    fun getHijriDate(context: Context): String {
        val json = getDateJson(context)
        val groups = json.getJSONArray("groups")
        val group = groups.getString(prefs.getInt("groups", 0))
        val dates = json.getJSONObject(group)
        val years = dates.keys().asSequence().toList()
        val months = dates.getJSONObject(years[0])
        val monthKeys = months.keys().asSequence().toList().reversed()
        var latest = monthKeys[0]
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
        var day = if (sub == 1 && days == -1 && doubtDay.compareTo(current) < 0) {
            "30?/1?"
        } else {
            "${29 - days}"
        }

        if (day == "0") {
            day = "30"
            latest = monthKeys[1]
        }
        return "$day ${getMonth(context, latest)}"
    }
}
