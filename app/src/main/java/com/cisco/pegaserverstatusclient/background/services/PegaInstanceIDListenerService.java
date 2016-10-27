package com.cisco.pegaserverstatusclient.background.services;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * Created by jonathanhavstad on 10/24/16.
 */

public class PegaInstanceIDListenerService extends InstanceIDListenerService {
    private static final String TAG = "JJH";

    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();
        Log.i(TAG, "Token refreshed!");
        Intent instanceIDServiceIntent = new Intent(this, PegaRegistrationIntentService.class);
        startService(instanceIDServiceIntent);
    }
}
