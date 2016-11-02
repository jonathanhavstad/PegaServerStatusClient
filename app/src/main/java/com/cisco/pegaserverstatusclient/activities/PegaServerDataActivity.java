package com.cisco.pegaserverstatusclient.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.cisco.pegaserverstatusclient.R;
import com.cisco.pegaserverstatusclient.adapters.TabViewAdapter;
import com.cisco.pegaserverstatusclient.background.services.PegaServerRefreshService;
import com.cisco.pegaserverstatusclient.binders.BaseLayoutInfoBinder;
import com.cisco.pegaserverstatusclient.binders.PegaServerNetworkBinder;
import com.cisco.pegaserverstatusclient.binders.SubscriberBinder;
import com.cisco.pegaserverstatusclient.data.AppLayoutInfo;
import com.cisco.pegaserverstatusclient.data.BaseLayoutInfo;
import com.cisco.pegaserverstatusclient.data.DomainLayoutInfo;
import com.cisco.pegaserverstatusclient.data.DrawerListItem;
import com.cisco.pegaserverstatusclient.data.LifecycleLayoutInfo;
import com.cisco.pegaserverstatusclient.data.ServerLayoutInfo;
import com.cisco.pegaserverstatusclient.fragments.PegaBaseFragment;
import com.cisco.pegaserverstatusclient.fragments.PegaChildFragment;
import com.cisco.pegaserverstatusclient.fragments.PegaParentFragment;
import com.cisco.pegaserverstatusclient.listeners.OnItemSelectedListener;
import com.cisco.pegaserverstatusclient.listeners.OnUpdateDataListener;
import com.cisco.pegaserverstatusclient.parcelables.BaseInfoParcelable;
import com.cisco.pegaserverstatusclient.parcelables.PegaServerNetworkParcelable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import icepick.Icepick;
import icepick.State;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;
import rx.functions.Action1;

public class PegaServerDataActivity extends AppCompatActivity
        implements OnUpdateDataListener {
    private static final String TAG = "PegaDataActivity";

    private static WeakReference<PegaServerDataActivity> weakActivity = null;

    private TabViewAdapter tabViewAdapter;
    private String friendlyName;
    private ViewPager.OnPageChangeListener onPageChangeListener;
    private SubscriberBinder binder;
    private ServiceConnection connection;
    private PegaBaseFragment currentChildFragment;
    private Action1<Map<String, Object>> subscriber;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private BaseLayoutInfo baseLayoutInfo;
    private Object appData;
    private Object drawerData;
    private List<DrawerListItem> drawerValueList;
    private PegaServerNetworkBinder appBinder;

    @BindView(R.id.pega_data_pager)
    ViewPager pegaDataPager;
    @BindView(R.id.pega_toolbar)
    Toolbar pegaToolbar;
    @BindView(R.id.swipe_refresh_view)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawerLayout;
    @BindView(R.id.left_drawer)
    ListView leftDrawer;

    @State
    PegaServerNetworkParcelable appParcelable;
    @State
    BaseInfoParcelable layoutParcelable;
    @State
    int pageNum;
    @State
    int tabIndex;
    @State
    String title;
    @State
    long lastRefreshTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pega_server_data);

        Fabric.with(this, new Crashlytics());

        ButterKnife.bind(this);

        weakActivity = new WeakReference<>(this);

        Intent intent = getIntent();
        if (intent != null) {
            appParcelable = intent.getParcelableExtra(getString(R.string.app_binder_data_bundle_key));
            layoutParcelable =
                    intent.getParcelableExtra(getString(R.string.info_binder_data_bundle_key));
            pageNum = intent.getIntExtra(getString(R.string.current_page_bundle_key), 1);
            tabIndex= 0;
        } else {
            Icepick.restoreInstanceState(this, savedInstanceState);
        }

        appBinder = appParcelable.getBinder();
        appData = appBinder.getAppData();
        drawerData = appBinder.getDrawerData();

        tabViewAdapter = new TabViewAdapter(getSupportFragmentManager());

        if (layoutParcelable != null) {
            BaseLayoutInfoBinder baseLayoutInfoBinder =
                    layoutParcelable.getBaseLayoutInfoBinder();
            baseLayoutInfo = baseLayoutInfoBinder.getBaseLayoutInfo();
            if (baseLayoutInfo instanceof LifecycleLayoutInfo) {
                if (appData instanceof Map<?, ?>) {
                    Map<String, Object> mapAppData = (Map<String, Object>) appData;
                    int index = 0;
                    for (String lcKey : LifecycleLayoutInfo.LC_KEY_ORDER) {
                        if (mapAppData.containsKey(lcKey.toLowerCase())) {
                            PegaParentFragment domainFragment =
                                    PegaParentFragment
                                            .newInstance(this,
                                                    LifecycleLayoutInfo.LC_MAPPING.get(lcKey),
                                                    lcKey,
                                                    (ArrayList<String>) appBinder.getKeyPath().clone(),
                                                    mapAppData.get(lcKey));
                            tabViewAdapter.addFragment(domainFragment);
                        }
                        if (index == 0) {
                            baseLayoutInfo.setKey(lcKey);
                        }
                        index++;
                    }
                }
            } else if (baseLayoutInfo instanceof DomainLayoutInfo) {
                DomainLayoutInfo domainLayoutInfo = (DomainLayoutInfo) baseLayoutInfo;
                friendlyName = domainLayoutInfo.getAppName();
                currentChildFragment =
                        PegaChildFragment
                                .newInstance(this,
                                        domainLayoutInfo.getAppName(),
                                        appBinder.getParentKey(),
                                        domainLayoutInfo.getKey(),
                                        (ArrayList<String>) appBinder.getKeyPath().clone(),
                                        appData);
                tabViewAdapter.addFragment(currentChildFragment);
            } else if (baseLayoutInfo instanceof AppLayoutInfo) {
                AppLayoutInfo appLayoutInfo = (AppLayoutInfo) baseLayoutInfo;
                friendlyName = appLayoutInfo.getFriendlyName();
                currentChildFragment =
                        PegaChildFragment
                                .newInstance(this,
                                        appLayoutInfo.getFriendlyName(),
                                        appBinder.getParentKey(),
                                        appLayoutInfo.getKey(),
                                        (ArrayList<String>) appBinder.getKeyPath().clone(),
                                        appData);
                tabViewAdapter.addFragment(currentChildFragment);
            } else if (baseLayoutInfo instanceof ServerLayoutInfo) {
                ServerLayoutInfo serverLayoutInfo = (ServerLayoutInfo) baseLayoutInfo;
                friendlyName = serverLayoutInfo.getFriendlyName();
                currentChildFragment =
                        PegaChildFragment
                                .newInstance(this,
                                        serverLayoutInfo.getFriendlyName(),
                                        appBinder.getParentKey(),
                                        serverLayoutInfo.getKey(),
                                        (ArrayList<String>) appBinder.getKeyPath().clone(),
                                        appData);
                tabViewAdapter.addFragment(currentChildFragment);
            }
        }

        onPageChangeListener = createPageChangeListener();

        pegaDataPager.addOnPageChangeListener(onPageChangeListener);

        pegaDataPager.setAdapter(tabViewAdapter);

        if (title == null) {
            if (friendlyName == null) {
                friendlyName = baseLayoutInfo.getFriendlyName(baseLayoutInfo.getKey(), false);
            }
            setPageTitle(friendlyName);
        }

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(true);
                requestRefresh();
            }
        });

        actionBarDrawerToggle = new ActionBarDrawerToggle(this,
                drawerLayout,
                pegaToolbar,
                R.string.open_drawer,
                R.string.close_drawer) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                Log.d(TAG, "Drawer open");
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                Log.d(TAG, "Drawer closed");
            }
        };

        drawerLayout.addDrawerListener(actionBarDrawerToggle);

        setSupportActionBar(pegaToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        createDrawerLayout();

        lastRefreshTime = System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startServerRefreshService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopServerRefreshService();
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

    private void launchPegaServerDataActivity(final PegaServerNetworkBinder appBinder,
                                              BaseLayoutInfo baseLayoutInfo) {
        Intent intent = new Intent(this,
                PegaServerDataActivity.class);
        PegaServerNetworkParcelable restData = new PegaServerNetworkParcelable();
        restData.setBinder(appBinder);
        intent.putExtra(getString(R.string.app_binder_data_bundle_key), restData);

        BaseLayoutInfoBinder baseLayoutInfoBinder = new BaseLayoutInfoBinder();
        baseLayoutInfoBinder.setBaseLayoutInfo(baseLayoutInfo);
        BaseInfoParcelable baseInfoParcelable = new BaseInfoParcelable();
        baseInfoParcelable.setBaseLayoutInfoBinder(baseLayoutInfoBinder);
        intent.putExtra(getString(R.string.info_binder_data_bundle_key), baseInfoParcelable);

        intent.putExtra(getString(R.string.current_page_bundle_key), pageNum + 1);

        startActivityForResult(intent, MainActivity.PEGA_DISPLAY_DATA_REQUEST);
    }

    @Override
    public void setPageTitle(String title) {
        this.title = title;
        this.friendlyName = title;
        pegaToolbar.setSubtitle(title);
    }

    @Override
    public void initPegaDataActivity(String key, PegaServerNetworkBinder childBinder) {
        if (appData instanceof Map<?,?>) {
            Map<String, Object> mapAppData = (Map<String, Object>) appData;
            childBinder.setDrawerData(baseLayoutInfo.getValue(mapAppData, key));
        }
        if (baseLayoutInfo instanceof LifecycleLayoutInfo) {
            DomainLayoutInfo domainLayoutInfo = new DomainLayoutInfo();
            domainLayoutInfo.setFriendlyName(key);
            domainLayoutInfo.setAppName(key);
            domainLayoutInfo.setKey(key);
            launchPegaServerDataActivity(childBinder, domainLayoutInfo);
        } else if (childBinder.getParentKey().equalsIgnoreCase(AppLayoutInfo.APP_JSON_KEY)) {
            AppLayoutInfo appLayoutInfo = new AppLayoutInfo();
            appLayoutInfo.setFriendlyName(baseLayoutInfo.getFriendlyName(key, true));
            appLayoutInfo.setKey(key);
            launchPegaServerDataActivity(childBinder, appLayoutInfo);
        } else if (childBinder.getParentKey().equalsIgnoreCase(ServerLayoutInfo.SERVER_JSON_KEY)) {
            ServerLayoutInfo serverLayoutInfo = new ServerLayoutInfo();
            serverLayoutInfo.setFriendlyName(baseLayoutInfo.getFriendlyName(key, true));
            serverLayoutInfo.setKey(key);
            launchPegaServerDataActivity(childBinder, serverLayoutInfo);
        } else {
            DomainLayoutInfo domainLayoutInfo = new DomainLayoutInfo();
            domainLayoutInfo.setAppName(baseLayoutInfo.getFriendlyName(key, true));
            domainLayoutInfo.setKey(key);
            launchPegaServerDataActivity(childBinder, domainLayoutInfo);
        }
    }

    @Override
    public void requestRefresh() {
        if (binder != null) {
            binder.forceRefresh();
        } else if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
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
        setPageTitle(this.title);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MainActivity.PEGA_DISPLAY_DATA_REQUEST) {
            if (resultCode == MainActivity.RESULT_RELOAD_DATA) {
                setResult(MainActivity.RESULT_RELOAD_DATA);
                finish();
            } else {

            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private ViewPager.OnPageChangeListener createPageChangeListener() {
        return new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position,
                                       float positionOffset,
                                       int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                PegaBaseFragment fragment =
                        (PegaBaseFragment) tabViewAdapter.getItem(position);
                setPageTitle(fragment.getFriendlyName());
                tabIndex = position;

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        };
    }

    private void notifyFragmentsDataChanged(Object appData) {
        lastRefreshTime = binder.getLastRefreshTime();

        for (int i = 0; i < tabViewAdapter.getCount(); i++) {
            PegaBaseFragment pegaBaseFragment = (PegaBaseFragment) tabViewAdapter.getItem(i);
            if (!pegaBaseFragment.notifyAppDataChanged(appData)) {
                PegaServerDataActivity activity = weakActivity.get();
                if (activity != null && !activity.isFinishing()) {
                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    ft.remove(pegaBaseFragment);
                    ft.commitAllowingStateLoss();
                }
            }
        }
        if (tabViewAdapter.getCount() == 0) {
            setResult(MainActivity.RESULT_RELOAD_DATA);
            finish();
        }
        if (currentChildFragment != null && !currentChildFragment.notifyAppDataChanged(appData)) {
            setResult(MainActivity.RESULT_RELOAD_DATA);
            finish();
        }

        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }

        refreshDrawerData(appData);
    }

    private void startServerRefreshService() {
        subscriber = new Action1<Map<String, Object>>() {
            @Override
            public void call(Map<String, Object> appData) {
                notifyFragmentsDataChanged(appData);
            }
        };

        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                binder = ((SubscriberBinder) service);
                binder.addSubscriber(subscriber);
                if (lastRefreshTime != -1L && lastRefreshTime < binder.getLastRefreshTime()) {
                    notifyFragmentsDataChanged(binder.getAppData());
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.e(TAG, "Failed to connect to service " + name);
            }
        };
        Intent refreshServiceIntent = new Intent(this, PegaServerRefreshService.class);
        bindService(refreshServiceIntent, connection, BIND_AUTO_CREATE);
    }

    private void stopServerRefreshService() {
        binder.removeSubscriber(subscriber);
        unbindService(connection);
    }

    private void replaceFragment(String key, Object replacementAppData) {
        baseLayoutInfo.setKey(key);
        if (baseLayoutInfo instanceof LifecycleLayoutInfo) {
            if (appData instanceof Map<?, ?>) {
                if (LifecycleLayoutInfo.LC_MAPPING.containsKey(key)) {
                    pegaDataPager.setCurrentItem(tabViewAdapter.getFragmentPosition(key));
                    baseLayoutInfo.setKey(key);
                }
            }
        } else {
            appData = replacementAppData;
            friendlyName = baseLayoutInfo.getFriendlyName(key, false);
            currentChildFragment =
                    PegaChildFragment
                            .newInstance(this,
                                    friendlyName,
                                    appBinder.getParentKey(),
                                    key,
                                    (ArrayList<String>) appBinder.getKeyPath().clone(),
                                    appData);
            tabViewAdapter = new TabViewAdapter(getSupportFragmentManager());
            tabViewAdapter.addFragment(currentChildFragment);
            pegaDataPager.setAdapter(tabViewAdapter);
            setPageTitle(friendlyName);
            pegaDataPager.forceLayout();
        }
        drawerLayout.closeDrawers();
    }

    private void createDrawerLayout() {
        populateDrawerList();

        leftDrawer.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_selectable_list_item,
                drawerValueList));

        leftDrawer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                baseLayoutInfo.setKey(drawerValueList.get(position).getKey());
                Map<String, Object> drawerMapData = (Map<String, Object>) drawerData;
                String drawerKey = drawerValueList.get(position).getKey();
                replaceFragment(drawerKey, drawerMapData.get(drawerKey));
            }
        });
    }

    private void populateDrawerList() {
        drawerValueList = new ArrayList<>();

        if (drawerData instanceof Map<?,?>) {
            Map<String, Object> mapData = (Map<String, Object>) drawerData;
            for (String key : mapData.keySet()) {
                baseLayoutInfo.setKey(key);
                String friendlyName = baseLayoutInfo.getFriendlyName(key, false);
                if (friendlyName != null) {
                    DrawerListItem drawerListItem = new DrawerListItem();
                    drawerListItem.setKey(key);
                    drawerListItem.setFriendlyName(friendlyName);
                    drawerValueList.add(drawerListItem);
                }
            }
        } else if (drawerData instanceof List<?>) {
            List<String> listData = (List<String>) drawerData;
            for (String value : listData) {
                baseLayoutInfo.setKey(value);
                String friendlyName = baseLayoutInfo.getFriendlyName(value, false);
                if (friendlyName != null) {
                    DrawerListItem drawerListItem = new DrawerListItem();
                    drawerListItem.setKey(value);
                    drawerListItem.setFriendlyName(friendlyName);
                    drawerValueList.add(drawerListItem);
                }
            }
        }
    }

    private void refreshDrawerData(Object appData) {
        if (appBinder != null &&
                appBinder.getKeyPath() != null) {
            Map<String, Object> mapAppData = (Map<String, Object>) appData;
            ArrayList<String> keyPath = appBinder.getKeyPath();
            Map<String, Object> lastChildMap = mapAppData;
            Object lastObject = lastChildMap;
            String parentKey = appBinder.getParentKey();
            for (String path : keyPath) {
                lastChildMap = (Map<String, Object>) lastChildMap.get(path);
                lastObject = lastChildMap;
            }
            if (lastChildMap != null && parentKey != null) {
                lastObject = lastChildMap.get(parentKey);
            }
            if (lastObject != null) {
                drawerData = lastObject;
                if (baseLayoutInfo instanceof DomainLayoutInfo ||
                        baseLayoutInfo instanceof AppLayoutInfo) {
                    this.appData = ((Map<String, Object>) drawerData).get(baseLayoutInfo.getKey());
                } else {
                    this.appData = drawerData;
                }
            }
        }
    }
}
