package com.cisco.pegaserverstatusclient.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cisco.pegaserverstatusclient.R;
import com.cisco.pegaserverstatusclient.data.BaseLayoutInfo;
import com.cisco.pegaserverstatusclient.listeners.OnOpenMenuItemClickListener;
import com.cisco.pegaserverstatusclient.listeners.OnSelectMenuItemClickListener;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by jonathanhavstad on 11/3/16.
 */

public class FragmentDrawerListAdapter extends RecyclerView.Adapter<FragmentDrawerListAdapter.ViewHolder> {
    private BaseLayoutInfo appLayoutInfo;
    private OnOpenMenuItemClickListener onOpenMenuItemClickListener;
    private OnSelectMenuItemClickListener onSelectMenuItemClickListener;

    public FragmentDrawerListAdapter(BaseLayoutInfo appLayoutInfo,
                                     OnOpenMenuItemClickListener onOpenMenuItemClickListener,
                                     OnSelectMenuItemClickListener onSelectMenuItemClickListener) {
        this.appLayoutInfo = appLayoutInfo;
        this.onOpenMenuItemClickListener = onOpenMenuItemClickListener;
        this.onSelectMenuItemClickListener = onSelectMenuItemClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_drawer_item,
                parent,
                false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final BaseLayoutInfo childLayout = appLayoutInfo.getChildLayout(position);
        holder.fragmentDrawerItem.setText(childLayout.getFriendlyName());
        holder.fragmentDrawerItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOpenMenuItemClickListener.open(childLayout);
            }
        });
        holder.fragmentDrawerItem.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onSelectMenuItemClickListener.select(childLayout);
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return appLayoutInfo.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View itemView;

        @BindView(R.id.fragment_drawer_item_view)
        TextView fragmentDrawerItem;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            this.itemView = itemView;
        }
    }
}
