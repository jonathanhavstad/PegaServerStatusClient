package com.cisco.pegaserverstatusclient.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cisco.pegaserverstatusclient.R;
import com.cisco.pegaserverstatusclient.adapters.PegaServerChildAdapter;
import com.cisco.pegaserverstatusclient.adapters.PegaServerRootAdapter;
import com.cisco.pegaserverstatusclient.binders.PegaServerNetworkBinder;
import com.cisco.pegaserverstatusclient.decoractors.DividerItemDecoration;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by jonathanhavstad on 10/25/16.
 */

public class PegaChildFragment extends PegaBaseFragment {
    @BindView(R.id.rest_data_list)
    RecyclerView restDataList;

    private Object appData;
    private OnSendDataListener onSendDataListener;
    private PegaServerRootAdapter adapter;

    public static PegaChildFragment newInstance(Context context,
                                                 String friendlyName,
                                                 Object fragmentData) {
        Bundle args = new Bundle();
        args.putString(context.getString(R.string.app_domain_data_bundle_key), friendlyName);
        PegaServerNetworkBinder binder = new PegaServerNetworkBinder();
        binder.setAppData(fragmentData);
        args.putBinder(context.getString(R.string.app_binder_data_bundle_key), binder);
        PegaChildFragment fragment = new PegaChildFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            friendlyName = args.getString(getString(R.string.app_domain_data_bundle_key));

            PegaServerNetworkBinder binder = (PegaServerNetworkBinder)
                    args.getBinder(getString(R.string.app_binder_data_bundle_key));
            if (binder != null) {
                appData = binder.getAppData();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(
                R.layout.fragment_pega_server_data,
                container,
                false);

        ButterKnife.bind(this, rootView);

        final PegaServerChildAdapter.OnItemSelectedListener onItemSelectedListener =
                new PegaServerChildAdapter.OnItemSelectedListener() {
                    @Override
                    public void sendData(String key, Object data) {
                        if (onSendDataListener != null) {
                            PegaServerNetworkBinder childBinder = new PegaServerNetworkBinder();
                            childBinder.setAppData(data);
                            onSendDataListener.sendData(key, childBinder);
                        }
                    }
                };

        adapter = new PegaServerRootAdapter(onItemSelectedListener,
                appData,
                new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL_LIST));

        restDataList.setAdapter(adapter);

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnSendDataListener) {
            onSendDataListener = (OnSendDataListener) context;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (onSendDataListener != null) {
            onSendDataListener.setCurrentPageTitle();
        }
    }
}
