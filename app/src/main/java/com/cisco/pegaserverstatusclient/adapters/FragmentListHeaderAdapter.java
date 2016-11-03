package com.cisco.pegaserverstatusclient.adapters;

import android.graphics.Paint;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cisco.pegaserverstatusclient.R;

import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by jonathanhavstad on 11/2/16.
 */

public class FragmentListHeaderAdapter extends RecyclerView.Adapter<FragmentListHeaderAdapter.ViewHolder> {
    private String[] headers;

    public FragmentListHeaderAdapter(String[] headers) {
        this.headers = headers;
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
        holder.landingFragmentListChildItem.setTypeface(holder.landingFragmentListChildItem.getTypeface(), 1);
        holder.landingFragmentListChildItem.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
        holder.landingFragmentListChildItem.setText(headers[position]);
    }

    @Override
    public int getItemCount() {
        return (headers != null ? headers.length : 0);
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
