package com.cisco.pegaserverstatusclient.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cisco.pegaserverstatusclient.R;
import com.cisco.pegaserverstatusclient.adapters.FragmentListAdapter;
import com.cisco.pegaserverstatusclient.binders.LayoutInfoBinder;
import com.cisco.pegaserverstatusclient.layouts.BaseLayoutInfo;
import com.cisco.pegaserverstatusclient.decoractors.DividerItemDecoration;
import com.cisco.pegaserverstatusclient.listeners.OnOpenMenuItemClickListener;
import com.cisco.pegaserverstatusclient.listeners.OnPositionVisibleListener;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by jonathanhavstad on 11/2/16.
 */

public class LayoutFragment extends Fragment {
    private BaseLayoutInfo appLayoutInfo;
    private OnOpenMenuItemClickListener onOpenMenuItemClickListener;
    private OnPositionVisibleListener onPositionVisibleListener;

    @BindView(R.id.landing_fragment_list)
    RecyclerView landingFragmentList;

    public static LayoutFragment newInstance(Context context,
                                             BaseLayoutInfo appLayoutInfo) {
        LayoutFragment layoutFragment = new LayoutFragment();

        Bundle args = new Bundle();

        LayoutInfoBinder layoutInfoBinder = new LayoutInfoBinder();
        layoutInfoBinder.setBaseLayoutInfo(appLayoutInfo);
        args.putBinder(context.getString(R.string.app_layout_info_bundle_key), layoutInfoBinder);

        layoutFragment.setArguments(args);

        return layoutFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnOpenMenuItemClickListener) {
            this.onOpenMenuItemClickListener = (OnOpenMenuItemClickListener) context;
        }
        if (context instanceof OnPositionVisibleListener) {
            this.onPositionVisibleListener = (OnPositionVisibleListener) context;
        }
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

        if (appLayoutInfo.isVerticalLayout()) {
            landingFragmentList
                    .setLayoutManager(new LinearLayoutManager(
                            getContext(),
                            LinearLayoutManager.VERTICAL,
                            false));
        } else {
            landingFragmentList
                    .setLayoutManager(new LinearLayoutManager(
                            getContext(),
                            LinearLayoutManager.HORIZONTAL,
                            false));
        }

        FragmentListAdapter adapter = new FragmentListAdapter(appLayoutInfo,
                onOpenMenuItemClickListener,
                onPositionVisibleListener);
        landingFragmentList.setAdapter(adapter);
        landingFragmentList.addItemDecoration(new DividerItemDecoration(getContext(),
                DividerItemDecoration.HORIZONTAL_LIST));

        return rootView;
    }

    public void updateAppData(BaseLayoutInfo appLayoutInfo) {
        FragmentListAdapter adapter = new FragmentListAdapter(appLayoutInfo,
                onOpenMenuItemClickListener,
                onPositionVisibleListener);
        landingFragmentList.swapAdapter(adapter, false);
    }

    public BaseLayoutInfo getLayoutInfo() {
        return appLayoutInfo;
    }
}
