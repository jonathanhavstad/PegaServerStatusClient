package com.cisco.pegaserverstatusclient.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
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
import com.cisco.pegaserverstatusclient.binders.LayoutFilterBinder;
import com.cisco.pegaserverstatusclient.binders.BgServiceInfoBinder;
import com.cisco.pegaserverstatusclient.listeners.OnDataReadyListener;
import com.cisco.pegaserverstatusclient.listeners.OnPositionVisibleListener;
import com.cisco.pegaserverstatusclient.utilities.BgServiceConnection;
import com.cisco.pegaserverstatusclient.utilities.BgServiceInfo;
import com.cisco.pegaserverstatusclient.layouts.AppsLayoutInfo;
import com.cisco.pegaserverstatusclient.layouts.BaseLayoutInfo;
import com.cisco.pegaserverstatusclient.fragments.DrawerFragment;
import com.cisco.pegaserverstatusclient.fragments.LayoutFragment;
import com.cisco.pegaserverstatusclient.listeners.OnBackPressedClickListener;
import com.cisco.pegaserverstatusclient.listeners.OnBgDataReadyListener;
import com.cisco.pegaserverstatusclient.listeners.OnOpenMenuItemClickListener;
import com.cisco.pegaserverstatusclient.listeners.OnSelectMenuItemClickListener;
import com.cisco.pegaserverstatusclient.utilities.DataCallbackHolder;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

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
        OnBackPressedClickListener,
        OnPositionVisibleListener {

    private static final int PLAY_SERVICE_RESOLUTION_REQUEST = 10000;

    public static final int RESULT_CLOSED = 9999;

    public static final int RESULT_LOGIN = 9998;

    private ActionBarDrawerToggle actionBarDrawerToggle;

    private BgServiceInfo[] bgServiceInfoList;

    private DataCallbackHolder[] dataCallbackHolderList;

    private Stack<BaseLayoutInfo> layoutFilter = new Stack<>();

    private String baseStatusUrl;

    private ServerDataRestTask[] bgTasks;

    private boolean stopRefresh;

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
    @State
    int refreshWaitTime;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apps);
        Fabric.with(this, new Crashlytics());
        ButterKnife.bind(this);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }
        init();
        initLayout(false);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        stopServerRefreshServices();
        stopRefresh = false;
        if (bgServiceInfoList != null) {
            for (int i = 0; i < bgServiceInfoList.length; i++) {
                if (bgServiceInfoList[i] != null) {
                    startBgService(bgServiceInfoList[i]);
                }
            }
            startRefreshing(refreshWaitTime);
            BaseLayoutInfo rootLayoutInfo = BaseLayoutInfo.getRoot(layoutFilter.peek());
            getData(curAppLayoutInfoPosition,
                    rootLayoutInfo.getChildLayout(curAppLayoutInfoPosition).getUrl(),
                    rootLayoutInfo,
                    true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopRefresh = true;
        stopServerRefreshServices();
    }

    @Override
    public void onBackPressed() {
        int preBackLayoutFilterSize = layoutFilter.size();
        FragmentManager fragmentManager = getSupportFragmentManager();
        DrawerFragment drawerFragment = (DrawerFragment)
                fragmentManager.findFragmentByTag(getString(R.string.drawer_fragment_tag));
        BaseLayoutInfo currentLayoutInfo = drawerFragment.getLayoutInfo();
        if (currentLayoutInfo != null &&
                !currentLayoutInfo.getParentLayout().isShouldBeParent()) {
            layoutFilter.remove(layoutFilter.size() - 1);
            currentLayoutInfo = currentLayoutInfo.getParentLayout();
        }
        backPressed(currentLayoutInfo);
        if (preBackLayoutFilterSize <= 2) {
            stopServerRefreshServices();
            getSupportFragmentManager().popBackStackImmediate();
            setResult(RESULT_CLOSED);
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

        LayoutFilterBinder layoutFilterBinder = new LayoutFilterBinder();
        layoutFilterBinder.setBaseLayoutInfoStack(layoutFilter);
        outState.putBinder(getString(R.string.app_filter_bundle_key), layoutFilterBinder);

        BgServiceInfoBinder bgServiceInfoBinder = new BgServiceInfoBinder();
        bgServiceInfoBinder.setBgServiceInfoList(bgServiceInfoList);
        outState.putBinder(getString(R.string.bg_serviceinfo_list_bundle_key), bgServiceInfoBinder);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Icepick.restoreInstanceState(this, savedInstanceState);


        LayoutFilterBinder layoutFilterBinder = (LayoutFilterBinder)
                savedInstanceState.getBinder(getString(R.string.app_filter_bundle_key));
        if (layoutFilterBinder != null) {
            layoutFilter = layoutFilterBinder.getBaseLayoutInfoStack();
        }
        if (layoutFilter == null) {
            layoutFilter = new Stack<>();
        }

        BgServiceInfoBinder bgServiceInfoBinder = (BgServiceInfoBinder)
                savedInstanceState.getBinder(getString(R.string.bg_serviceinfo_list_bundle_key));
        if (bgServiceInfoBinder != null) {
            if (bgServiceInfoList == null) {
                bgServiceInfoList = bgServiceInfoBinder.getBgServiceInfoList();
            }
        }
    }

    @Override
    public void open(BaseLayoutInfo layoutInfo) {
        swipeRefreshLayout.setRefreshing(false);
        if (layoutInfo.readFromInputStream(null)) {
            if (!hasGrandChildren(layoutInfo) && !layoutInfo.forceDrawerLayout()) {
                populateDrawerFrame(layoutInfo.getParentLayout());
                drawerLayout.closeDrawers();
            } else {
                populateDrawerFrame(layoutInfo);
                if (layoutInfo.forceDrawerLayout()) {
                    drawerLayout.closeDrawers();
                }
            }
            applyLayoutToLayoutFilter(layoutInfo);
            if (layoutInfo.isShouldBeParent()) {
                populateCurrentFrame(layoutInfo.getParentLayout().filteredLayout(layoutInfo.getKey()));
            }
        } else if (layoutInfo.getLayoutIndex() != -1) {
            startRefreshing(refreshWaitTime * 2);
            drawerLayout.closeDrawers();
            getData(layoutInfo.getLayoutIndex(),
                    layoutInfo.getUrl(),
                    BaseLayoutInfo.getRoot(layoutInfo),
                    false);
        }

        if (layoutInfo.getLayoutIndex() != -1) {
            curAppLayoutInfoPosition = layoutInfo.getLayoutIndex();
        }
    }

    @Override
    public void select(BaseLayoutInfo layoutInfo) {

    }

    @Override
    public void backPressed(BaseLayoutInfo baseLayoutInfo) {
        if (layoutFilter != null && layoutFilter.size() > 0) {
            if (layoutFilter.size() > 2) {
                BaseLayoutInfo lastFilter = layoutFilter.pop();
                if (!layoutFilter.peek().isShouldBeParent() && layoutFilter.size() > 2) {
                    lastFilter = layoutFilter.pop();
                }
                if (layoutFilter.size() > 1) {
                    open(layoutFilter.pop());
                } else {
                    getSupportActionBar().setSubtitle("");
                }
            } else {
                populateDrawerFrame(baseLayoutInfo.getParentLayout());
            }
        }
    }

    @Override
    public void positionVisible(int position) {
        if (position == 0) {
            swipeRefreshLayout.setEnabled(true);
        } else {
            swipeRefreshLayout.setEnabled(false);
        }
    }

    private void init() {
        verifyGooglePlayServices();
        startInstanceIDService();
        initToolbar();
        readIntent();
        refreshWaitTime = getResources().getInteger(R.integer.data_refresh_timeout_ms);
    }

    private void initLayout(boolean refresh) {
        curAppLayoutInfoPosition = 0;

        AppsLayoutInfo layoutInfo = new AppsLayoutInfo(null);
        layoutInfo.setUrl(layoutUrl);
        getLayout(layoutInfo, refresh);
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

            boolean stopActivity =
                    intent.getBooleanExtra(getString(R.string.stop_activity_bundle_key), false);

            if (stopActivity) {
                setResult(RESULT_LOGIN);
                finish();
            }
        }
    }

    private void showAlert(String title, String body, final boolean critical) {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(body)
                .setNeutralButton(R.string.alert_dialog_confirm_btn_txt,
                        new DialogInterface.OnClickListener() {
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

    private void startServerRefreshService(int index, final BaseLayoutInfo layoutInfo) {
        BgServiceInfo bgServiceInfo = null;

        if (index < bgServiceInfoList.length && bgServiceInfoList[index] != null) {
            bgServiceInfo = bgServiceInfoList[index];
        } else {
            bgServiceInfo = new BgServiceInfo(index,
                    layoutInfo,
                    new OnBgDataReadyListener() {
                        @Override
                        public void send(BgServiceInfo bgServiceInfo, Map<String, Object> appData) {
                            BaseLayoutInfo appLayout =
                                    bgServiceInfo
                                            .getLayoutInfo()
                                            .getChildLayout(bgServiceInfo.getIndex());
                            applyDataToLayout(appLayout, appData);
                            if (bgServiceInfo.getIndex() == curAppLayoutInfoPosition &&
                                    !stopRefresh) {
                                refreshCurrentFrame(layoutFilter.peek());
                            }
                        }
                    });
            bgServiceInfoList[index] = bgServiceInfo;
        }

        bgServiceInfo.setIndex(index);

        BaseLayoutInfo currentAppLayoutInfo = layoutInfo.getChildLayout(index);

        ServerDataRestTask task = null;

        if (index < bgTasks.length && bgTasks[index] != null) {
            task = bgTasks[index];
        } else {
            task = new ServerDataRestTask(this);
            bgTasks[index] = task;
        }

        if (index == 0 && baseStatusUrl != null) {
            currentAppLayoutInfo.setUrl(baseStatusUrl);
        }

        bgServiceInfo.setConnection(new BgServiceConnection(task,
                bgServiceInfo,
                currentAppLayoutInfo.getUrl()));

        startBgService(bgServiceInfo);
    }

    private void startBgService(BgServiceInfo bgServiceInfo) {
        Intent intent = new Intent(this, ServerRefreshService.class);
        bindService(intent, bgServiceInfo.getConnection(), BIND_AUTO_CREATE);
        startService(intent);
        bgServiceInfo.setBgServiceStarted(true);
    }

    private void stopServerRefreshServices() {
        for (int i = 0; i < bgServiceInfoList.length; i++) {
            stopServerRefreshService(i);
        }
    }

    private void stopServerRefreshService(int index) {
        if (index < bgServiceInfoList.length && bgServiceInfoList[index] != null) {
            BgServiceInfo bgServiceInfo = bgServiceInfoList[index];
            if (bgServiceInfo.isBgServiceStarted()) {
                bgServiceInfo.getBinder().removeSubscriber(bgServiceInfo.getSubscriber());
                unbindService(bgServiceInfo.getConnection());
                bgServiceInfo.setBgServiceStarted(false);
            }
        }
        Intent intent = new Intent(this, ServerRefreshService.class);
        stopService(intent);
    }

    private void getLayout(BaseLayoutInfo layoutInfo, final boolean refresh) {
        this.layoutUrl = layoutInfo.getUrl();
        startRefreshing(refreshWaitTime);
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
                        processLayout(layoutInfo, refresh);
                    }
                });
    }

    private void processLayout(BaseLayoutInfo layoutInfo, boolean refresh) {
        initBgServiceInfoList(layoutInfo);
        initDataCallbackHolderList(layoutInfo);
        initBgTasks(layoutInfo);
        initData(layoutInfo, refresh);
        initRefreshView(layoutInfo);
        populateDrawerFrame(layoutInfo);
        if (refresh) {
            reInitLayoutFilter();
        }
    }

    private void initBgServiceInfoList(BaseLayoutInfo layoutInfo) {
        bgServiceInfoList = new BgServiceInfo[layoutInfo.size()];
    }

    public void initDataCallbackHolderList(BaseLayoutInfo layoutInfo) {
        dataCallbackHolderList = new DataCallbackHolder[layoutInfo.size()];
    }

    public void initBgTasks(BaseLayoutInfo layoutInfo) {
        bgTasks = new ServerDataRestTask[layoutInfo.size()];
    }

    private void initData(BaseLayoutInfo layoutInfo, boolean refresh) {
        if (layoutInfo.size() > 0) {
            startRefreshing(refreshWaitTime);
        }
        if (layoutInfo.size() > 0) {
            BaseLayoutInfo childLayout = layoutInfo.getChildLayout(0);
            getData(0, childLayout.getUrl(), layoutInfo, refresh);
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

    private void getData(int index,
                         String restUrl,
                         BaseLayoutInfo layoutInfo,
                         boolean refresh) {

        ServerDataRestTask task = null;

        if (index < bgTasks.length && bgTasks[index] != null) {
            task = bgTasks[index];
        } else {
            task = new ServerDataRestTask(this);
            bgTasks[index] = task;
        }

        if (index == 0 && baseStatusUrl != null) {
            restUrl = baseStatusUrl;
        }

        DataCallbackHolder dataCallbackHolder = null;
        if (index < dataCallbackHolderList.length && dataCallbackHolderList[index] != null) {
            dataCallbackHolder = dataCallbackHolderList[index];
            dataCallbackHolder.setRefresh(refresh);
        } else {
            dataCallbackHolder = new DataCallbackHolder(layoutInfo,
                    index,
                    refresh,
                    new OnDataReadyListener() {
                        @Override
                        public void sendDataLoadResult(DataCallbackHolder dataCallbackHolder,
                                                       int dataLoadResult) {
                            swipeRefreshLayout.setRefreshing(false);

                            BaseLayoutInfo currentAppLayoutInfo =
                                    dataCallbackHolder
                                            .getLayoutInfo()
                                            .getChildLayout(dataCallbackHolder.getIndex());

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
                                stopServerRefreshService(dataCallbackHolder.getIndex());
                                startServerRefreshService(dataCallbackHolder.getIndex(),
                                        dataCallbackHolder.getLayoutInfo());
                            }
                        }

                        @Override
                        public void sendData(DataCallbackHolder dataCallbackHolder,
                                             Map<String, Object> appData) {
                            BaseLayoutInfo currentAppLayoutInfo =
                                    dataCallbackHolder
                                            .getLayoutInfo()
                                            .getChildLayout(dataCallbackHolder.getIndex());
                            applyDataToLayout(currentAppLayoutInfo, appData);
                            if (dataCallbackHolder.getIndex() == curAppLayoutInfoPosition) {
                                if (dataCallbackHolder.isRefresh() && !stopRefresh) {
                                    refreshCurrentFrame(layoutFilter.peek());
                                } else {
                                    open(currentAppLayoutInfo);
                                }
                            }
                        }
                    });
            dataCallbackHolderList[index] = dataCallbackHolder;
        }
        dataCallbackHolder.setLayoutInfo(layoutInfo);

        boolean isJsonArray = true;
        if (index == 1) {
            isJsonArray = false;
        }

        if (!task.loadStatusFromNetwork(this,
                restUrl,
                dataCallbackHolder.getDataLoadSubscriber(),
                dataCallbackHolder.getAppDataSubscriber(),
                isJsonArray)) {
            swipeRefreshLayout.setRefreshing(false);
            showAlert(getString(R.string.data_load_failure_alert_title),
                    getString(R.string.data_load_failure_alert_body)
                            + "\n"
                            + "Application: "
                            + layoutInfo.getChildLayout(index).getFriendlyName()
                            +"\n"
                            + "URL: "
                            + restUrl,
                    false);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(refreshWaitTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (swipeRefreshLayout.isRefreshing()) {
                    ServerAppsActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }
            }
        }).start();
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
        if (layoutFilter.size() == 0 || layoutFilter.size() == 1) {
            getSupportActionBar().setSubtitle("");
        } else {
            getSupportActionBar().setSubtitle(concatAppFilter());
        }
    }

    private void refreshCurrentFrame(BaseLayoutInfo appLayoutInfo) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        LayoutFragment layoutFragment = (LayoutFragment)
                fragmentManager.findFragmentByTag(getString(R.string.app_fragment_tag));
        if (!appLayoutInfo.isShouldBeParent()) {
            appLayoutInfo = appLayoutInfo.getParentLayout();
        }
        layoutFragment.updateAppData(appLayoutInfo
                .getParentLayout()
                .filteredLayout(appLayoutInfo.getKey()));
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

    private String concatAppFilter() {
        StringBuffer sb = new StringBuffer();
        int index = 0;
        for (BaseLayoutInfo value : layoutFilter) {
            sb.append(value.getKey());
            if (index < layoutFilter.size() - 1) {
                sb.append(" / ");
            }
            index++;
        }
        return sb.toString();
    }

    private void reInitLayoutFilter() {
        for (int i = 0; i < layoutFilter.size(); i++) {
            layoutFilter.get(i).readFromInputStream(null);
        }
    }

    private void applyLayoutToLayoutFilter(BaseLayoutInfo layoutInfo) {
        List<BaseLayoutInfo> reverseLayoutFilter = new ArrayList<>();
        BaseLayoutInfo currentLayoutInfo = layoutInfo;
        while (currentLayoutInfo != null) {
            reverseLayoutFilter.add(currentLayoutInfo);
            currentLayoutInfo = currentLayoutInfo.getParentLayout();
        }

        layoutFilter.clear();
        String appName = "";
        String currentLevelName = "";
        for (int i = reverseLayoutFilter.size() - 1; i >= 0; i--) {
            layoutFilter.push(reverseLayoutFilter.get(i));
            if (i == reverseLayoutFilter.size() - 2) {
                appName = reverseLayoutFilter.get(i).getFriendlyName();
            } else if (i == 0) {
                currentLevelName = reverseLayoutFilter.get(i).getFriendlyName();
            }
        }
        if (!appName.isEmpty() && !currentLevelName.isEmpty()) {
            setTitle(appName + " (" + currentLevelName + ")");
        } else if (!appName.isEmpty()) {
            setTitle(appName);
        }
    }

    private boolean hasGrandChildren(BaseLayoutInfo layoutInfo) {
        List<BaseLayoutInfo> childrenLayoutInfoList = layoutInfo.getChildrenLayouts();
        if (childrenLayoutInfoList != null) {
            for (BaseLayoutInfo childLayoutInfo : childrenLayoutInfoList) {
                childLayoutInfo.readFromInputStream(null);
                List<BaseLayoutInfo> grandchildrenLayoutInfoList =
                        childLayoutInfo.getChildrenLayouts();
                if (grandchildrenLayoutInfoList != null && grandchildrenLayoutInfoList.size() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private void startRefreshing(final int timeout) {
        swipeRefreshLayout.setRefreshing(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (swipeRefreshLayout.isRefreshing()) {
                    ServerAppsActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }
            }
        }).start();
    }
}
