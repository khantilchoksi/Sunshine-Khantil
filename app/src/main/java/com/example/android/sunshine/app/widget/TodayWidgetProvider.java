package com.example.android.sunshine.app.widget;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import com.example.android.sunshine.app.MainActivity;
import com.example.android.sunshine.app.R;
import com.example.android.sunshine.app.Utility;
import com.google.android.gms.iid.InstanceID;

/**
 * Created by Khantil on 01-08-2016.
 */
public class TodayWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        int weatherArtResourceId = R.drawable.art_clear;
        String description = "Clear";
        double maxTemp = 24;
        String formattedMaxTemperature = Utility.formatTemperature(context, maxTemp);

        // Perform this loop procedure for each Today widget
        for(int appWidgetId : appWidgetIds){
            int layoutId = R.layout.widget_today_small;

            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), layoutId);
            //Add the data to remote View
            remoteViews.setImageViewResource(R.id.widget_icon, weatherArtResourceId);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
                setRemoteContentDescription(remoteViews, description);
            }
            remoteViews.setTextViewText(R.id.widget_high_tempreture, formattedMaxTemperature);

            //create a pending to intent to launch main activity
            Intent launchIntent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context,0,launchIntent,0);
            remoteViews.setOnClickPendingIntent(R.id.widget, pendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void setRemoteContentDescription(RemoteViews remoteViews, String description) {
        remoteViews.setContentDescription(R.id.widget_icon, description);
    }
}
