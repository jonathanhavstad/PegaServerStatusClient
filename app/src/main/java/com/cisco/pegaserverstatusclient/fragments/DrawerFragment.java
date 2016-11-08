package com.cisco.pegaserverstatusclient.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cisco.pegaserverstatusclient.R;
import com.cisco.pegaserverstatusclient.adapters.FragmentDrawerListAdapter;
import com.cisco.pegaserverstatusclient.binders.LayoutInfoBinder;
import com.cisco.pegaserverstatusclient.data.BaseLayoutInfo;
import com.cisco.pegaserverstatusclient.listeners.OnBackPressedClickListener;
import com.cisco.pegaserverstatusclient.listeners.OnOpenMenuItemClickListener;
import com.cisco.pegaserverstatusclient.listeners.OnSelectMenuItemClickListener;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by jonathanhavstad on 11/3/16.
 */

public class DrawerFragment extends Fragment {
    private BaseLayoutInfo appLayoutInfo;

    @BindView(R.id.drawer_toolbar)
    Toolbar drawerToolbar;

    @BindView(R.id.drawer_list)
    RecyclerView drawerList;

    private OnOpenMenuItemClickListener onOpenMenuItemClickListener;
    private OnSelectMenuItemClickListener onSelectMenuItemClickListener;
    private OnBackPressedClickListener onBackPressedClickListener;

    public static DrawerFragment newInstance(Context context,
                                             BaseLayoutInfo appLayoutInfo) {
        DrawerFragment fragment = new DrawerFragment();

        Bundle args = new Bundle();

        LayoutInfoBinder layoutInfoBinder = new LayoutInfoBinder();
        layoutInfoBinder.setBaseLayoutInfo(appLayoutInfo);
        args.putBinder(context.getString(R.string.app_layout_info_bundle_key), layoutInfoBinder);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnOpenMenuItemClickListener) {
            this.onOpenMenuItemClickListener = (OnOpenMenuItemClickListener) context;
        }
        if (context instanceof OnSelectMenuItemClickListener) {
            this.onSelectMenuItemClickListener = (OnSelectMenuItemClickListener) context;
        }
        if (context instanceof OnBackPressedClickListener) {
            this.onBackPressedClickListener = (OnBackPressedClickListener) context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            LayoutInfoBinder layoutInfoBinder = (LayoutInfoBinder)
                    args.getBinder(getString(R.string.app_layout_info_bundle_key));
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
        View rootView = inflater.inflate(R.layout.fragment_drawer, container, false);

        ButterKnife.bind(this, rootView);

        drawerToolbar.setTitle(appLayoutInfo.getFriendlyName());

        if (appLayoutInfo.getParentLayout() != null) {
            drawerToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
            drawerToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressedClickListener.backPressed(appLayoutInfo);
                }
            });
        }

        FragmentDrawerListAdapter adapter = new FragmentDrawerListAdapter(appLayoutInfo,
                onOpenMenuItemClickListener,
                onSelectMenuItemClickListener);
        drawerList.setAdapter(adapter);

        return rootView;
    }
}
