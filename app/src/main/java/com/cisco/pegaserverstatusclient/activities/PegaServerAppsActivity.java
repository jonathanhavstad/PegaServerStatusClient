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
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.cisco.pegaserverstatusclient.R;
import com.cisco.pegaserverstatusclient.background.services.PegaRegistrationIntentService;
import com.cisco.pegaserverstatusclient.background.services.PegaServerRefreshService;
import com.cisco.pegaserverstatusclient.background.tasks.LayoutRestTask;
import com.cisco.pegaserverstatusclient.background.tasks.ServerDataRestTask;
import com.cisco.pegaserverstatusclient.binders.SubscriberBinder;
import com.cisco.pegaserverstatusclient.data.BaseLayoutInfo;
import com.cisco.pegaserverstatusclient.data.KeyMapping;
import com.cisco.pegaserverstatusclient.fragments.LayoutFragment;
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

public class PegaServerAppsActivity extends AppCompatActivity {
    private static final int PLAY_SERVICE_RESOLUTION_REQUEST = 10000;

    private String appsUrl;
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

    private static class AppMenuItem {
        private int groupId;
        private Map<String, Object> appData;
        private BaseLayoutInfo baseLayoutInfo;

        AppMenuItem(int groupId,
                    BaseLayoutInfo baseLayoutInfo,
                    Map<String, Object> appData) {
            this.groupId = groupId;
            this.baseLayoutInfo = baseLayoutInfo;
            this.appData = appData;
        }

        Map<String, Object> getAppData() {
            return appData;
        }

        int getGroupId() { return groupId; }

        BaseLayoutInfo getBaseLayoutInfo() { return baseLayoutInfo; }
    }

    private Map<View, Integer> menuOptionsViewMap = new HashMap<>();

    private Map<Integer, AppMenuItem> menuOptionsMap = new HashMap<>();

    private List<String> appFilter = new ArrayList<>();

    private AdapterView.OnItemClickListener drawerClickListener;

    @BindView(R.id.app_toolbar)
    Toolbar pegaToolbar;
    @BindView(R.id.app_swipe_refresh_view)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.app_drawer_layout)
    DrawerLayout drawerLayout;
    @BindView(R.id.app_left_drawer)
    ListView leftDrawer;

    @State
    int curAppLayoutInfoPosition;

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
        getApps(appsUrl);
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopServerRefreshServices();
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
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        int appIndex = menuOptionsViewMap.get(v);
        AppMenuItem appMenuItem = menuOptionsMap.get(appIndex);
        Map<String, Object> appData = appMenuItem.getAppData();
        int groupId = appMenuItem.getGroupId();
        int uniqueId = 0;
        for (String key : appData.keySet()) {
            String friendlyName = KeyMapping.getFriendlyName(key);
            if (friendlyName != null) {
                menu.add(groupId, uniqueId, Menu.NONE, friendlyName);
                uniqueId++;
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        drawerLayout.closeDrawers();
        appFilter.clear();
        AppMenuItem appMenuItem = menuOptionsMap.get(item.getGroupId());
        if (appMenuItem != null) {
            Map<String, Object> appData = appMenuItem.getAppData();
            int index = 0;
            for (String key : appData.keySet()) {
                String friendlyName = KeyMapping.getFriendlyName(key);
                if (friendlyName != null) {
                    if (index == item.getItemId()) {
                        Map<String, Object> childAppData = (Map<String, Object>) appData.get(key);
                        Map<String, Object> replacementAppData = new HashMap<>();
                        replacementAppData.put(key, childAppData);
                        appFilter.add(key);
                        populateCurrentFrame(appMenuItem.getBaseLayoutInfo(), replacementAppData);
                        return true;
                    }
                    index++;
                }
            }
        }
        return super.onContextItemSelected(item);
    }

    private void init() {
        verifyGooglePlayServices();
        startInstanceIDService();
        initToolbar();
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
                    }};

        drawerLayout.addDrawerListener(actionBarDrawerToggle);

        setSupportActionBar(pegaToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        setTitle(getString(R.string.app_chooser_title));
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
                refreshCurrentFrame(appLayoutInfo, appData);
            }
        };

        bgServiceInfo.connection = new BgServiceConnection(bgServiceInfo);
        Intent intent = new Intent(this, PegaServerRefreshService.class);
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

    private void getApps(String appsUrl) {
        swipeRefreshLayout.setRefreshing(true);
        LayoutRestTask task = new LayoutRestTask();
        task.loadAppsLayout(this,
                appsUrl,
                new Action1<Integer>() {
                    @Override
                    public void call(Integer loadStatus) {
                        if (loadStatus == LayoutRestTask.LOAD_FAILURE) {
                            showAlert(getString(R.string.app_load_error_alert_title),
                                    getString(R.string.app_load_error_alert_body),
                                    false);
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    }
                },
                new Action1<List<BaseLayoutInfo>>() {
                    @Override
                    public void call(List<BaseLayoutInfo> appLayoutInfoList) {
                        processAppLayoutInfoList(appLayoutInfoList);
                    }
                });
    }

    private void processAppLayoutInfoList(List<BaseLayoutInfo> appLayoutInfoList) {
        initBgServiceInfoList(appLayoutInfoList);
        initDrawer(appLayoutInfoList);
        initAppData(appLayoutInfoList);
        initRefreshView(appLayoutInfoList);
    }

    private void initBgServiceInfoList(List<BaseLayoutInfo> appLayoutInfoList) {
        bgServiceInfoList = new BgServiceInfo[appLayoutInfoList.size()];
    }

    private void initDrawer(final List<BaseLayoutInfo> appLayoutInfoList) {
        leftDrawer.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_selectable_list_item,
                appLayoutInfoList));

        leftDrawer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                boolean refresh = true;
                if (curAppLayoutInfoPosition != position) {
                    curAppLayoutInfoPosition = position;
                    refresh = false;
                }
                final BaseLayoutInfo appLayoutInfo = appLayoutInfoList.get(position);
                if (appLayoutInfo.getUrl() != null) {
                    appFilter.clear();
                    swipeRefreshLayout.setRefreshing(true);
                    getData(position, appLayoutInfo.getUrl(), appLayoutInfo, refresh);
                }
                drawerLayout.closeDrawers();
            }
        });
    }

    private void initAppData(List<BaseLayoutInfo> appLayoutInfoList) {
        for (int i = 0; i < appLayoutInfoList.size(); i++) {
            BaseLayoutInfo currentAppLayoutInfo = appLayoutInfoList.get(i);
            getData(i, currentAppLayoutInfo.getUrl(), currentAppLayoutInfo, false);
        }
    }

    private void initRefreshView(List<BaseLayoutInfo> appLayoutInfoList) {
        if (appLayoutInfoList.size() > 0 && curAppLayoutInfoPosition < appLayoutInfoList.size()) {
            final BaseLayoutInfo currentAppLayoutInfo =
                    appLayoutInfoList.get(curAppLayoutInfoPosition);
            swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    getData(curAppLayoutInfoPosition, currentAppLayoutInfo.getUrl(), currentAppLayoutInfo, true);
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
                if (index == curAppLayoutInfoPosition) {
                    if (refresh) {
                        refreshCurrentFrame(appLayoutInfo, appData);
                    } else {
                        populateCurrentFrame(appLayoutInfo, appData);
                    }
                }
                View childView = leftDrawer.getChildAt(index);
                registerForContextMenu(childView);
                childView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean refresh = true;
                        if (curAppLayoutInfoPosition != index) {
                            curAppLayoutInfoPosition = index;
                            refresh = false;
                        }
                        if (appLayoutInfo.getUrl() != null) {
                            appFilter.clear();
                            swipeRefreshLayout.setRefreshing(true);
                            getData(index, appLayoutInfo.getUrl(), appLayoutInfo, refresh);
                        }
                        drawerLayout.closeDrawers();
                    }
                });
                menuOptionsViewMap.put(childView, index);
                menuOptionsMap.put(index, new AppMenuItem(index, appLayoutInfo, appData));
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

    private void populateCurrentFrame(BaseLayoutInfo appLayoutInfo, Map<String, Object> appData) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        LayoutFragment layoutFragment = LayoutFragment.newInstance(this, appLayoutInfo, appData, false);
        fragmentTransaction.replace(R.id.app_fragment_view, layoutFragment);
        fragmentTransaction.addToBackStack(getString(R.string.app_fragment_tag));
        fragmentTransaction.commit();
        setTitle(appLayoutInfo.getFriendlyName());
    }

    private void populateCurrentFrameWithChild(BaseLayoutInfo baseLayoutInfo,
                                               Map<String, Object> appData) {

    }

    private void refreshCurrentFrame(BaseLayoutInfo appLayoutInfo, Map<String, Object> appData) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        List<Fragment> currentFragments = fragmentManager.getFragments();
        if (currentFragments.size() > 0) {
            ((LayoutFragment) currentFragments.get(currentFragments.size() - 1))
                    .updateAppData(appLayoutInfo, filterData(appData));
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
}
