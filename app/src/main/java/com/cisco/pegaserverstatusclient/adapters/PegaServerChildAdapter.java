package com.cisco.pegaserverstatusclient.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cisco.pegaserverstatusclient.R;
import com.cisco.pegaserverstatusclient.listeners.OnItemSelectedListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by jonathanhavstad on 10/20/16.
 */

public class PegaServerChildAdapter extends
        RecyclerView.Adapter<PegaServerChildAdapter.ViewHolder> {
    private Object appData;
    private List<String[]> viewHolderData = new ArrayList<>();
    private OnItemSelectedListener onItemSelectedListener;
    private int itemCount;
    private String parentKey;
    private List<String> headers;
    private boolean listOnlySelectableItems;

    public PegaServerChildAdapter(
            OnItemSelectedListener onItemSelectedListener,
            Object appData,
            String parentKey,
            List<String> headers) {
        this.onItemSelectedListener = onItemSelectedListener;
        this.appData = appData;
        this.itemCount = 0;
        this.parentKey = parentKey;
        this.headers = headers;
        populateViewHolderDataFromMap();
    }

    private void populateViewHolderDataFromMap() {
        if (appData instanceof Map<?,?>) {
            Map<String, Object> mapData = (Map<String, Object>) appData;
            itemCount = mapData.size();
            for (String key : mapData.keySet()) {
                if (mapData.get(key) instanceof String && !listOnlySelectableItems) {
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
        } else if (appData instanceof List<?> && !listOnlySelectableItems) {
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
                .inflate(R.layout.pega_server_child_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String key = null;
        String[] value = viewHolderData.get(position);
        if (value.length == 1) {
            key = value[0];
            holder.itemView.setClickable(false);
            holder.objectItemValueView.setText(value[0]);
        } else if (value.length == 2) {
            key = value[0];
            holder.objectItemKeyView.setText(value[0]);
            holder.objectItemKeyView.setTypeface(holder.objectItemKeyView.getTypeface(), 1);
            holder.objectItemValueView.setText(value[1]);
        }

        if (appData instanceof Map<?,?>) {
            Map<String, Object> mapData = (Map<String, Object>) appData;
            if (key != null && mapData.containsKey(key)) {
                final Object mapValue = mapData.get(key);
                if (mapValue instanceof Map<?, ?> || mapValue instanceof List<?>) {
                    holder.itemView.setClickable(true);
                    final String keyValue = key;
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (onItemSelectedListener != null) {
                                onItemSelectedListener.receiveData(parentKey, keyValue, mapValue);
                            }
                        }
                    });

                    holder
                            .objectItemValueView
                            .setTypeface(holder.objectItemValueView.getTypeface(), 1);

                    if (mapValue instanceof Map<?, ?>) {
                        Map<String, Object> childAppMapData = (Map<String, Object>) mapValue;
                        if (hasStatus(childAppMapData)) {
                            holder.mainItemValueStatus.setText(getStatus(childAppMapData));
                        }
                    }
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }

    private boolean hasStatus(Map<String, Object> mapAppData) {
        for (String key : mapAppData.keySet()) {
            if (key.equalsIgnoreCase("STATUS")) {
                return true;
            }
        }
        return false;
    }

    private String getStatus(Map<String, Object> mapAppData) {
        for (String key : mapAppData.keySet()) {
            if (key.equalsIgnoreCase("STATUS")) {
                return (String) mapAppData.get(key);
            }
        }
        return null;
    }

    public boolean isListOnlySelectableItems() {
        return listOnlySelectableItems;
    }

    public void setListOnlySelectableItems(boolean listOnlySelectableItems) {
        this.listOnlySelectableItems = listOnlySelectableItems;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View itemView;
        @BindView(R.id.object_item_key)
        TextView objectItemKeyView;
        @BindView(R.id.object_item_value)
        TextView objectItemValueView;
        @BindView(R.id.main_item_value_status)
        TextView mainItemValueStatus;
        public ViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            ButterKnife.bind(this, itemView);
        }
    }
}
