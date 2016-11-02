package com.cisco.pegaserverstatusclient.activities;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.idling.CountingIdlingResource;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.cisco.pegaserverstatusclient.R;
import com.cisco.pegaserverstatusclient.background.services.PegaRegistrationIntentService;
import com.cisco.pegaserverstatusclient.background.services.PegaServerRefreshService;
import com.cisco.pegaserverstatusclient.background.tasks.PegaServerRestTask;
import com.cisco.pegaserverstatusclient.binders.BaseLayoutInfoBinder;
import com.cisco.pegaserverstatusclient.binders.PegaServerNetworkBinder;
import com.cisco.pegaserverstatusclient.binders.SubscriberBinder;
import com.cisco.pegaserverstatusclient.data.AppLayoutInfo;
import com.cisco.pegaserverstatusclient.data.BaseLayoutInfo;
import com.cisco.pegaserverstatusclient.data.DomainLayoutInfo;
import com.cisco.pegaserverstatusclient.data.LifecycleLayoutInfo;
import com.cisco.pegaserverstatusclient.data.ServerLayoutInfo;
import com.cisco.pegaserverstatusclient.parcelables.BaseInfoParcelable;
import com.cisco.pegaserverstatusclient.parcelables.PegaServerNetworkParcelable;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.functions.Action1;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final int PLAY_SERVICE_RESOLUTION_REQUEST = 10000;

    public  static final int PEGA_DISPLAY_DATA_REQUEST = 2000;
    public static final int RESULT_RELOAD_DATA = 2000;

    @BindView(R.id.username_view)
    EditText usernameView;

    @BindView(R.id.password_view)
    EditText passwordView;

    @BindView(R.id.get_view_url)
    EditText getViewUrl;

    @BindView(R.id.get_view_btn)
    Button getViewBtn;

    @BindView(R.id.progress_indicator)
    ProgressBar progressIndicator;

    private Map<String, Object> appData = new HashMap<>();

    private Map<String, Object> drawerData;

    private BaseLayoutInfo baseLayoutInfo;

    private CountingIdlingResource countingIdlingResource =
            new CountingIdlingResource("LoaddAppDataIdlingResource");

    private PegaServerRestTask task;

    private String statusUrl;

    private ServiceConnection connection;

    private Action1<Map<String, Object>> subscriber;

    private SubscriberBinder binder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Fabric.with(this, new Crashlytics());

        ButterKnife.bind(this);

        verifyGooglePlayServices();
        startInstanceIDService();

        parseIntent();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getData(statusUrl);
        startServerRefreshService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopServerRefreshService();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PEGA_DISPLAY_DATA_REQUEST) {
            if (resultCode == RESULT_RELOAD_DATA) {

            } else {
                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private boolean verifyGooglePlayServices() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int playServicesResultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (playServicesResultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(playServicesResultCode)) {
                googleApiAvailability.getErrorDialog(this,
                        playServicesResultCode,
                        PLAY_SERVICE_RESOLUTION_REQUEST)
                .show();
            } else {
                showAlert(getString(R.string.google_play_services_error_alert_title),
                        getString(R.string.google_play_services_error_alert_message));
                finish();
            }
            return false;
        }

        return true;
    }

    private void startInstanceIDService() {
        Intent instanceIDServiceIntent = new Intent(this, PegaRegistrationIntentService.class);
        startService(instanceIDServiceIntent);
    }

    private void parseIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            statusUrl = intent.getStringExtra(getString(R.string.status_url_bundle_key));
            if (statusUrl == null) {
                statusUrl = getString(R.string.default_url);
            }
        }
    }

    private void getData(String restUrl) {
        startLoading();
        Action1<Integer> subscriber = new Action1<Integer>() {
            @Override
            public void call(Integer authSuccessful) {
                evaluateAuthResult(authSuccessful);
            }
        };
        task = new PegaServerRestTask(this, appData);
        if (restUrl != null && !restUrl.isEmpty()) {
            task.loadStatusFromNetwork(restUrl, subscriber);
        }
    }

    private void launchPegaServerDataActivity() {
        Intent intent = new Intent(
                MainActivity.this,
                PegaServerDataActivity.class);
        PegaServerNetworkBinder appBinder = new PegaServerNetworkBinder();
        appBinder.setAppData(appData);
        appBinder.setDrawerData(drawerData);
        appBinder.setKeyPath(new ArrayList<String>());
        PegaServerNetworkParcelable appParcelable = new PegaServerNetworkParcelable();
        appParcelable.setBinder(appBinder);
        intent.putExtra(getString(R.string.app_binder_data_bundle_key), appParcelable);
        BaseLayoutInfoBinder baseLayoutInfoBinder = new BaseLayoutInfoBinder();
        baseLayoutInfoBinder.setBaseLayoutInfo(baseLayoutInfo);
        BaseInfoParcelable baseInfoParcelable = new BaseInfoParcelable();
        baseInfoParcelable.setBaseLayoutInfoBinder(baseLayoutInfoBinder);
        intent.putExtra(getString(R.string.info_binder_data_bundle_key), baseInfoParcelable);
        intent.putExtra(getString(R.string.current_page_bundle_key), 1);
        startActivityForResult(intent, PEGA_DISPLAY_DATA_REQUEST);
    }

    private void startLoading() {
        countingIdlingResource.increment();
        getViewUrl.setEnabled(false);
        getViewBtn.setEnabled(false);
        progressIndicator.setVisibility(View.VISIBLE);
    }

    private void stopLoading() {
        getViewUrl.setEnabled(true);
        getViewBtn.setEnabled(true);
        progressIndicator.setVisibility(View.INVISIBLE);
        if (!countingIdlingResource.isIdleNow()) {
            countingIdlingResource.decrement();
        }
    }

    public Map<String, Object> getAppData() {
        return appData;
    }

    public IdlingResource getIdlingResource() {
        return countingIdlingResource;
    }

    private void showAlert(String title, String body) {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(body)
                .setNeutralButton(R.string.alert_dialog_confirm_btn_txt, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                })
                .create();
        alertDialog.show();
    }

    private void evaluateAuthResult(int authResult) {
        stopLoading();

        if (authResult == PegaServerRestTask.AUTH_FAILURE) {
            showAlert(getString(R.string.authentication_error_alert_title),
                    getString(R.string.authentication_error_alert_body));
        } else if (authResult == PegaServerRestTask.AUTH_SUCCESS) {
            Log.i(TAG, getString(R.string.rest_auth_success_log_message));
        } else if (authResult == PegaServerRestTask.DATA_LOAD_SUCCESS) {
            if (verifyData(appData)) {
                launchPegaServerDataActivity();
            } else {
                showAlert(getString(R.string.data_load_failure_alert_title),
                        getString(R.string.data_load_failure_alert_body) + statusUrl);
            }
        } else if (authResult == PegaServerRestTask.DATA_LOAD_FAILURE) {
            showAlert(getString(R.string.data_load_failure_alert_title),
                    getString(R.string.data_load_failure_alert_body) + statusUrl);
        } else if (authResult == PegaServerRestTask.ACCESS_TOKEN_FAILURE) {
            Log.e(TAG, getString(R.string.rest_access_token_error_log_message));
        }
    }

    private void loadLayoutInfo() {
        baseLayoutInfo = new LifecycleLayoutInfo();
    }

    private boolean verifyData(Object appData) {
        drawerData = this.appData;
        if (appData instanceof Map<?,?>) {
            Map<String, Object> mapAppData = (Map<String, Object>) appData;
            for (String key : mapAppData.keySet()) {
                if (LifecycleLayoutInfo.LC_MAPPING.containsKey(key.toLowerCase())) {
                    baseLayoutInfo = new LifecycleLayoutInfo();
                    return true;
                }
                Object childAppData = mapAppData.get(key);
                if (childAppData instanceof Map<?, ?>) {
                    Map<String, Object> childMapAppData = (Map<String, Object>) childAppData;
                    for (String childKey : childMapAppData.keySet()) {
                        if (childKey.toUpperCase().equals(AppLayoutInfo.APP_JSON_KEY)) {
                            baseLayoutInfo = new DomainLayoutInfo();
                            ((DomainLayoutInfo) baseLayoutInfo)
                                    .setFriendlyName(baseLayoutInfo.getFriendlyName(key, true));
                            baseLayoutInfo.setKey(key);
                            if (mapAppData.size() == 1) {
                                this.appData = childMapAppData;
                            }
                            return true;
                        }
                    }
                    for (String childKey : childMapAppData.keySet()) {
                        if (childKey.toUpperCase().equals(ServerLayoutInfo.SERVER_JSON_KEY)) {
                            baseLayoutInfo = new AppLayoutInfo();
                            baseLayoutInfo.setFriendlyName(baseLayoutInfo.getFriendlyName(key, true));
                            baseLayoutInfo.setKey(key);
                            if (mapAppData.keySet().size() == 1) {
                                this.appData = childMapAppData;
                            }
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void startServerRefreshService() {
        subscriber = new Action1<Map<String, Object>>() {
            @Override
            public void call(Map<String, Object> appData) {
                verifyData(appData);
            }
        };

        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                binder = ((SubscriberBinder) service);
                binder.setAppData(appData);
                binder.addSubscriber(subscriber);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        Intent intent = new Intent(this, PegaServerRefreshService.class);
        intent.putExtra(getString(R.string.status_url_bundle_key), statusUrl);
        bindService(intent, connection, BIND_AUTO_CREATE);
        startService(intent);
    }

    private void stopServerRefreshService() {
        binder.removeSubscriber(subscriber);
        unbindService(connection);
    }
}
