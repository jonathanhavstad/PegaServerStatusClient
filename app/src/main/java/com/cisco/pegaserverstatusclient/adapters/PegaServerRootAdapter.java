package com.cisco.pegaserverstatusclient.adapters;

import android.graphics.Paint;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cisco.pegaserverstatusclient.R;
import com.cisco.pegaserverstatusclient.decoractors.DividerItemDecoration;
import com.cisco.pegaserverstatusclient.listeners.OnItemSelectedListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * Created by jonathanhavstad on 10/24/16.
 */

public class PegaServerRootAdapter extends RecyclerView.Adapter<PegaServerRootAdapter.ViewHolder> {
    private Object appData;
    private List<String[]> viewHolderData = new ArrayList<>();
    private OnItemSelectedListener onItemSelectedListener;
    private int itemCount;
    private DividerItemDecoration dividerItemDecoration;

    public PegaServerRootAdapter(OnItemSelectedListener onItemSelectedListener,
                                 Object appData,
                                 DividerItemDecoration dividerItemDecoration) {
        this.onItemSelectedListener = onItemSelectedListener;
        this.appData = appData;
        this.itemCount = 0;
        this.dividerItemDecoration = dividerItemDecoration;
        populateViewHolderDataFromMap();
    }

    private void populateViewHolderDataFromMap() {
        if (appData instanceof Map<?,?>) {
            Map<String, Object> mapData = (Map<String, Object>) appData;
            itemCount = mapData.size();
            for (String key : mapData.keySet()) {
                if (mapData.get(key) instanceof String) {
                    String[] value = new String[2];
                    value[0] = key;
                    value[1] = (String) mapData.get(key);
                    viewHolderData.add(value);
                } else {
                    String[] value = new String[1];
                    value[0] = key;
                    viewHolderData.add(value);
                }
            }
        } else if (appData instanceof List<?>) {
            List<String> listData = (List<String>) appData;
            itemCount = listData.size();
            for (String datum : listData) {
                String[] value = new String[1];
                value[0] = datum;
                viewHolderData.add(value);
            }
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.pega_server_root_list_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (appData instanceof Map<?,?>) {
            Map<String, Object> mapAppData = (Map<String, Object>) appData;
            String[] appValues = viewHolderData.get(position);
            Object mapValue = mapAppData.get(appValues[0]);
            PegaServerChildAdapter adapter =
                    new PegaServerChildAdapter(onItemSelectedListener,
                            mapValue,
                            appValues[0],
                            null);
            holder.recyclerView.setAdapter(adapter);
        }
        String[] viewHolderValue = viewHolderData.get(position);
        if (viewHolderValue.length > 1) {
            holder.mainItemKey.setText(viewHolderValue[0]);
            holder.mainItemValue.setText(viewHolderValue[1]);
        } else {
            holder.mainItemKey.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
            holder.mainItemKey.setText(viewHolderValue[0]);
            LinearLayout.LayoutParams layoutParams =
                    new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            holder.mainItemKey.setGravity(Gravity.CENTER_HORIZONTAL);
            holder.mainItemKey.setLayoutParams(layoutParams);
        }
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View itemView;
        @BindView(R.id.main_item_key)
        TextView mainItemKey;
        @BindView(R.id.main_item_value)
        TextView mainItemValue;
        @BindView(R.id.rest_data_list_embedded)
        RecyclerView recyclerView;

        public ViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            ButterKnife.bind(this, itemView);
        }
    }
}
