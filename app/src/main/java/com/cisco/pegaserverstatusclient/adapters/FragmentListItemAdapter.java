package com.cisco.pegaserverstatusclient.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cisco.pegaserverstatusclient.R;
import com.cisco.pegaserverstatusclient.layouts.BaseLayoutInfo;
import com.cisco.pegaserverstatusclient.listeners.OnPositionVisibleListener;
import com.cisco.pegaserverstatusclient.listeners.OnOpenMenuItemClickListener;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by jonathanhavstad on 11/2/16.
 */

public class FragmentListItemAdapter extends RecyclerView.Adapter<FragmentListItemAdapter.ViewHolder> {
    private BaseLayoutInfo appLayoutInfo;
    private OnOpenMenuItemClickListener onOpenMenuItemClickListener;
    private OnPositionVisibleListener onPositionVisibleListener;
    private int size;

    public FragmentListItemAdapter(BaseLayoutInfo appLayoutInfo,
                                   OnOpenMenuItemClickListener onOpenMenuItemClickListener,
                                   OnPositionVisibleListener onPositionVisibleListener) {
        this.appLayoutInfo = appLayoutInfo;
        this.onOpenMenuItemClickListener = onOpenMenuItemClickListener;
        this.onPositionVisibleListener = onPositionVisibleListener;
        if (this.appLayoutInfo.getChildrenLayouts() == null) {
            this.appLayoutInfo.readFromInputStream(null);
        }
        this.size = appLayoutInfo.size() * appLayoutInfo.getNumCols();
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
        String key = appLayoutInfo.getKeyFromPosition(position);
        holder.landingFragmentListChildItem.setClickable(false);
        holder.landingFragmentListChildItem.setText("");
        holder.landingFragmentListChildItem.setTextAppearance(holder.itemView.getContext(),
                R.style.DefaultItemTextStyle);

        int index = appLayoutInfo.getKeyIndex(key);
        if (index != -1) {
            final BaseLayoutInfo childLayoutInfo = appLayoutInfo.getChildLayout(index);

            if (childLayoutInfo != null) {

                int colIndex = position % appLayoutInfo.getNumCols();

                if (appLayoutInfo.isColBold(colIndex)) {
                    holder.landingFragmentListChildItem.setClickable(true);
                    holder
                            .landingFragmentListChildItem
                            .setTypeface(holder.landingFragmentListChildItem.getTypeface(), 1);
                }

                Object value = childLayoutInfo.getKeyedValue(colIndex, key, true);

                if (value != null) {
                    holder.landingFragmentListChildItem.setText(value.toString());
                    if (appLayoutInfo.isClickable(colIndex)) {
                        holder.landingFragmentListChildItem.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                onOpenMenuItemClickListener.open(childLayoutInfo);
                            }
                        });
                    }
                } else {
                    holder.landingFragmentListChildItem.setText("");
                }
            }
        }

        if (position == 0 && onPositionVisibleListener != null) {
            onPositionVisibleListener.positionVisible(position);
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
