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

import com.cisco.pegaserverstatusclient.R;
import com.cisco.pegaserverstatusclient.background.services.RegistrationIntentService;
import com.cisco.pegaserverstatusclient.background.services.ServerRefreshService;
import com.cisco.pegaserverstatusclient.background.tasks.LayoutRestTask;
import com.cisco.pegaserverstatusclient.background.tasks.ServerDataRestTask;
import com.cisco.pegaserverstatusclient.binders.SubscriberBinder;
import com.cisco.pegaserverstatusclient.layouts.AppsLayoutInfo;
import com.cisco.pegaserverstatusclient.layouts.BaseLayoutInfo;
import com.cisco.pegaserverstatusclient.fragments.DrawerFragment;
import com.cisco.pegaserverstatusclient.fragments.LayoutFragment;
import com.cisco.pegaserverstatusclient.listeners.OnBackPressedClickListener;
import com.cisco.pegaserverstatusclient.listeners.OnOpenMenuItemClickListener;
import com.cisco.pegaserverstatusclient.listeners.OnSelectMenuItemClickListener;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.ArrayList;
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
        private ServerDataRestTask task;
        private BgServiceInfo bgServiceInfo;
        private String url;

        public BgServiceConnection(ServerDataRestTask task, BgServiceInfo bgServiceInfo, String url) {
            this.task = task;
            this.bgServiceInfo = bgServiceInfo;
            this.url = url;
        }
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bgServiceInfo.binder = ((SubscriberBinder) service);
            bgServiceInfo.binder.setTask(task);
            bgServiceInfo.binder.addSubscriber(bgServiceInfo.subscriber);
            bgServiceInfo.binder.addUrl(url);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bgServiceInfo.binder.removeSubscriber(bgServiceInfo.subscriber);
        }
    }

    private BgServiceInfo[] bgServiceInfoList;

    private List<String> appFilter = new ArrayList<>();

    private String baseStatusUrl;

    private ServerDataRestTask task;

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
        FragmentManager fragmentManager = getSupportFragmentManager();
        DrawerFragment drawerFragment = (DrawerFragment)
                fragmentManager.findFragmentByTag(getString(R.string.drawer_fragment_tag));
        BaseLayoutInfo currentLayoutInfo = drawerFragment.getLayoutInfo();
        if (!currentLayoutInfo.getParentLayout().isShouldBeParent()) {
            appFilter.remove(appFilter.size() - 1);
            currentLayoutInfo = currentLayoutInfo.getParentLayout();
        }
        backPressed(currentLayoutInfo);
        if (appFilter.size() <= 2) {
            stopServerRefreshServices();
            getSupportFragmentManager().popBackStackImmediate();
            finish();
            super.onBackPressed();
        }
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
        if (layoutInfo.isShouldBeParent()) {
            addLayoutKeyToAppFilter(layoutInfo);
            populateCurrentFrame(layoutInfo.getParentLayout().filteredLayout(layoutInfo.getKey()));
        } else {
            appFilter.add(layoutInfo.getKey());
        }
        populateDrawerFrame(layoutInfo);
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
                    if (appFilter.size() > 2) {
                        appFilter.remove(appFilter.size() - 1);
                        if (appFilter.get(appFilter.size() - 1).equals(parentLayout.getKey())) {
                            appFilter.remove(appFilter.size() - 1);
                        }
                        if (parentLayout.getParentLayout() != null) {
                            open(parentLayout);
                        } else {
                            getSupportActionBar().setSubtitle("");
                        }
                    } else {
                        populateDrawerFrame(parentLayout);
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

    private void startServerRefreshService(final int index,
                                           final BaseLayoutInfo layoutInfo) {
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
                BaseLayoutInfo appLayout = layoutInfo.getChildLayout(index);
                applyDataToLayout(appLayout, appData);
                if (index == curAppLayoutInfoPosition) {
                    refreshCurrentFrame(getChildLayoutFromAppFilter(layoutInfo));
                }
            }
        };

        final BaseLayoutInfo currentAppLayoutInfo = layoutInfo.getChildLayout(index);

        bgServiceInfo.connection = new BgServiceConnection(task,
                bgServiceInfo,
                currentAppLayoutInfo.getUrl());
        Intent intent = new Intent(this, ServerRefreshService.class);
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
        Intent intent = new Intent(this, ServerRefreshService.class);
        stopService(intent);
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
            getData(i, childLayout.getUrl(), layoutInfo, false);
        }
    }

    private void initRefreshView(final BaseLayoutInfo layoutInfo) {
        if (layoutInfo.size() > 0 && curAppLayoutInfoPosition < layoutInfo.size()) {
            final BaseLayoutInfo currentAppLayoutInfo =
                    layoutInfo.getChildLayout(curAppLayoutInfoPosition);
            swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    getData(curAppLayoutInfoPosition,
                            currentAppLayoutInfo.getUrl(),
                            layoutInfo,
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
                         final BaseLayoutInfo layoutInfo,
                         final boolean refresh) {
        if (baseStatusUrl != null) {
            restUrl = baseStatusUrl;
        }
        final BaseLayoutInfo currentAppLayoutInfo = layoutInfo.getChildLayout(index);
        task = new ServerDataRestTask(this);
        Action1<Integer> dataLoadSubscriber = new Action1<Integer>() {
            @Override
            public void call(Integer dataLoadResult) {
                swipeRefreshLayout.setRefreshing(false);
                if (dataLoadResult == ServerDataRestTask.DATA_LOAD_FAILURE) {
                    showAlert(getString(R.string.data_load_failure_alert_title),
                            getString(R.string.data_load_failure_alert_body)
                                    + "\n"
                                    + "Application: "
                                    + currentAppLayoutInfo.getFriendlyName()
                                    +"\n"
                                    + "URL: "
                                    + currentAppLayoutInfo.getUrl(),
                            false);
                } else if (dataLoadResult == ServerDataRestTask.DATA_LOAD_SUCCESS) {
                    stopServerRefreshService(index);
                    startServerRefreshService(index, layoutInfo);
                }
            }
        };
        Action1<Map<String, Object>> appDataSubscriber = new Action1<Map<String, Object>>() {
            @Override
            public void call(Map<String, Object> appData) {
                applyDataToLayout(currentAppLayoutInfo, appData);
                if (index == curAppLayoutInfoPosition) {
                    if (refresh) {
                        refreshCurrentFrame(getChildLayoutFromAppFilter(layoutInfo));
                    } else {
                        open(currentAppLayoutInfo);
                    }
                }
            }
        };
        if (!task.loadStatusFromNetwork(restUrl,
                dataLoadSubscriber,
                appDataSubscriber)) {
            swipeRefreshLayout.setRefreshing(false);
            showAlert(getString(R.string.data_load_failure_alert_title),
                    getString(R.string.data_load_failure_alert_body)
                            + "\n"
                            + "Application: "
                            + currentAppLayoutInfo.getFriendlyName()
                            +"\n"
                            + "URL: "
                            + restUrl,
                    false);
        }
    }

    private void populateCurrentFrame(BaseLayoutInfo parentLayoutInfo) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        LayoutFragment layoutFragment = LayoutFragment.newInstance(this,
                parentLayoutInfo);
        fragmentTransaction.replace(R.id.app_fragment_view,
                layoutFragment,
                getString(R.string.app_fragment_tag));
        fragmentTransaction.addToBackStack(getString(R.string.app_fragment_tag));
        fragmentTransaction.commit();
        if (appFilter.size() == 0 || appFilter.size() == 1) {
            getSupportActionBar().setSubtitle("");
        } else {
            getSupportActionBar().setSubtitle(concatAppFilter());
        }
    }

    private void refreshCurrentFrame(BaseLayoutInfo appLayoutInfo) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        LayoutFragment layoutFragment = (LayoutFragment)
                fragmentManager.findFragmentByTag(getString(R.string.app_fragment_tag));
        BaseLayoutInfo filteredLayout = filterData(appLayoutInfo);
        if (!filteredLayout.isShouldBeParent()) {
            filteredLayout = filteredLayout.getParentLayout();
        }
        layoutFragment.updateAppData(filteredLayout
                .getParentLayout()
                .filteredLayout(filteredLayout.getKey()));
    }

    private BaseLayoutInfo filterData(BaseLayoutInfo layoutInfo) {
        BaseLayoutInfo currentLayout = layoutInfo;
        if (appFilter != null) {
            for (String appKey : appFilter) {
                List<BaseLayoutInfo> childrenLayouts = currentLayout.getChildrenLayouts();
                int index = 0;
                boolean foundChild = false;
                while (index < childrenLayouts.size() && !foundChild) {
                    BaseLayoutInfo childLayout = childrenLayouts.get(index);
                    if (childLayout.getKey().equals(appKey)) {
                        currentLayout = childLayout;
                        foundChild = true;
                    }
                    index++;
                }
            }
        }
        return currentLayout;
    }

    private void populateDrawerFrame(BaseLayoutInfo appLayoutInfo) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        DrawerFragment drawerFragment = DrawerFragment.newInstance(this, appLayoutInfo);
        fragmentTransaction.replace(R.id.drawer_fragment_view,
                drawerFragment,
                getString(R.string.drawer_fragment_tag));
        fragmentTransaction.commit();
    }

    private void applyDataToLayout(BaseLayoutInfo layoutinfo, Object appData) {
        layoutinfo.setAppData(appData);
        List<BaseLayoutInfo> childrenLayouts = layoutinfo.getChildrenLayouts();
        Map<String, Object> mapAppData = (Map<String, Object>) appData;
        if (childrenLayouts != null) {
            for (BaseLayoutInfo childLayout : childrenLayouts) {
                Object childAppData = mapAppData.get(childLayout.getKey());
                if (childAppData != null) {
                    if (childAppData instanceof Map<?,?>) {
                        applyDataToLayout(childLayout, childAppData);
                    } else {
                        String childData = null;
                        if (childAppData instanceof List<?>) {
                            List<String> listData = (List<String>) childAppData;
                            childData = BaseLayoutInfo.concatListData(listData);
                        } else {
                            childData = (String) childAppData;
                        }
                        childLayout.setAppData(childData);
                    }
                }
            }
        }
    }

    private void addLayoutKeyToAppFilter(BaseLayoutInfo layoutInfo) {
        if (appFilter != null) {
            BaseLayoutInfo parentLayoutInfo = layoutInfo.getParentLayout();
            int parentIndex = appFilter.size() - 1;
            if ((parentIndex == -1 && parentLayoutInfo != null) ||
                    !appFilter.get(parentIndex).equals(parentLayoutInfo.getKey()) &&
                    !appFilter.get(parentIndex).equals(layoutInfo.getKey())) {
                appFilter.add(parentLayoutInfo.getKey());
            }
            if (appFilter.size() > 0) {
                if (appFilter.size() == 1) {
                    setTitle(layoutInfo.getFriendlyName());
                }
                String lastKey = appFilter.get(appFilter.size() - 1);
                if (!lastKey.equals(layoutInfo.getKey())) {
                    appFilter.add(layoutInfo.getKey());
                }
            } else {
                appFilter.add(layoutInfo.getKey());
            }
        }
    }

    private BaseLayoutInfo getChildLayoutFromAppFilter(BaseLayoutInfo layoutInfo) {
        BaseLayoutInfo childLayoutInfo = layoutInfo;

        for (String app : appFilter) {
            boolean foundNextChild = false;
            int childIndex = 0;
            List<BaseLayoutInfo> childrenLayouts = layoutInfo.getChildrenLayouts();
            while (!foundNextChild && childIndex < childrenLayouts.size()) {
                if (childrenLayouts.get(childIndex).getKey().equals(app)) {
                    foundNextChild = true;
                    childLayoutInfo = childrenLayouts.get(childIndex);
                }
                childIndex++;
            }
        }

        return childLayoutInfo;
    }

    private String concatAppFilter() {
        StringBuffer sb = new StringBuffer();
        int index = 0;
        for (String value : appFilter) {
            sb.append(value);
            if (index < appFilter.size() - 1) {
                sb.append(" / ");
            }
            index++;
        }
        return sb.toString();
    }
}
