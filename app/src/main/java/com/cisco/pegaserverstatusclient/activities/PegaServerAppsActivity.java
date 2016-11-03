package com.cisco.pegaserverstatusclient.activities;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.cisco.pegaserverstatusclient.R;
import com.cisco.pegaserverstatusclient.background.services.PegaRegistrationIntentService;
import com.cisco.pegaserverstatusclient.background.services.PegaServerRefreshService;
import com.cisco.pegaserverstatusclient.background.tasks.AppsRestTask;
import com.cisco.pegaserverstatusclient.background.tasks.PegaServerRestTask;
import com.cisco.pegaserverstatusclient.binders.SubscriberBinder;
import com.cisco.pegaserverstatusclient.data.AppLayoutInfo;
import com.cisco.pegaserverstatusclient.fragments.AppFragment;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.fabric.sdk.android.Fabric;
import rx.functions.Action1;

/**
 * Created by jonathanhavstad on 11/2/16.
 */

public class PegaServerAppsActivity extends AppCompatActivity {
    private static final int PLAY_SERVICE_RESOLUTION_REQUEST = 10000;

    private Map<String, Object> appData = new HashMap<>();
    private String appsUrl;
    private ServiceConnection connection;
    private Action1<Map<String, Object>> subscriber;
    private SubscriberBinder binder;
    private ActionBarDrawerToggle actionBarDrawerToggle;

    @BindView(R.id.app_toolbar)
    Toolbar pegaToolbar;
    @BindView(R.id.app_swipe_refresh_view)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.app_drawer_layout)
    DrawerLayout drawerLayout;
    @BindView(R.id.app_left_drawer)
    ListView leftDrawer;
    @BindView(R.id.app_fragment_view)
    FrameLayout appFragmentView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apps);
        Fabric.with(this, new Crashlytics());
        ButterKnife.bind(this);
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getApps(appsUrl);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        getSupportFragmentManager().popBackStackImmediate();
        super.onBackPressed();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        actionBarDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        actionBarDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void init() {
        verifyGooglePlayServices();
        startInstanceIDService();
        initToolbar();
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

    private void initToolbar() {
        actionBarDrawerToggle = new ActionBarDrawerToggle(this,
                drawerLayout,
                pegaToolbar,
                R.string.open_drawer,
                R.string.close_drawer) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }
        };

        drawerLayout.addDrawerListener(actionBarDrawerToggle);

        setSupportActionBar(pegaToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        setTitle(getString(R.string.app_chooser_title));
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

    private void startServerRefreshService(String statusUrl) {
        subscriber = new Action1<Map<String, Object>>() {
            @Override
            public void call(Map<String, Object> appData) {

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

    private void getApps(String appsUrl) {
        swipeRefreshLayout.setRefreshing(true);
        AppsRestTask task = new AppsRestTask();
        task.loadAppsLayout(this,
                appsUrl,
                new Action1<Integer>() {
                    @Override
                    public void call(Integer loadStatus) {
                        if (loadStatus == AppsRestTask.LOAD_FAILURE) {
                            showAlert("Applications Load Failure", "Failed to load application data!");
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    }
                },
                new Action1<List<AppLayoutInfo>>() {
                    @Override
                    public void call(List<AppLayoutInfo> appLayoutInfoList) {
                        processAppLayoutInfoList(appLayoutInfoList);
                    }
                });
    }

    private void processAppLayoutInfoList(List<AppLayoutInfo> appLayoutInfoList) {
        initDrawer(appLayoutInfoList);
        initMainFrame(appLayoutInfoList);
    }

    private void initDrawer(final List<AppLayoutInfo> appLayoutInfoList) {
        leftDrawer.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_selectable_list_item,
                populateDrawerList(appLayoutInfoList)));
        leftDrawer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AppLayoutInfo appLayoutInfo = appLayoutInfoList.get(position);
                if (appLayoutInfo.getUrl() != null) {
                    drawerLayout.closeDrawers();
                    appData.clear();
                    swipeRefreshLayout.setRefreshing(true);
                    getData(appLayoutInfo.getUrl(), appLayoutInfo);
                }
            }
        });
        for (int childIndex = 0; childIndex < leftDrawer.getChildCount(); childIndex++) {
            View childView = leftDrawer.getChildAt(childIndex);
            registerForContextMenu(childView);
        }
    }

    private void initMainFrame(List<AppLayoutInfo> appLayoutInfoList) {
        if (appLayoutInfoList.size() > 0) {
            AppLayoutInfo firstFrame = appLayoutInfoList.get(0);
            getData(firstFrame.getUrl(), firstFrame);
        }
    }

    private List<String> populateDrawerList(List<AppLayoutInfo> appLayoutInfoList) {
        List<String> drawerList = new ArrayList<>();
        for (AppLayoutInfo appLayoutInfo : appLayoutInfoList) {
            drawerList.add(appLayoutInfo.getAppId());
        }
        return drawerList;
    }

    private void getData(String restUrl, final AppLayoutInfo appLayoutInfo) {
        Action1<Integer> subscriber = new Action1<Integer>() {
            @Override
            public void call(Integer dataLoadResult) {
                swipeRefreshLayout.setRefreshing(false);
                if (dataLoadResult == PegaServerRestTask.DATA_LOAD_FAILURE) {
                    showAlert("Data Load Failure", "Failed to load application data!");
                } else if (dataLoadResult == PegaServerRestTask.DATA_LOAD_SUCCESS) {
                    populateMainFrame(appLayoutInfo, appData);
                }
            }
        };
        PegaServerRestTask task = new PegaServerRestTask(this, appData);
        task.loadStatusFromNetwork(restUrl, subscriber);
    }

    private void populateMainFrame(AppLayoutInfo appLayoutInfo, Map<String, Object> appData) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        AppFragment appFragment = AppFragment.newInstance(this, appLayoutInfo, appData);
        fragmentTransaction.replace(R.id.app_fragment_view, appFragment);
        fragmentTransaction.commit();
        setTitle(appLayoutInfo.getAppId());
    }
}
