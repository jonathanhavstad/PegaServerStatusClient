package com.ciscozensarpegateam.pegaserverstatusclient.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ciscozensarpegateam.pegaserverstatusclient.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by jonathanhavstad on 10/20/16.
 */

public class PegaServerNetworkAdapter extends
        RecyclerView.Adapter<PegaServerNetworkAdapter.ViewHolder> {
    private Map<String, Object> restData;
    private List<String[]> viewHolderData = new ArrayList<>();
    private OnItemSelectedListener onItemSelectedListener;

    public interface OnItemSelectedListener {
        void sendData(Map<String, Object> data);
        void sendData(List<String> data);
    }

    public PegaServerNetworkAdapter(
            OnItemSelectedListener onItemSelectedListener,
            Map<String, Object> restData) {
        this.onItemSelectedListener = onItemSelectedListener;
        this.restData = restData;
        populateViewHolderDataFromMap();
    }

    public PegaServerNetworkAdapter(
            OnItemSelectedListener onItemSelectedListener,
            List<String> restArrayData) {
        this.onItemSelectedListener = onItemSelectedListener;
        populateViewHolderDataFromList(restArrayData);
    }

    private void populateViewHolderDataFromMap() {
        for (String key : restData.keySet()) {
            if (restData.get(key) instanceof String) {
                String[] value = new String[2];
                value[0] = key;
                value[1] = (String) restData.get(key);
                viewHolderData.add(value);
            } else {
                String[] value = new String[1];
                value[0] = key;
                viewHolderData.add(value);
            }
        }
    }

    private void populateViewHolderDataFromList(List<String> arrayData) {
        for (String datum : arrayData) {
            String[] value = new String[1];
            value[0] = datum;
            viewHolderData.add(value);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.pega_server_network_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String key = null;
        String[] value = viewHolderData.get(position);
        if (value.length == 1) {
            key = value[0];
            holder.objectItemKeyView.setText(value[0]);
        } else if (value.length == 2) {
            key = value[0];
            holder.objectItemKeyView.setText(value[0]);
            holder.objectItemKeyView.setTypeface(holder.objectItemKeyView.getTypeface(), 1);
            holder.objectItemValueView.setText(value[1]);
        }
        if (key != null && restData.containsKey(key)) {
            final Object mapValue = restData.get(key);
            if (mapValue instanceof List<?> || mapValue instanceof Map<?,?>) {
                holder.itemView.setClickable(true);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (onItemSelectedListener != null) {
                            if (mapValue instanceof Map<?,?>) {
                                onItemSelectedListener.sendData((Map<String, Object>) mapValue);
                            } else if (mapValue instanceof List<?>) {
                                onItemSelectedListener.sendData((List<String>) mapValue);
                            }
                        }
                    }
                });
            } else {
                holder.itemView.setClickable(false);
            }
        }
    }

    @Override
    public int getItemCount() {
        return (restData != null ? restData.size() : 0);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View itemView;
        @BindView(R.id.object_item_key)
        TextView objectItemKeyView;
        @BindView(R.id.object_item_value)
        TextView objectItemValueView;
        public ViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            ButterKnife.bind(this, itemView);
        }
    }
}
