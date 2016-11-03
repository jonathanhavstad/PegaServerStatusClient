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
import com.cisco.pegaserverstatusclient.data.DomainAppLayoutInfo;
import com.cisco.pegaserverstatusclient.data.BaseLayoutInfo;
import com.cisco.pegaserverstatusclient.data.DomainLayoutInfo;
import com.cisco.pegaserverstatusclient.data.DrawerListItem;
import com.cisco.pegaserverstatusclient.fragments.PegaBaseFragment;
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
        init(savedInstanceState);
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

        BaseLayoutInfo childLayoutInfo = new BaseLayoutInfo.Builder()
                .layout(baseLayoutInfo)
                .parentKey(childBinder.getParentKey())
                .friendlyName(baseLayoutInfo.getFriendlyName(key, true))
                .key(key)
                .build();

        launchPegaServerDataActivity(childBinder, childLayoutInfo);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PegaServerAppActivity.PEGA_DISPLAY_DATA_REQUEST) {
            if (resultCode == PegaServerAppActivity.RESULT_RELOAD_DATA) {
                setResult(PegaServerAppActivity.RESULT_RELOAD_DATA);
                finish();
            } else {

            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
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

        startActivityForResult(intent, PegaServerAppActivity.PEGA_DISPLAY_DATA_REQUEST);
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
            setResult(PegaServerAppActivity.RESULT_RELOAD_DATA);
            finish();
        }
        if (currentChildFragment != null && !currentChildFragment.notifyAppDataChanged(appData)) {
            setResult(PegaServerAppActivity.RESULT_RELOAD_DATA);
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

    private void replaceFragment(final String key, final Object replacementAppData) {
        baseLayoutInfo.setKey(key);
        friendlyName = baseLayoutInfo.getFriendlyName(key, false);

        baseLayoutInfo.replaceLayoutToView(this,
                appBinder.getParentKey(),
                appBinder.getKeyPath(),
                replacementAppData,
                new BaseLayoutInfo.ReplaceLayoutViewAdapter() {
                    @Override
                    public void replace(boolean recreateView, PegaBaseFragment newFragment) {
                        if (recreateView) {
                            currentChildFragment = newFragment;
                            tabViewAdapter = new TabViewAdapter(getSupportFragmentManager());
                            tabViewAdapter.addFragment(currentChildFragment);
                            pegaDataPager.setAdapter(tabViewAdapter);
                            setPageTitle(friendlyName);
                            pegaDataPager.forceLayout();
                            appData = replacementAppData;
                        } else {
                            pegaDataPager.setCurrentItem(tabViewAdapter.getFragmentPosition(key));
                        }
                    }
                });

        drawerLayout.closeDrawers();
    }

    private void init(Bundle savedInstanceState) {
        weakActivity = new WeakReference<>(this);
        readIntent(savedInstanceState);
        initTabAdapter();
        initTitle();
        initRefreshLayout();
        initToolbar();
        initDrawerLayout();
        lastRefreshTime = System.currentTimeMillis();
    }

    private void readIntent(Bundle savedInstanceState) {
        Intent intent = getIntent();
        if (intent != null) {
            appParcelable = intent.getParcelableExtra(getString(R.string.app_binder_data_bundle_key));
            layoutParcelable =
                    intent.getParcelableExtra(getString(R.string.info_binder_data_bundle_key));
            pageNum = intent.getIntExtra(getString(R.string.current_page_bundle_key), 1);
            tabIndex= 0;

            appBinder = appParcelable.getBinder();
            appData = appBinder.getAppData();
            drawerData = appBinder.getDrawerData();
        } else {
            Icepick.restoreInstanceState(this, savedInstanceState);
        }
    }

    private void initTabAdapter() {
        tabViewAdapter = new TabViewAdapter(getSupportFragmentManager());

        if (layoutParcelable != null) {
            BaseLayoutInfoBinder baseLayoutInfoBinder =
                    layoutParcelable.getBaseLayoutInfoBinder();
            baseLayoutInfo = baseLayoutInfoBinder.getBaseLayoutInfo();
            friendlyName = baseLayoutInfo.getFriendlyName();
            currentChildFragment = baseLayoutInfo.addLayoutToView(this,
                    appBinder.getParentKey(),
                    appBinder.getKeyPath(),
                    appData,
                    new BaseLayoutInfo.AddLayoutViewAdapter() {
                        @Override
                        public void add(PegaBaseFragment fragment) {
                            tabViewAdapter.addFragment(fragment);
                        }
                    });
        }

        onPageChangeListener = createPageChangeListener();

        pegaDataPager.addOnPageChangeListener(onPageChangeListener);

        pegaDataPager.setAdapter(tabViewAdapter);
    }

    private void initTitle() {
        if (title == null) {
            if (friendlyName == null) {
                friendlyName = baseLayoutInfo.getFriendlyName(baseLayoutInfo.getKey(), false);
            }
            setPageTitle(friendlyName);
        }
    }

    private void initRefreshLayout() {
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(true);
                requestRefresh();
            }
        });
    }

    private void initDrawerLayout() {
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

    private void initToolbar() {
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
                        baseLayoutInfo instanceof DomainAppLayoutInfo) {
                    this.appData = ((Map<String, Object>) drawerData).get(baseLayoutInfo.getKey());
                } else {
                    this.appData = drawerData;
                }
            }
        }
    }
}
