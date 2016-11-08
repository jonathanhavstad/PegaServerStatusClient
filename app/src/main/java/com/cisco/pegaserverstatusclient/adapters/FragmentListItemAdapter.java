package com.cisco.pegaserverstatusclient.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cisco.pegaserverstatusclient.R;
import com.cisco.pegaserverstatusclient.data.BaseLayoutInfo;
import com.cisco.pegaserverstatusclient.listeners.OnOpenMenuItemClickListener;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by jonathanhavstad on 11/2/16.
 */

public class FragmentListItemAdapter extends RecyclerView.Adapter<FragmentListItemAdapter.ViewHolder> {
    private BaseLayoutInfo appLayoutInfo;
    private OnOpenMenuItemClickListener onOpenMenuItemClickListener;
    private int size;

    public FragmentListItemAdapter(BaseLayoutInfo appLayoutInfo,
                                   OnOpenMenuItemClickListener onOpenMenuItemClickListener) {
        this.appLayoutInfo = appLayoutInfo;
        this.onOpenMenuItemClickListener = onOpenMenuItemClickListener;
        this.size = appLayoutInfo.size();
        if (this.appLayoutInfo.getChildrenLayouts() == null) {
            this.appLayoutInfo.readFromNetwork(null);
        }
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
        String key = appLayoutInfo.getKeyFromPosition(position);
        holder.landingFragmentListChildItem.setClickable(false);
        holder.landingFragmentListChildItem.setText("");
        holder.landingFragmentListChildItem.setTextAppearance(holder.itemView.getContext(),
                R.style.DefaultItemTextStyle);

        int colIndex = position % headerColumnList.length;

        if (appLayoutInfo.isColBold(colIndex)) {
            holder.landingFragmentListChildItem.setClickable(true);
            holder
                    .landingFragmentListChildItem
                    .setTypeface(holder.landingFragmentListChildItem.getTypeface(), 1);
        }
        int index = appLayoutInfo.getKeyIndex(key);
        if (index != -1) {
            final BaseLayoutInfo childLayoutInfo = appLayoutInfo.getChildLayout(index);
            holder
                    .landingFragmentListChildItem
                    .setText(childLayoutInfo.getKeyedValue(colIndex, key));
            if (appLayoutInfo.isClickable(colIndex)) {
                holder.landingFragmentListChildItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onOpenMenuItemClickListener.open(childLayoutInfo);
                    }
                });
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
