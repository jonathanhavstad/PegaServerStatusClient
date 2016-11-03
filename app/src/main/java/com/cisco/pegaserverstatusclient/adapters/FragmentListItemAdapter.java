package com.cisco.pegaserverstatusclient.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cisco.pegaserverstatusclient.R;
import com.cisco.pegaserverstatusclient.data.AppLayoutInfo;
import com.cisco.pegaserverstatusclient.data.KeyMapping;

import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by jonathanhavstad on 11/2/16.
 */

public class FragmentListItemAdapter extends RecyclerView.Adapter<FragmentListItemAdapter.ViewHolder> {
    private AppLayoutInfo appLayoutInfo;
    private Map<String, Object> appData;
    private List<String> orderedKeySet;
    private int size;

    public FragmentListItemAdapter(AppLayoutInfo appLayoutInfo, Map<String, Object> appData) {
        this.appLayoutInfo = appLayoutInfo;
        this.appData = appData;
        this.orderedKeySet = KeyMapping.populateOrderedKeySet(appData);
        this.size = this.orderedKeySet.size() * this.appLayoutInfo.getHeaderDescList().length;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.fragment_child_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String[] headerColumnList = appLayoutInfo.getHeaderColsList();
        String key = orderedKeySet.get(position % orderedKeySet.size());
        Map<String, Object> childAppData = (Map<String, Object>) appData.get(key);
        int colIndex = position % headerColumnList.length;
        if (colIndex == 0) {
            holder.landingFragmentListChildItem.setClickable(true);
            holder.landingFragmentListChildItem.setTypeface(holder.landingFragmentListChildItem.getTypeface(), 1);
            holder.landingFragmentListChildItem.setText(key);
        } else {
            String header = headerColumnList[colIndex];
            for (String headerKey : childAppData.keySet()) {
                if (headerKey.equalsIgnoreCase(header)) {
                    holder.landingFragmentListChildItem.setText(childAppData.get(headerKey).toString());
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return size;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private View itemView;

        @BindView(R.id.landing_fragment_list_child_item)
        TextView landingFragmentListChildItem;

        ViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            ButterKnife.bind(this, itemView);
        }
    }
}
