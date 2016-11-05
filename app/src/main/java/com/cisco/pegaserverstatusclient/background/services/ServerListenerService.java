package com.cisco.pegaserverstatusclient.background.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.cisco.pegaserverstatusclient.R;
import com.cisco.pegaserverstatusclient.activities.LoginActivity;
import com.google.android.gms.gcm.GcmListenerService;

/**
 * Created by jonathanhavstad on 10/21/16.
 */

public class ServerListenerService extends GcmListenerService {
    private static final String TAG = "ListenerService";

    private static final int INITIAL_NOTIFICATION_ID = 1000;
    private int notificationId;

    public ServerListenerService() {
        notificationId = INITIAL_NOTIFICATION_ID;
    }

    @Override
    public void onMessageReceived(String s, Bundle bundle) {
        Log.d(TAG, "Received message: " + s);
        Bundle notificationBundle = bundle.getBundle(getString(R.string.notification_bundle_key));
        if (notificationBundle != null) {
            String title =
                    notificationBundle.getString(getString(R.string.notification_title_bundle_key));
            String body =
                    notificationBundle.getString(getString(R.string.notification_body_bundle_key));
            String icon =
                    notificationBundle.getString(getString(R.string.notification_icon_bundle_key));
            String url = bundle.getString(getString(R.string.notification_push_data_url_bundle_key));
            sendNotification(title, body, url);
        }
    }

    private void sendNotification(String title, String body, String url) {
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_add_alert_black_24dp)
                        .setContentTitle(title)
                        .setContentText(body);
        Intent activityIntent = new Intent(this, LoginActivity.class);
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
