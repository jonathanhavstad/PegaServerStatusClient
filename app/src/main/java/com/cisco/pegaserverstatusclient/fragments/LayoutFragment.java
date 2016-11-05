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
import com.cisco.pegaserverstatusclient.binders.LayoutInfoBinder;
import com.cisco.pegaserverstatusclient.binders.ServerDataBinder;
import com.cisco.pegaserverstatusclient.data.AppLayoutInfo;
import com.cisco.pegaserverstatusclient.data.BaseLayoutInfo;
import com.cisco.pegaserverstatusclient.decoractors.DividerItemDecoration;

import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by jonathanhavstad on 11/2/16.
 */

public class LayoutFragment extends Fragment {
    private BaseLayoutInfo appLayoutInfo;
    private Map<String, Object> appData;
    private boolean isChildLayout;

    @BindView(R.id.landing_fragment_list)
    RecyclerView landingFragmentList;

    public static LayoutFragment newInstance(Context context,
                                             BaseLayoutInfo appLayoutInfo,
                                             Map<String, Object> fragmentData,
                                             boolean isChildLayout) {
        LayoutFragment layoutFragment = new LayoutFragment();

        Bundle args = new Bundle();

        LayoutInfoBinder layoutInfoBinder = new LayoutInfoBinder();
        layoutInfoBinder.setBaseLayoutInfo(appLayoutInfo);
        args.putBinder(context.getString(R.string.app_layout_info_bundle_key), layoutInfoBinder);

        ServerDataBinder serverDataBinder = new ServerDataBinder();
        serverDataBinder.setAppData(fragmentData);
        args.putBinder(context.getString(R.string.app_binder_data_bundle_key), serverDataBinder);

        args.putBoolean(context.getString(R.string.child_layout_bundle_key), isChildLayout);

        layoutFragment.setArguments(args);

        return layoutFragment;
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
            LayoutInfoBinder layoutInfoBinder =
                    (LayoutInfoBinder) args.getBinder(getContext().getString(R.string.app_layout_info_bundle_key));
            if (layoutInfoBinder != null) {
                appLayoutInfo = layoutInfoBinder.getBaseLayoutInfo();
                appLayoutInfo.readFromNetwork(null);
            }

            ServerDataBinder binder =
                    (ServerDataBinder) args.getBinder(getString(R.string.app_binder_data_bundle_key));
            if (binder != null) {
                appData = (Map<String, Object>) binder.getAppData();
            }

            isChildLayout = args.getBoolean(getString(R.string.child_layout_bundle_key));
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = null;

        if (isChildLayout) {

        } else {
            rootView = inflater.inflate(R.layout.fragment_list, container, false);

            ButterKnife.bind(this, rootView);
            FragmentListAdapter adapter = new FragmentListAdapter(appLayoutInfo);
            landingFragmentList.setAdapter(adapter);
            landingFragmentList.addItemDecoration(new DividerItemDecoration(getContext(),
                    DividerItemDecoration.HORIZONTAL_LIST));
        }

        return rootView;
    }

    public void updateAppData(BaseLayoutInfo appLayoutInfo, Map<String, Object> appData) {
        FragmentListAdapter adapter = new FragmentListAdapter(appLayoutInfo);
        landingFragmentList.swapAdapter(adapter, false);
    }
}
