package com.ciscozensarpegateam.pegaserverstatusclient.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.idling.CountingIdlingResource;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.ciscozensarpegateam.pegaserverstatusclient.R;
import com.ciscozensarpegateam.pegaserverstatusclient.background.tasks.PegaServerRestTask;
import com.ciscozensarpegateam.pegaserverstatusclient.binders.PegaServerNetworkBinder;
import com.ciscozensarpegateam.pegaserverstatusclient.data.PegaServerNetworkData;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.functions.Action1;

public class MainActivity extends AppCompatActivity {
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

    private CountingIdlingResource countingIdlingResource =
            new CountingIdlingResource("LoaddAppDataIdlingResource");

    private PegaServerRestTask task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        getViewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getData(getViewUrl.getText().toString(),
                        usernameView.getText().toString(),
                        passwordView.getText().toString());
            }
        });
    }

    private void getData(String restUrl, String username, String password) {
        startLoading();
        appData.clear();
        if (!restUrl.isEmpty() && !username.isEmpty() && !password.isEmpty()) {
            task = new PegaServerRestTask(this, appData);

            Action1<Boolean> subscriber = new Action1<Boolean>() {
                @Override
                public void call(Boolean authSuccessful) {
                    evaluateAuthResult(authSuccessful);
                }
            };

            task.loadFromNetwork(restUrl,
                    getString(R.string.authorization_url),
                    username,
                    password,
                    subscriber);
        } else {
            StringBuffer sb = new StringBuffer();
            if (restUrl.isEmpty()) {
                sb.append("Please input a REST URL.  ");
            }
            if (username.isEmpty()) {
                sb.append("Please input a username.  ");
            }
            if (password.isEmpty()) {
                sb.append("Please input a password.  ");
            }
            showAlert("Empty Fields", sb.toString());
        }
    }

    private void launchPegaServerDataActivity() {
        Intent intent = new Intent(
                MainActivity.this,
                PegaServerDataActivity.class);
        PegaServerNetworkBinder binder = new PegaServerNetworkBinder();
        binder.setAppData(appData);
        PegaServerNetworkData restData = new PegaServerNetworkData();
        restData.setBinder(binder);
        intent.putExtra(getString(R.string.app_binder_data_bundle_key), restData);
        startActivity(intent);
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
                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        alertDialog.show();
    }

    private void evaluateAuthResult(boolean authSuccessful) {
        stopLoading();

        if (!authSuccessful) {
            showAlert("Authentication Failure",
                    "Invalid credentials.  Please try again.");
        } else {
            launchPegaServerDataActivity();
        }
    }
}
