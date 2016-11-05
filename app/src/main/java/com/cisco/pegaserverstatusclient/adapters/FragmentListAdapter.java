package com.cisco.pegaserverstatusclient.adapters;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cisco.pegaserverstatusclient.R;
import com.cisco.pegaserverstatusclient.data.BaseLayoutInfo;
import com.cisco.pegaserverstatusclient.data.KeyMapping;

import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by jonathanhavstad on 11/2/16.
 */

public class FragmentListAdapter extends RecyclerView.Adapter<FragmentListAdapter.ViewHolder> {
    private BaseLayoutInfo appLayoutInfo;
    private Map<String, Object> appData;
    private List<String> orderedKeySet;

    public FragmentListAdapter(BaseLayoutInfo appLayoutInfo) {
        init(appLayoutInfo);
    }

    private void init(BaseLayoutInfo appLayoutInfo) {
        this.appLayoutInfo  = appLayoutInfo;
        this.appData = appLayoutInfo.getAppData();
        this.orderedKeySet = KeyMapping.populateOrderedKeySet(appData);
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
        String key = orderedKeySet.get(position);
        String friendlyName = KeyMapping.getFriendlyName(key);
        if (friendlyName == null) {
            friendlyName = key;
        }
        holder.landringFragmentListTitle.setTypeface(holder.landringFragmentListTitle.getTypeface(), 1);
        holder.landringFragmentListTitle.setText(friendlyName);

        String[] headers = appLayoutInfo.getHeaderDescList();
        FragmentListHeaderAdapter headerAdapter = new FragmentListHeaderAdapter(headers);
        ((GridLayoutManager) holder.loadingFragmentListHeaders.getLayoutManager())
                .setSpanCount(appLayoutInfo.getHeaderColsList().length);
        holder.loadingFragmentListHeaders.setAdapter(headerAdapter);

        BaseLayoutInfo childLayout = appLayoutInfo.getChildLayout(position);
        FragmentListItemAdapter adapter = new FragmentListItemAdapter(childLayout);
        ((GridLayoutManager) holder.landingFragmentListItem.getLayoutManager())
                .setSpanCount(appLayoutInfo.getHeaderColsList().length);
        holder.landingFragmentListItem.setAdapter(adapter);
    }

    @Override
    public int getItemCount() {
        return (orderedKeySet != null ? orderedKeySet.size() : 0);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private View itemView;

        @BindView(R.id.landing_fragment_list_title)
        TextView landringFragmentListTitle;

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
}
