package com.cisco.pegaserverstatusclient.activities;

import android.content.DialogInterface;
import android.content.Intent;
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
import com.cisco.pegaserverstatusclient.background.tasks.PegaServerRestTask;
import com.cisco.pegaserverstatusclient.binders.BaseLayoutInfoBinder;
import com.cisco.pegaserverstatusclient.binders.PegaServerNetworkBinder;
import com.cisco.pegaserverstatusclient.data.BaseLayoutInfo;
import com.cisco.pegaserverstatusclient.data.LifecycleLayoutInfo;
import com.cisco.pegaserverstatusclient.parcelables.BaseInfoParcelable;
import com.cisco.pegaserverstatusclient.parcelables.PegaServerNetworkParcelable;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

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
    private static final int PEGA_DISPLAY_DATA_REQUEST = 2000;

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

    private BaseLayoutInfo baseLayoutInfo;

    private CountingIdlingResource countingIdlingResource =
            new CountingIdlingResource("LoaddAppDataIdlingResource");

    private PegaServerRestTask task;

    private String statusUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Fabric.with(this, new Crashlytics());

        ButterKnife.bind(this);

        verifyGooglePlayServices();
        startInstanceIDService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        parseIntent();
        getData(statusUrl);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PEGA_DISPLAY_DATA_REQUEST) {
            finish();
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
        }
    }

    private void getData(String restUrl) {
        startLoading();
        appData.clear();
        Action1<Integer> subscriber = new Action1<Integer>() {
            @Override
            public void call(Integer authSuccessful) {
                evaluateAuthResult(authSuccessful);
            }
        };
        task = new PegaServerRestTask(this, appData);
        if (restUrl != null && !restUrl.isEmpty()) {
            task.loadStatusFromNetwork(restUrl, subscriber);
        } else {
            task.loadStatusFromFile(subscriber);
        }
    }

    private void launchPegaServerDataActivity() {
        Intent intent = new Intent(
                MainActivity.this,
                PegaServerDataActivity.class);
        PegaServerNetworkBinder appBinder = new PegaServerNetworkBinder();
        appBinder.setAppData(appData);
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
            Log.i(TAG, "Successfully authorized with REST service!");
        } else if (authResult == PegaServerRestTask.DATA_LOAD_SUCCESS) {
            if (verifyData()) {
                launchPegaServerDataActivity();
            } else {
                showAlert(getString(R.string.data_load_failure_alert_title),
                        getString(R.string.data_load_failure_alert_body) + statusUrl);
            }
        } else if (authResult == PegaServerRestTask.DATA_LOAD_FAILURE) {
            showAlert(getString(R.string.data_load_failure_alert_title),
                    getString(R.string.data_load_failure_alert_body) + statusUrl);
        } else if (authResult == PegaServerRestTask.ACCESS_TOKEN_FAILURE) {
            Log.e(TAG, "Access token failure!");
        }
    }

    private void loadDomainInfo() {
        task.loadDomainInfo(this, "", new Action1<BaseLayoutInfo>() {
            @Override
            public void call(BaseLayoutInfo baseLayoutInfo) {
                MainActivity.this.baseLayoutInfo = baseLayoutInfo;
                launchPegaServerDataActivity();
            }
        });
    }

    private boolean verifyData() {
        if (appData instanceof Map<?, ?>) {
            Map<String, Object> mapAppdata = (Map<String, Object>) appData;
            Set<String> keySet = mapAppdata.keySet();
            for (String key : LifecycleLayoutInfo.LC_KEY_ORDER) {
                if (!keySet.contains(key)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
