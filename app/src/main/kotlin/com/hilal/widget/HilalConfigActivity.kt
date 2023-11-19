package com.hilal.widget

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.appwidget.AppWidgetManager
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.Spinner
import android.widget.TextView
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import com.hilal.widget.R

class HilalConfigActivity : AppCompatActivity(), ColorPickerDialogListener {
    lateinit var colorView: ImageView
    var selectedColor: Int = 0xFFFAFAFA.toInt()
    var selectedGroup: Int = 0

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        selectedColor = prefs.getInt("color", 0xFFFAFAFA.toInt())
        selectedGroup = prefs.getInt("groups", 0)
        val metrics = DisplayMetrics()
        getWindowManager().getDefaultDisplay().getMetrics(metrics)
        val newWidth = (metrics.widthPixels * 0.5).toInt()
        val newHeight = (metrics.heightPixels * 0.7).toInt()
        val text = TextView(this)
        //text.text = "Test"
        //text.setLayoutParams(LayoutParams(newWidth, newHeight))
        getWindow().setLayout(newWidth, newHeight)
        setContentView(text)
        //text.setOnClickListener {
        //}

        val view = LayoutInflater.from(this).inflate(R.layout.list, null, false)
        val spinner: Spinner = view.findViewById(R.id.group)
        val groups = HilalWidgetProvider.getGroups(HilalWidgetProvider.getDateJson(this))
        val groupArray = Array(groups.length()) {
            groups.getString(it)
        }
        val dialog = AlertDialog.Builder(ContextThemeWrapper(this, R.style.CustomDialog))
            .setPositiveButton("Positive") { _, _ ->
                android.widget.Toast.makeText(this, groupArray.indexOf(spinner.getSelectedItem()).toString(), 5).show()
                val prefs = this.getSharedPreferences("settings", Context.MODE_PRIVATE)
                val editor = prefs.edit()
                editor.putInt("groups", groupArray.indexOf(spinner.getSelectedItem()))
                editor.putInt("color", selectedColor)
                editor.apply()
            
                val appWidgetId = intent?.extras?.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
                val views = RemoteViews(this.packageName, R.layout.widget)
                AppWidgetManager.getInstance(this).updateAppWidget(appWidgetId, views)
                val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                setResult(Activity.RESULT_OK, resultValue)
                val intent = Intent()
                intent.setPackage("com.hilal.widget")
                intent.setAction("com.hilal.widget.action.UPDATE")
                sendBroadcast(intent)
                finish()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setOnDismissListener { finish() }
            .create()

        colorView = view.findViewById(R.id.colour)
        colorView.background.setColorFilter(selectedColor, PorterDuff.Mode.SRC_IN)
        colorView.setOnClickListener {
            ColorPickerDialog.newBuilder().setColor(0xFFFAFAFA.toInt()).show(this)
        }
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, groupArray)
        spinner.setSelection(selectedGroup)
        dialog.setView(view)
        dialog.getWindow()?.setBackgroundDrawableResource(R.drawable.rounded_bg)
        dialog.show()
    }

    override fun onStart() {
        super.onStart()
        window.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    override fun onColorSelected(dialog: Int, color: Int) {
        colorView.background.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        selectedColor = color
    }

    override fun onDialogDismissed(dialog: Int) {
    }
}
