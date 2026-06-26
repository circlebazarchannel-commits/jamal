package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class SunriseSunsetWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val widgetData = WidgetUtils.getWidgetData(context)
            val views = RemoteViews(context.packageName, R.layout.widget_sunrise_sunset)

            // Dynamic Data
            views.setTextViewText(R.id.widget_location_name, widgetData.locationName)
            views.setTextViewText(R.id.widget_hijri_date, widgetData.hijriDate)
            views.setTextViewText(R.id.widget_sunrise_time, widgetData.sunriseTimeOnly)
            views.setTextViewText(R.id.widget_sunset_time, widgetData.sunsetTimeOnly)
            views.setTextViewText(R.id.widget_gregorian_date, widgetData.gregorianDate)

            // Labels based on language
            if (widgetData.isEnglish) {
                views.setTextViewText(R.id.widget_sunrise_label, "Sunrise")
                views.setTextViewText(R.id.widget_sunset_label, "Sunset")
            } else {
                views.setTextViewText(R.id.widget_sunrise_label, "সূর্যোদয় (Sunrise)")
                views.setTextViewText(R.id.widget_sunset_label, "সূর্যাস্ত (Sunset)")
            }

            // Click pending intent to open MainActivity
            val configIntent = Intent(context, MainActivity::class.java)
            val configPendingIntent = PendingIntent.getActivity(
                context, 102, configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, configPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
