package com.cisco.pegaserverstatusclient.adapters;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cisco.pegaserverstatusclient.R;
import com.cisco.pegaserverstatusclient.layouts.BaseLayoutInfo;
import com.cisco.pegaserverstatusclient.listeners.OnOpenMenuItemClickListener;
import com.cisco.pegaserverstatusclient.listeners.OnPositionVisibleListener;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by jonathanhavstad on 11/2/16.
 */

public class FragmentListAdapter extends RecyclerView.Adapter<FragmentListAdapter.ViewHolder> {
    private BaseLayoutInfo appLayoutInfo;
    private OnOpenMenuItemClickListener onOpenMenuItemClickListener;
    private OnPositionVisibleListener onPositionVisibleListener;

    public FragmentListAdapter(BaseLayoutInfo appLayoutInfo,
                               OnOpenMenuItemClickListener onOpenMenuItemClickListener,
                               OnPositionVisibleListener onPositionVisibleListener) {
        init(appLayoutInfo, onOpenMenuItemClickListener, onPositionVisibleListener);
    }

    private void init(BaseLayoutInfo appLayoutInfo,
                      OnOpenMenuItemClickListener onOpenMenuItemClickListener,
                      OnPositionVisibleListener onPositionVisibleListener) {
        this.appLayoutInfo = appLayoutInfo;
        this.onOpenMenuItemClickListener = onOpenMenuItemClickListener;
        this.onPositionVisibleListener = onPositionVisibleListener;
        if (this.appLayoutInfo.getChildrenLayouts() == null) {
            this.appLayoutInfo.readFromInputStream(null);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.fragment_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        BaseLayoutInfo childLayout = appLayoutInfo.getDetailLayout(position);

        String friendlyName = childLayout.getFriendlyName();
        holder.landingFragmentListTitle.setTypeface(holder.landingFragmentListTitle.getTypeface(), 1);
        holder.landingFragmentListTitle.setText(friendlyName);

        holder
                .loadingFragmentListHeaders
                .setLayoutManager(new GridLayoutManager(
                        holder.itemView.getContext(),
                        childLayout.getNumCols()));

        holder
                .landingFragmentListItem
                .setLayoutManager(new GridLayoutManager(
                        holder.itemView.getContext(),
                        childLayout.getNumCols()));

        holder
                .loadingFragmentMainItemList
                .setOrientation(LinearLayout.VERTICAL);

        holder
                .loadingFragmentListHeaders
                .setLayoutParams(getLayoutParams(false));
        holder
                .landingFragmentListItem
                .setLayoutParams(getLayoutParams(false));

        String[] headers = childLayout.getHeaderDescList();
        if (headers != null && headers.length > 0) {
            FragmentListHeaderAdapter headerAdapter = new FragmentListHeaderAdapter(headers);
            holder.loadingFragmentListHeaders.setAdapter(headerAdapter);
        }

        FragmentListItemAdapter adapter = new FragmentListItemAdapter(childLayout,
                onOpenMenuItemClickListener,
                onPositionVisibleListener);
        holder.landingFragmentListItem.swapAdapter(adapter, false);
    }

    @Override
    public int getItemCount() {
        return (appLayoutInfo != null ? appLayoutInfo.size() : 0);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private View itemView;

        @BindView(R.id.landing_fragment_list_title)
        TextView landingFragmentListTitle;

        @BindView(R.id.loading_fragment_main_item_list)
        LinearLayout loadingFragmentMainItemList;

        @BindView(R.id.loading_fragment_list_headers)
        RecyclerView loadingFragmentListHeaders;

        @BindView(R.id.loading_fragment_list_item)
        RecyclerView landingFragmentListItem;

        ViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            ButterKnife.bind(this, itemView);
        }
    }

    private LinearLayout.LayoutParams getLayoutParams(boolean isVertical) {
        LinearLayout.LayoutParams layoutParams = null;
        if (isVertical) {
            layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        } else {
            layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        return layoutParams;
    }
}
