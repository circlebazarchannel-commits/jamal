package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class PrayerTimesWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val widgetData = WidgetUtils.getWidgetData(context)
            val views = RemoteViews(context.packageName, R.layout.widget_prayer_times)

            // Header info
            views.setTextViewText(R.id.widget_gregorian_date, widgetData.gregorianDate)
            views.setTextViewText(R.id.widget_hijri_date, widgetData.hijriDate)
            views.setTextViewText(R.id.widget_location_name, widgetData.locationName)

            // Times
            views.setTextViewText(R.id.time_fajr, widgetData.fajr)
            views.setTextViewText(R.id.time_dhuhr, widgetData.dhuhr)
            views.setTextViewText(R.id.time_asr, widgetData.asr)
            views.setTextViewText(R.id.time_maghrib, widgetData.maghrib)
            views.setTextViewText(R.id.time_isha, widgetData.isha)

            // Row labels based on language
            if (widgetData.isEnglish) {
                views.setTextViewText(R.id.label_fajr, "Fajr")
                views.setTextViewText(R.id.label_dhuhr, "Dhuhr")
                views.setTextViewText(R.id.label_asr, "Asr")
                views.setTextViewText(R.id.label_maghrib, "Maghrib")
                views.setTextViewText(R.id.label_isha, "Isha")
            } else {
                views.setTextViewText(R.id.label_fajr, "ফজর (Fajr)")
                views.setTextViewText(R.id.label_dhuhr, "যোহর (Dhuhr)")
                views.setTextViewText(R.id.label_asr, "আসর (Asr)")
                views.setTextViewText(R.id.label_maghrib, "মাগরিব (Maghrib)")
                views.setTextViewText(R.id.label_isha, "এশা (Isha)")
            }

            // Click pending intent to open MainActivity
            val configIntent = Intent(context, MainActivity::class.java)
            val configPendingIntent = PendingIntent.getActivity(
                context, 100, configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, configPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
