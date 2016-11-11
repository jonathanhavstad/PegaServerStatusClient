package com.cisco.pegaserverstatusclient.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
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
import com.cisco.pegaserverstatusclient.listeners.OnDataReadyListener;
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
        OnBackPressedClickListener {

    private static final int PLAY_SERVICE_RESOLUTION_REQUEST = 10000;

    private ActionBarDrawerToggle actionBarDrawerToggle;

    private BgServiceInfo[] bgServiceInfoList;

    private DataCallbackHolder[] dataCallbackHolderList;

    private Stack<BaseLayoutInfo> layoutFilter = new Stack<>();

    private String baseStatusUrl;

    private ServerDataRestTask[] bgTasks;

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
        initLayout(false);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        init();
        reInitLayoutFilter();
        if (layoutFilter.size() > 0) {
            BaseLayoutInfo appsLayoutInfo = layoutFilter.get(0);
            List<BaseLayoutInfo> childrenLayouts = appsLayoutInfo.getChildrenLayouts();
            for (int i = 0; i < childrenLayouts.size(); i++) {
                getData(i, childrenLayouts.get(i).getUrl(), appsLayoutInfo, true);
            }
            refreshCurrentFrame(layoutFilter.peek());
        }
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
            layoutFilter.remove(layoutFilter.size() - 1);
            currentLayoutInfo = currentLayoutInfo.getParentLayout();
        }
        backPressed(currentLayoutInfo);
        if (layoutFilter.size() <= 1) {
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
        LayoutFilterBinder layoutFilterBinder = new LayoutFilterBinder();
        layoutFilterBinder.setBaseLayoutInfoStack(layoutFilter);
        outState.putBinder(getString(R.string.app_filter_bundle_key), layoutFilterBinder);
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
    }

    @Override
    public void open(BaseLayoutInfo layoutInfo) {
        swipeRefreshLayout.setRefreshing(false);
        layoutInfo.readFromNetwork(null);
        if (!hasGrandChildren(layoutInfo)) {
            populateDrawerFrame(layoutInfo.getParentLayout());
            drawerLayout.closeDrawers();
        } else {
            populateDrawerFrame(layoutInfo);
        }
        applyLayoutToLayoutFilter(layoutInfo);
        if (layoutInfo.isShouldBeParent()) {
            populateCurrentFrame(layoutInfo.getParentLayout().filteredLayout(layoutInfo.getKey()));
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
                if (lastFilter.size() > 1) {
                    open(layoutFilter.pop());
                } else {
                    getSupportActionBar().setSubtitle("");
                }
            } else {
                populateDrawerFrame(baseLayoutInfo.getParentLayout());
            }
        }
    }

    private void init() {
        verifyGooglePlayServices();
        startInstanceIDService();
        initToolbar();
        readIntent();
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
                            if (bgServiceInfo.getIndex() == curAppLayoutInfoPosition) {
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

        bgServiceInfo.setConnection(new BgServiceConnection(task,
                bgServiceInfo,
                currentAppLayoutInfo.getUrl()));
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
            swipeRefreshLayout.setRefreshing(true);
        }
        for (int i = 0; i < layoutInfo.size(); i++) {
            BaseLayoutInfo childLayout = layoutInfo.getChildLayout(i);
            getData(i, childLayout.getUrl(), layoutInfo, refresh);
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
        if (baseStatusUrl != null) {
            restUrl = baseStatusUrl;
        }

        ServerDataRestTask task = null;

        if (index < bgTasks.length && bgTasks[index] != null) {
            task = bgTasks[index];
        } else {
            task = new ServerDataRestTask(this);
            bgTasks[index] = task;
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
                            BaseLayoutInfo currentAppLayoutInfo =
                                    dataCallbackHolder
                                            .getLayoutInfo()
                                            .getChildLayout(dataCallbackHolder.getIndex());

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
                                if (dataCallbackHolder.isRefresh()) {
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

        if (!task.loadStatusFromNetwork(restUrl,
                dataCallbackHolder.getDataLoadSubscriber(),
                dataCallbackHolder.getAppDataSubscriber())) {
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
        BaseLayoutInfo filteredLayout = appLayoutInfo; //filterData(appLayoutInfo);
        if (!filteredLayout.isShouldBeParent()) {
            filteredLayout = filteredLayout.getParentLayout();
        }
        layoutFragment.updateAppData(filteredLayout
                .getParentLayout()
                .filteredLayout(filteredLayout.getKey()));
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
            layoutFilter.get(i).readFromNetwork(null);
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
        for (int i = reverseLayoutFilter.size() - 1; i >= 0; i--) {
            layoutFilter.push(reverseLayoutFilter.get(i));
            if (i == reverseLayoutFilter.size() - 2) {
                setTitle(reverseLayoutFilter.get(i).getFriendlyName());
            }
        }
    }

    private boolean hasGrandChildren(BaseLayoutInfo layoutInfo) {
        List<BaseLayoutInfo> childrenLayoutInfoList = layoutInfo.getChildrenLayouts();
        if (childrenLayoutInfoList != null) {
            for (BaseLayoutInfo childLayoutInfo : childrenLayoutInfoList) {
                childLayoutInfo.readFromNetwork(null);
                List<BaseLayoutInfo> grandchildrenLayoutInfoList =
                        childLayoutInfo.getChildrenLayouts();
                if (grandchildrenLayoutInfoList != null && grandchildrenLayoutInfoList.size() > 0) {
                    return true;
                }
            }
        }
        return false;
    }
}
