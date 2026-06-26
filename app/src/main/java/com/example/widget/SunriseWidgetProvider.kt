package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class SunriseWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val widgetData = WidgetUtils.getWidgetData(context)
            val views = RemoteViews(context.packageName, R.layout.widget_sunrise)

            // Dynamic Data
            views.setTextViewText(R.id.widget_location_name, widgetData.locationName)
            views.setTextViewText(R.id.widget_hijri_date, widgetData.hijriDate.replace(" AH", "").replace(" হিজরি", ""))
            views.setTextViewText(R.id.widget_sunrise_time, widgetData.sunriseTimeOnly)
            views.setTextViewText(R.id.widget_gregorian_date, widgetData.gregorianDate)

            // Labels based on language
            if (widgetData.isEnglish) {
                views.setTextViewText(R.id.widget_sunrise_label, "Sunrise")
            } else {
                views.setTextViewText(R.id.widget_sunrise_label, "সূর্যোদয় (Sunrise)")
            }

            // Click pending intent to open MainActivity
            val configIntent = Intent(context, MainActivity::class.java)
            val configPendingIntent = PendingIntent.getActivity(
                context, 103, configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, configPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
