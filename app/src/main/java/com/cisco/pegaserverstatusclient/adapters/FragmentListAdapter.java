package com.cisco.pegaserverstatusclient.adapters;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cisco.pegaserverstatusclient.R;
import com.cisco.pegaserverstatusclient.data.BaseLayoutInfo;
import com.cisco.pegaserverstatusclient.listeners.OnOpenMenuItemClickListener;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by jonathanhavstad on 11/2/16.
 */

public class FragmentListAdapter extends RecyclerView.Adapter<FragmentListAdapter.ViewHolder> {
    private BaseLayoutInfo appLayoutInfo;
    private OnOpenMenuItemClickListener onOpenMenuItemClickListener;

    public FragmentListAdapter(BaseLayoutInfo appLayoutInfo,
                               OnOpenMenuItemClickListener onOpenMenuItemClickListener) {
        init(appLayoutInfo, onOpenMenuItemClickListener);
    }

    private void init(BaseLayoutInfo appLayoutInfo,
                      OnOpenMenuItemClickListener onOpenMenuItemClickListener) {
        this.appLayoutInfo = appLayoutInfo;
        this.onOpenMenuItemClickListener = onOpenMenuItemClickListener;
        if (this.appLayoutInfo.getChildrenLayouts() == null) {
            this.appLayoutInfo.readFromNetwork(null);
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

        if (appLayoutInfo.isGridLayout()) {
            holder
                    .loadingFragmentListHeaders
                    .setLayoutManager(new StaggeredGridLayoutManager(
                            appLayoutInfo.getHeaderColsList().length,
                            StaggeredGridLayoutManager.VERTICAL));

            holder
                    .landingFragmentListItem
                    .setLayoutManager(new StaggeredGridLayoutManager(
                            appLayoutInfo.getHeaderColsList().length,
                            StaggeredGridLayoutManager.VERTICAL));

            holder
                    .loadingFragmentMainItemList
                    .setOrientation(LinearLayout.VERTICAL);
            holder
                    .loadingFragmentListHeaders
                    .setLayoutParams(getColumnListLayoutParams(false));
            holder
                    .landingFragmentListItem
                    .setLayoutParams(getColumnListLayoutParams(false));
        } else {
            if (appLayoutInfo.isVerticalLayout()) {
                holder
                        .loadingFragmentListHeaders
                        .setLayoutManager(new LinearLayoutManager(
                                holder.itemView.getContext(),
                                LinearLayoutManager.VERTICAL,
                                false));
                holder
                        .landingFragmentListItem
                        .setLayoutManager(new LinearLayoutManager(
                                holder.itemView.getContext(),
                                LinearLayoutManager.VERTICAL,
                                false));
                holder
                        .loadingFragmentMainItemList
                        .setOrientation(LinearLayout.HORIZONTAL);
                holder
                        .loadingFragmentListHeaders
                        .setLayoutParams(getColumnListLayoutParams(true));
                holder
                        .landingFragmentListItem
                        .setLayoutParams(getColumnListLayoutParams(true));
            } else if (appLayoutInfo.isHorizontalLayout()) {
                holder
                        .loadingFragmentListHeaders
                        .setLayoutManager(new LinearLayoutManager(
                                holder.itemView.getContext(),
                                LinearLayoutManager.HORIZONTAL,
                                false));
                holder
                        .landingFragmentListItem
                        .setLayoutManager(new LinearLayoutManager(
                                holder.itemView.getContext(),
                                LinearLayoutManager.HORIZONTAL,
                                false));
                holder
                        .loadingFragmentMainItemList
                        .setOrientation(LinearLayout.VERTICAL);
                holder
                        .loadingFragmentListHeaders
                        .setLayoutParams(getColumnListLayoutParams(false));
                holder
                        .landingFragmentListItem
                        .setLayoutParams(getColumnListLayoutParams(false));
            }
        }

        String[] headers = appLayoutInfo.getHeaderDescList();
        if (headers != null && headers.length > 0) {
            FragmentListHeaderAdapter headerAdapter = new FragmentListHeaderAdapter(headers);
            holder.loadingFragmentListHeaders.setAdapter(headerAdapter);
        }

        FragmentListItemAdapter adapter = new FragmentListItemAdapter(childLayout,
                onOpenMenuItemClickListener);
        holder.landingFragmentListItem.setAdapter(adapter);
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

        @BindView(R.id.landing_fragment_list_item)
        RecyclerView landingFragmentListItem;

        ViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            ButterKnife.bind(this, itemView);
        }
    }

    private LinearLayout.LayoutParams getColumnListLayoutParams(boolean isVertical) {
        LinearLayout.LayoutParams layoutParams = null;
        if (isVertical) {
            layoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        } else {
            layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        }
        return layoutParams;
    }
}
