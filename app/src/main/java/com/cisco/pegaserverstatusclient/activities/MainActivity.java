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
import com.cisco.pegaserverstatusclient.parcelables.BaseInfoParcelable;
import com.cisco.pegaserverstatusclient.parcelables.PegaServerNetworkParcelable;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.HashMap;
import java.util.Map;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Fabric.with(this, new Crashlytics());

        ButterKnife.bind(this);

        verifyGooglePlayServices();
        startInstanceIDService();

        getData(getString(R.string.authorization_url),
                getString(R.string.default_user),
                getString(R.string.default_password));
    }

    @Override
    protected void onResume() {
        super.onResume();
        parseIntent();
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
            Log.d(TAG, "Intent action: " + intent.getAction());
            Log.d(TAG, "Displaying categories");

            if (intent.getData() != null) {
                Log.d(TAG, "Intent data UR: " + intent.getData().toString());
            }
        }
    }

    private void getData(String restUrl, String username, String password) {
        startLoading();
        appData.clear();
        if (!restUrl.isEmpty() && !username.isEmpty() && !password.isEmpty()) {
            task = new PegaServerRestTask(this, appData);

            Action1<Integer> subscriber = new Action1<Integer>() {
                @Override
                public void call(Integer authSuccessful) {
                    evaluateAuthResult(authSuccessful);
                }
            };

            task.loadFromNetwork(restUrl,
                    getString(R.string.authorization_url),
                    username,
                    password,
                    subscriber);
        } else {
            stopLoading();
            StringBuffer sb = new StringBuffer();
            if (restUrl.isEmpty()) {
                sb.append(getString(R.string.url_empty_alert_message));
            }
            if (username.isEmpty()) {
                sb.append(getString(R.string.username_empty_alert_message));
            }
            if (password.isEmpty()) {
                sb.append(getString(R.string.email_empty_alert_message));
            }
            showAlert(getString(R.string.input_entry_error_alert_title), sb.toString());
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
                    }
                })
                .create();
        alertDialog.show();
    }

    private void evaluateAuthResult(int authResult) {
        stopLoading();

        if (authResult == 0) {
            showAlert(getString(R.string.authentication_error_alert_title),
                    getString(R.string.authentication_error_alert_body));
        } else if (authResult == 1) {

        } else if (authResult == 2) {
            launchPegaServerDataActivity();
        } else if (authResult == -1) {

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
}
