package com.cisco.pegaserverstatusclient.background.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.cisco.pegaserverstatusclient.R;
import com.google.android.gms.gcm.GcmListenerService;

/**
 * Created by jonathanhavstad on 10/21/16.
 */

public class PegaServerListenerService extends GcmListenerService {
    private static final String TAG = "ListenerService";
    private static final int INITIAL_NOTIFICATION_ID = 1000;
    private int notificationId;

    public PegaServerListenerService() {
        notificationId = INITIAL_NOTIFICATION_ID;
    }

    @Override
    public void onMessageReceived(String s, Bundle bundle) {
        Log.d(TAG, "Received message: " + s);
        Bundle notificationBundle = bundle.getBundle("notification");
        if (notificationBundle != null) {
            String title = notificationBundle.getString("title");
            String body = notificationBundle.getString("body");
            String icon = notificationBundle.getString("icon");
            String url = bundle.getString("url");
            sendNotification(title, body, url);
        }
    }

    private void sendNotification(String title, String body, String url) {
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_add_alert_black_24dp)
                        .setContentTitle(title)
                        .setContentText(body);
        Intent activityIntent = new Intent();
        activityIntent.setAction("android.app.action.MAIN");
        activityIntent.addCategory("android.intent.category.LAUNCHER");
        activityIntent.putExtra(getString(R.string.status_url_bundle_key), url);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(pendingIntent);
        notificationBuilder.setAutoCancel(true);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification pushNotification = notificationBuilder.build();
        notificationManager.notify(notificationId, pushNotification);
        notificationId++;
    }
}
