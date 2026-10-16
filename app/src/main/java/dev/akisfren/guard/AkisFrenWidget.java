package dev.akisfren.guard;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** Simple privacy-first home widget. Updated periodically and on tracked-usage changes. */
public class AkisFrenWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) update(context, manager, id);
    }
    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, AkisFrenWidget.class));
        for (int id : ids) update(context, manager, id);
    }
    private static void update(Context context, AppWidgetManager manager, int id) {
        AppSettings settings = new AppSettings(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_akisfren);
        int seconds = settings.todayTotalSeconds();
        views.setTextViewText(R.id.widget_usage,
                seconds < 60 ? seconds + " sn" : seconds / 60 + " dk");
        views.setTextViewText(R.id.widget_mode,
                settings.isPaused() ? "Koruma duraklatıldı" : "Mod: " + settings.activeMode());
        views.setTextViewText(R.id.widget_budget,
                settings.budgetEnabled() ? settings.budgetMinutes() + " dk günlük bütçe" : "Günlük bütçe kapalı");
        Intent launch = new Intent(context, MainActivity.class);
        PendingIntent click = PendingIntent.getActivity(context, 0, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, click);
        manager.updateAppWidget(id, views);
    }
}
