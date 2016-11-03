package com.cisco.pegaserverstatusclient.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cisco.pegaserverstatusclient.R;
import com.cisco.pegaserverstatusclient.adapters.FragmentListAdapter;
import com.cisco.pegaserverstatusclient.binders.BaseLayoutInfoBinder;
import com.cisco.pegaserverstatusclient.binders.PegaServerNetworkBinder;
import com.cisco.pegaserverstatusclient.data.AppLayoutInfo;
import com.cisco.pegaserverstatusclient.data.BaseLayoutInfo;
import com.cisco.pegaserverstatusclient.decoractors.DividerItemDecoration;

import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by jonathanhavstad on 11/2/16.
 */

public class AppFragment extends Fragment {
    private AppLayoutInfo appLayoutInfo;
    private Map<String, Object> appData;
    private FragmentListAdapter adapter;

    @BindView(R.id.landing_fragment_list)
    RecyclerView landingFragmentList;

    public static AppFragment newInstance(Context context,
                                          AppLayoutInfo appLayoutInfo,
                                          Map<String, Object> fragmentData) {
        AppFragment appFragment = new AppFragment();

        Bundle args = new Bundle();

        BaseLayoutInfoBinder baseLayoutInfoBinder = new BaseLayoutInfoBinder();
        baseLayoutInfoBinder.setBaseLayoutInfo(appLayoutInfo);
        args.putBinder(context.getString(R.string.app_layout_info_bundle_key), baseLayoutInfoBinder);

        PegaServerNetworkBinder pegaServerNetworkBinder = new PegaServerNetworkBinder();
        pegaServerNetworkBinder.setAppData(fragmentData);
        args.putBinder(context.getString(R.string.app_binder_data_bundle_key), pegaServerNetworkBinder);

        appFragment.setArguments(args);

        return appFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            BaseLayoutInfoBinder baseLayoutInfoBinder =
                    (BaseLayoutInfoBinder) args.getBinder(getContext().getString(R.string.app_layout_info_bundle_key));
            if (baseLayoutInfoBinder != null) {
                appLayoutInfo = (AppLayoutInfo) baseLayoutInfoBinder.getBaseLayoutInfo();
            }

            PegaServerNetworkBinder binder =
                    (PegaServerNetworkBinder) args.getBinder(getString(R.string.app_binder_data_bundle_key));
            if (binder != null) {
                appData = (Map<String, Object>) binder.getAppData();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_list, container, false);

        ButterKnife.bind(this, rootView);

        adapter = new FragmentListAdapter(appLayoutInfo, appData);
        landingFragmentList.setAdapter(adapter);
        landingFragmentList.addItemDecoration(new DividerItemDecoration(getContext(),
                DividerItemDecoration.HORIZONTAL_LIST));

        return rootView;
    }

    public void updateAppData(AppLayoutInfo appLayoutInfo, Map<String, Object> appData) {
        adapter.updateAppData(appLayoutInfo, appData);
    }
}
