package com.cisco.pegaserverstatusclient.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.cisco.pegaserverstatusclient.R;
import com.cisco.pegaserverstatusclient.adapters.TabViewAdapter;
import com.cisco.pegaserverstatusclient.background.tasks.PegaServerRestTask;
import com.cisco.pegaserverstatusclient.binders.BaseLayoutInfoBinder;
import com.cisco.pegaserverstatusclient.binders.PegaServerNetworkBinder;
import com.cisco.pegaserverstatusclient.data.AppLayoutInfo;
import com.cisco.pegaserverstatusclient.data.BaseLayoutInfo;
import com.cisco.pegaserverstatusclient.data.DomainLayoutInfo;
import com.cisco.pegaserverstatusclient.data.LifecycleLayoutInfo;
import com.cisco.pegaserverstatusclient.data.ServerLayoutInfo;
import com.cisco.pegaserverstatusclient.fragments.PegaBaseFragment;
import com.cisco.pegaserverstatusclient.fragments.PegaChildFragment;
import com.cisco.pegaserverstatusclient.fragments.PegaParentFragment;
import com.cisco.pegaserverstatusclient.parcelables.BaseInfoParcelable;
import com.cisco.pegaserverstatusclient.parcelables.PegaServerNetworkParcelable;

import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import icepick.Icepick;
import icepick.State;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

public class PegaServerDataActivity extends AppCompatActivity
        implements PegaParentFragment.OnSendDataListener {
    private TabViewAdapter tabViewAdapter;
    private String friendlyName;

    @BindView(R.id.pega_data_pager)
    ViewPager pegaDataPager;

    @BindView(R.id.domain_toolbar)
    Toolbar pegaToolbar;

    private PegaServerRestTask task;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pega_server_data);

        Fabric.with(this, new Crashlytics());

        ButterKnife.bind(this);

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

        PegaServerNetworkBinder appBinder = appParcelable.getBinder();
        Object appData = appBinder.getAppData();

        task = new PegaServerRestTask(this, null);

        tabViewAdapter = new TabViewAdapter(getSupportFragmentManager());

        if (layoutParcelable != null) {
            BaseLayoutInfoBinder baseLayoutInfoBinder = layoutParcelable.getBaseLayoutInfoBinder();
            BaseLayoutInfo baseLayoutInfo = baseLayoutInfoBinder.getBaseLayoutInfo();
            if (pageNum == 1) {
                if (appData instanceof Map<?, ?>) {
                    Map<String, Object> mapAppData = (Map<String, Object>) appData;
                    for (String lcKey : LifecycleLayoutInfo.LC_KEY_ORDER) {
                        if (mapAppData.containsKey(lcKey)) {
                            PegaParentFragment domainFragment =
                                    PegaParentFragment
                                            .newInstance(this,
                                                    LifecycleLayoutInfo.LC_MAPPING.get(lcKey),
                                                    mapAppData.get(lcKey));
                            tabViewAdapter.addFragment(domainFragment);
                        }
                    }
                }

            } else if (baseLayoutInfo instanceof DomainLayoutInfo) {
                DomainLayoutInfo domainLayoutInfo = (DomainLayoutInfo) baseLayoutInfo;
                friendlyName = domainLayoutInfo.getAppName();
                PegaChildFragment domainFragment =
                        PegaChildFragment
                                .newInstance(this,
                                        domainLayoutInfo.getAppName(),
                                        appData);
                tabViewAdapter.addFragment(domainFragment);
            } else if (baseLayoutInfo instanceof AppLayoutInfo) {
                AppLayoutInfo appLayoutInfo = (AppLayoutInfo) baseLayoutInfo;
                friendlyName = appLayoutInfo.getFriendlyName();
                PegaChildFragment appFragment =
                        PegaChildFragment
                                .newInstance(this,
                                        appLayoutInfo.getFriendlyName(),
                                        appData);
                tabViewAdapter.addFragment(appFragment);
            } else if (baseLayoutInfo instanceof ServerLayoutInfo) {
                ServerLayoutInfo serverLayoutInfo = (ServerLayoutInfo) baseLayoutInfo;
                friendlyName = serverLayoutInfo.getFriendlyName();
                PegaChildFragment serverFragment =
                        PegaChildFragment
                                .newInstance(this,
                                        serverLayoutInfo.getFriendlyName(),
                                        appData);
                tabViewAdapter.addFragment(serverFragment);
            }
        }

        pegaDataPager.setAdapter(tabViewAdapter);

        pegaDataPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                PegaBaseFragment fragment = (PegaBaseFragment) tabViewAdapter.getItem(position);
                setPageTitle(fragment.getFriendlyName());
                tabIndex = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void launchPegaServerDataActivity(final PegaServerNetworkBinder appBinder,
                                                          BaseLayoutInfo baseLayoutInfo) {
        Intent intent = new Intent(
                this,
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

        startActivity(intent);
    }

    @Override
    public void setCurrentPageTitle() {
        if (pegaDataPager != null && tabViewAdapter != null) {
            PegaBaseFragment fragment =
                    (PegaBaseFragment) tabViewAdapter.getItem(pegaDataPager.getCurrentItem());
            setPageTitle(fragment.getFriendlyName());
        }
    }

    @Override
    public void setPageTitle(String title) {
        this.title = title;
        pegaToolbar.setTitle(title);
        tabViewAdapter.notifyDataSetChanged();
        pegaToolbar.requestLayout();
    }

    @Override
    public void sendData(String key, PegaServerNetworkBinder childBinder) {
        if (pageNum == 1) {
            DomainLayoutInfo domainLayoutInfo = new DomainLayoutInfo();
            domainLayoutInfo.setAppName(key);
            launchPegaServerDataActivity(childBinder, domainLayoutInfo);
        } else if (key.equalsIgnoreCase(AppLayoutInfo.APP_JSON_KEY)) {
            AppLayoutInfo appLayoutInfo = new AppLayoutInfo();
            appLayoutInfo.setFriendlyName(friendlyName + " " + key);
            launchPegaServerDataActivity(childBinder, appLayoutInfo);
        } else if (key.equalsIgnoreCase(ServerLayoutInfo.SERVER_JSON_KEY)) {
            ServerLayoutInfo serverLayoutInfo = new ServerLayoutInfo();
            serverLayoutInfo.setFriendlyName(friendlyName + " " + key);
            launchPegaServerDataActivity(childBinder, serverLayoutInfo);
        } else {
            DomainLayoutInfo domainLayoutInfo = new DomainLayoutInfo();
            domainLayoutInfo.setAppName(friendlyName + " " + key);
            launchPegaServerDataActivity(childBinder, domainLayoutInfo);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Icepick.saveInstanceState(this, outState);
        pegaDataPager.clearOnPageChangeListeners();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Icepick.restoreInstanceState(this, savedInstanceState);
        setPageTitle(this.title);
    }
}
