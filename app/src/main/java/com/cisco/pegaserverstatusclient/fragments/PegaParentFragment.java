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
import com.cisco.pegaserverstatusclient.binders.PegaServerNetworkBinder;
import com.cisco.pegaserverstatusclient.listeners.OnItemSelectedListener;
import com.cisco.pegaserverstatusclient.listeners.OnUpdateDataListener;

import java.util.ArrayList;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by jonathanhavstad on 10/25/16.
 */

public class PegaParentFragment extends PegaBaseFragment {
    @BindView(R.id.rest_data_list_embedded)
    RecyclerView restDataList;

    private OnItemSelectedListener onItemSelectedListener;
    private OnUpdateDataListener onUpdateDataListener;
    private PegaServerChildAdapter adapter;
    private PegaServerNetworkBinder binder;

    public static PegaParentFragment newInstance(Context context,
                                                 String friendlyName,
                                                 String key,
                                                 ArrayList<String> keyPath,
                                                 Object fragmentData) {
        Bundle args = new Bundle();
        args.putString(context.getString(R.string.app_domain_data_bundle_key), friendlyName);
        PegaServerNetworkBinder binder = new PegaServerNetworkBinder();
        binder.setAppData(fragmentData);
        keyPath.add(key);
        binder.setKeyPath(keyPath);
        args.putBinder(context.getString(R.string.app_binder_data_bundle_key), binder);
        PegaParentFragment fragment = new PegaParentFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            friendlyName = args.getString(getString(R.string.app_domain_data_bundle_key));

            binder = (PegaServerNetworkBinder) args.getBinder(getString(R.string.app_binder_data_bundle_key));
            if (binder != null) {
                appData = binder.getAppData();
                keyPath = binder.getKeyPath();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.pega_server_root_list_item,
                container,
                false);

        ButterKnife.bind(this, rootView);

        onItemSelectedListener =
                new OnItemSelectedListener() {
                    @Override
                    public void receiveData(String parentKey, String key, Object data) {
                        if (onUpdateDataListener != null) {
                            PegaServerNetworkBinder childBinder = new PegaServerNetworkBinder();
                            childBinder.setAppData(data);
                            childBinder.setKeyPath(keyPath);
                            onUpdateDataListener.sendData(key, childBinder);
                        }
                    }
                };

        adapter = new PegaServerChildAdapter(onItemSelectedListener, appData, null, null);

        restDataList.setAdapter(adapter);

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnUpdateDataListener) {
            onUpdateDataListener = (OnUpdateDataListener) context;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public boolean notifyAppDataChanged(Object appData) {
        boolean validFragment = true;
        if (keyPath != null) {
                Map<String, Object> mapAppData = (Map<String, Object>) appData;
            Object value = null;
            for (int i = 0; i < keyPath.size(); i++) {
                value = mapAppData.get(keyPath.get(i));
                if (value == null) {
                    validFragment = false;
                } else if (value instanceof Map<?, ?>) {
                    mapAppData = (Map<String, Object>) value;
                }
            }

            if (value != null && validFragment) {
                this.appData = value;
            }
        }

        if (this.isVisible()) {
            adapter = new PegaServerChildAdapter(onItemSelectedListener, this.appData, null, null);
            restDataList.swapAdapter(adapter, false);
        }
        return validFragment;
    }
}
