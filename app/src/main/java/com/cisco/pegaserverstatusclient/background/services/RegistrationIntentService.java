package com.cisco.pegaserverstatusclient.background.services;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.cisco.pegaserverstatusclient.R;
import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

/**
 * Created by jonathanhavstad on 10/24/16.
 */

public class RegistrationIntentService extends IntentService {
    private static final String TAG = "RegistrationService";

    public RegistrationIntentService() {
        super(RegistrationIntentService.class.getCanonicalName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        InstanceID instanceID = InstanceID.getInstance(RegistrationIntentService.this);
        String token = null;
        try {
            token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE);
            // TODO: Send message to App Server with updated Registration Token
            String topic = "/topics/myTopic";
            GcmPubSub.getInstance(this).subscribe(token, topic, null);

            Log.i(TAG, "Received token: " + token);
        } catch (IOException e) {
            Log.e(TAG, "Error retrieving InstanceID token");
        }
    }
}
