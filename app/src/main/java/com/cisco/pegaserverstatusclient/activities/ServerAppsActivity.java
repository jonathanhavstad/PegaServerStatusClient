package com.cisco.pegaserverstatusclient.activities;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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

import com.cisco.pegaserverstatusclient.R;
import com.cisco.pegaserverstatusclient.background.services.RegistrationIntentService;
import com.cisco.pegaserverstatusclient.background.services.ServerRefreshService;
import com.cisco.pegaserverstatusclient.background.tasks.LayoutRestTask;
import com.cisco.pegaserverstatusclient.background.tasks.ServerDataRestTask;
import com.cisco.pegaserverstatusclient.binders.SubscriberBinder;
import com.cisco.pegaserverstatusclient.data.AppLayoutInfo;
import com.cisco.pegaserverstatusclient.data.AppsLayoutInfo;
import com.cisco.pegaserverstatusclient.data.BaseLayoutInfo;
import com.cisco.pegaserverstatusclient.fragments.DrawerFragment;
import com.cisco.pegaserverstatusclient.fragments.LayoutFragment;
import com.cisco.pegaserverstatusclient.listeners.OnBackPressedClickListener;
import com.cisco.pegaserverstatusclient.listeners.OnOpenMenuItemClickListener;
import com.cisco.pegaserverstatusclient.listeners.OnSelectMenuItemClickListener;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import icepick.Icepick;
import icepick.State;
import io.fabric.sdk.android.Fabric;
import rx.functions.Action1;

/**
 * Created by jonathanhavstad on 11/2/16.
 */

public class ServerAppsActivity extends AppCompatActivity implements
        OnOpenMenuItemClickListener,
        OnSelectMenuItemClickListener,
        OnBackPressedClickListener {

    private static final int PLAY_SERVICE_RESOLUTION_REQUEST = 10000;

    private ActionBarDrawerToggle actionBarDrawerToggle;

    private static class BgServiceInfo {
        ServiceConnection connection;
        Action1<Map<String, Object>> subscriber;
        SubscriberBinder binder;
        boolean bgServiceStarted;
    }

    private static class BgServiceConnection implements ServiceConnection {
        private BgServiceInfo bgServiceInfo;

        public BgServiceConnection(BgServiceInfo bgServiceInfo) {
            this.bgServiceInfo = bgServiceInfo;
        }
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bgServiceInfo.binder = ((SubscriberBinder) service);
            bgServiceInfo.binder.addSubscriber(bgServiceInfo.subscriber);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bgServiceInfo.binder.removeSubscriber(bgServiceInfo.subscriber);
        }
    }

    private BgServiceInfo[] bgServiceInfoList;

    private List<String> appFilter = new ArrayList<>();

    private String baseStatusUrl;

    @BindView(R.id.app_toolbar)
    Toolbar pegaToolbar;
    @BindView(R.id.app_swipe_refresh_view)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.app_drawer_layout)
    DrawerLayout drawerLayout;

    @State
    int curAppLayoutInfoPosition;

    @State
    String layoutUrl;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apps);
        Fabric.with(this, new Crashlytics());
        ButterKnife.bind(this);
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // TODO: Create a means to determine what kind of layout has been parsed and create an appropriate instance of LayoutInfo
        AppsLayoutInfo layoutInfo = new AppsLayoutInfo(null);
        layoutInfo.setUrl(layoutUrl);
        getLayout(layoutInfo);
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopServerRefreshServices();
    }

    @Override
    public void onBackPressed() {
        getSupportFragmentManager().popBackStackImmediate();
        finish();
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Icepick.saveInstanceState(this, outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Icepick.restoreInstanceState(this, savedInstanceState);
    }

    @Override
    public void open(BaseLayoutInfo layoutInfo) {
        layoutInfo.readFromNetwork(null);
        appFilter.add(layoutInfo.getKey());
        populateDrawerFrame(layoutInfo);
        populateCurrentFrame(layoutInfo.getParentLayout().filteredLayout(layoutInfo.getKey()));
    }

    @Override
    public void select(BaseLayoutInfo layoutInfo) {

    }

    @Override
    public void backPressed(BaseLayoutInfo baseLayoutInfo) {
        if (appFilter != null && appFilter.size() > 0) {
            String lastFilter = appFilter.get(appFilter.size() - 1);
            if (lastFilter.equals(baseLayoutInfo.getKey())) {
                BaseLayoutInfo parentLayout = baseLayoutInfo.getParentLayout();
                if (parentLayout != null) {
                    appFilter.remove(appFilter.size() - 1);
                    populateDrawerFrame(parentLayout);
                    if (parentLayout.getParentLayout() != null) {
                        populateCurrentFrame(parentLayout
                                .getParentLayout()
                                .filteredLayout(parentLayout.getKey()));
                    } else {
                        getSupportActionBar().setSubtitle("");
                    }
                }
            }
        }
    }

    private void init() {
        verifyGooglePlayServices();
        startInstanceIDService();
        initToolbar();
        readIntent();
        curAppLayoutInfoPosition = 0;
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
                        getString(R.string.google_play_services_error_alert_message),
                        true);
            }
            return false;
        }

        return true;
    }

    private void startInstanceIDService() {
        Intent instanceIDServiceIntent = new Intent(this, RegistrationIntentService.class);
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
                    }};

        drawerLayout.addDrawerListener(actionBarDrawerToggle);

        setSupportActionBar(pegaToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        setTitle(getString(R.string.app_chooser_title));
    }

    private void readIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            baseStatusUrl = intent.getStringExtra(getString(R.string.status_url_bundle_key));
        }
    }

    private void showAlert(String title, String body, final boolean critical) {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(body)
                .setNeutralButton(R.string.alert_dialog_confirm_btn_txt, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (critical) {
                            setResult(RESULT_CANCELED);
                            finish();
                        }
                    }
                })
                .create();
        alertDialog.show();
    }

    private void startServerRefreshService(int index, final BaseLayoutInfo appLayoutInfo) {
        BgServiceInfo bgServiceInfo = null;

        if (index < bgServiceInfoList.length && bgServiceInfoList[index] != null) {
            bgServiceInfo = bgServiceInfoList[index];
        } else {
            bgServiceInfo = new BgServiceInfo();
            bgServiceInfoList[index] = bgServiceInfo;
        }

        bgServiceInfo.subscriber = new Action1<Map<String, Object>>() {
            @Override
            public void call(Map<String, Object> appData) {
                refreshCurrentFrame(appLayoutInfo);
            }
        };

        bgServiceInfo.connection = new BgServiceConnection(bgServiceInfo);
        Intent intent = new Intent(this, ServerRefreshService.class);
        intent.putExtra(getString(R.string.status_url_bundle_key), appLayoutInfo.getUrl());
        bindService(intent, bgServiceInfo.connection, BIND_AUTO_CREATE);
        startService(intent);
        bgServiceInfo.bgServiceStarted = true;
    }

    private void stopServerRefreshServices() {
        for (int i = 0; i < bgServiceInfoList.length; i++) {
            stopServerRefreshService(i);
        }
    }

    private void stopServerRefreshService(int index) {
        if (index < bgServiceInfoList.length && bgServiceInfoList[index] != null) {
            BgServiceInfo bgServiceInfo = bgServiceInfoList[index];
            if (bgServiceInfo.bgServiceStarted) {
                bgServiceInfo.binder.removeSubscriber(bgServiceInfo.subscriber);
                unbindService(bgServiceInfo.connection);
                bgServiceInfo.bgServiceStarted = false;
            }
        }
    }

    private void getLayout(BaseLayoutInfo layoutInfo) {
        this.layoutUrl = layoutInfo.getUrl();
        swipeRefreshLayout.setRefreshing(true);
        LayoutRestTask task = new LayoutRestTask();
        task.loadAppsLayout(this,
                layoutInfo,
                new Action1<Integer>() {
                    @Override
                    public void call(Integer loadStatus) {
                        if (loadStatus == LayoutRestTask.LOAD_FAILURE) {
                            showAlert(getString(R.string.app_load_error_alert_title),
                                    getString(R.string.app_load_error_alert_body),
                                    false);
                        }
                        swipeRefreshLayout.setRefreshing(false);
                    }
                },
                new Action1<BaseLayoutInfo>() {
                    @Override
                    public void call(BaseLayoutInfo layoutInfo) {
                        processLayout(layoutInfo);
                    }
                });
    }

    private void processLayout(BaseLayoutInfo layoutInfo) {
        initBgServiceInfoList(layoutInfo);
        initData(layoutInfo);
        initRefreshView(layoutInfo);
        populateDrawerFrame(layoutInfo);
    }

    private void initBgServiceInfoList(BaseLayoutInfo layoutInfo) {
        bgServiceInfoList = new BgServiceInfo[layoutInfo.size()];
    }

    private void initData(BaseLayoutInfo layoutInfo) {
        if (layoutInfo.size() > 0) {
            swipeRefreshLayout.setRefreshing(true);
        }
        for (int i = 0; i < layoutInfo.size(); i++) {
            BaseLayoutInfo childLayout = layoutInfo.getChildLayout(i);
            getData(i, childLayout.getUrl(), childLayout, false);
        }
    }

    private void initRefreshView(BaseLayoutInfo layoutInfo) {
        if (layoutInfo.size() > 0 && curAppLayoutInfoPosition < layoutInfo.size()) {
            final BaseLayoutInfo currentAppLayoutInfo =
                    layoutInfo.getChildLayout(curAppLayoutInfoPosition);
            swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    getData(curAppLayoutInfoPosition,
                            currentAppLayoutInfo.getUrl(),
                            currentAppLayoutInfo,
                            true);
                }
            });
        } else {
            swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    swipeRefreshLayout.setRefreshing(false);
                    showAlert(getString(R.string.app_layout_refresh_error_alert_title),
                            getString(R.string.app_layout_refresh_error_alert_body),
                            false);
                }
            });
        }
    }

    private void getData(final int index,
                         String restUrl,
                         final BaseLayoutInfo appLayoutInfo,
                         final boolean refresh) {
        if (baseStatusUrl != null) {
            restUrl = baseStatusUrl;
        }
        Action1<Integer> dataLoadSubscriber = new Action1<Integer>() {
            @Override
            public void call(Integer dataLoadResult) {
                swipeRefreshLayout.setRefreshing(false);
                if (dataLoadResult == ServerDataRestTask.DATA_LOAD_FAILURE) {
                    showAlert(getString(R.string.data_load_failure_alert_title),
                            getString(R.string.data_load_failure_alert_body)
                                    + "\n"
                                    + "Application: "
                                    + appLayoutInfo.getFriendlyName()
                                    +"\n"
                                    + "URL: "
                                    + appLayoutInfo.getUrl(),
                            false);
                } else if (dataLoadResult == ServerDataRestTask.DATA_LOAD_SUCCESS) {
                    stopServerRefreshService(index);
                    startServerRefreshService(index, appLayoutInfo);
                }
            }
        };
        Action1<Map<String, Object>> appDataSubscriber = new Action1<Map<String, Object>>() {
            @Override
            public void call(Map<String, Object> appData) {
                applyAppDataToLayout(appLayoutInfo, appData);
                if (index == curAppLayoutInfoPosition) {
                    if (refresh) {
                        refreshCurrentFrame(appLayoutInfo);
                    } else {
                        populateCurrentFrame(appLayoutInfo);
                    }
                }
            }
        };
        ServerDataRestTask task = new ServerDataRestTask();
        if (!task.loadStatusFromNetwork(restUrl, dataLoadSubscriber, appDataSubscriber)) {
            swipeRefreshLayout.setRefreshing(false);
            showAlert(getString(R.string.data_load_failure_alert_title),
                    getString(R.string.data_load_failure_alert_body)
                            + "\n"
                            + "Application: "
                            + appLayoutInfo.getFriendlyName()
                            +"\n"
                            + "URL: "
                            + restUrl,
                    false);
        }

    }

    private void populateCurrentFrame(BaseLayoutInfo appLayoutInfo) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        LayoutFragment layoutFragment = LayoutFragment.newInstance(this,
                appLayoutInfo,
                appLayoutInfo.getAppData(),
                false);
        fragmentTransaction.replace(R.id.app_fragment_view, layoutFragment);
        fragmentTransaction.addToBackStack(getString(R.string.app_fragment_tag));
        fragmentTransaction.commit();
        if (appFilter.size() == 0) {
            setTitle(appLayoutInfo.getFriendlyName());
        } else {
            getSupportActionBar().setSubtitle(appLayoutInfo.getFriendlyName());
        }
    }

    private void refreshCurrentFrame(BaseLayoutInfo appLayoutInfo) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        List<Fragment> currentFragments = fragmentManager.getFragments();
        if (currentFragments.size() > 0) {
            ((LayoutFragment) currentFragments.get(currentFragments.size() - 1))
                    .updateAppData(appLayoutInfo, filterData(appLayoutInfo.getAppData()));
        }
    }

    private Map<String, Object> filterData(Map<String, Object> appData) {
        Map<String, Object> filteredData = appData;
        if (appFilter != null && appFilter.size() > 0) {
            Map<String, Object> childAppData = filteredData;
            for (String filter : appFilter) {
                childAppData = (Map<String, Object>) childAppData.get(filter);
            }
            filteredData = new HashMap<>();
            filteredData.put(appFilter.get(appFilter.size() - 1), childAppData);
        }
        return filteredData;
    }

    private void populateDrawerFrame(BaseLayoutInfo appLayoutInfo) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        DrawerFragment drawerFragment = DrawerFragment.newInstance(this, appLayoutInfo);
        fragmentTransaction.replace(R.id.drawer_fragment_view, drawerFragment);
        fragmentTransaction.commit();
    }

    private void applyAppDataToLayout(BaseLayoutInfo layoutinfo, Map<String, Object> appData) {
        layoutinfo.setAppData(appData);
    }
}
