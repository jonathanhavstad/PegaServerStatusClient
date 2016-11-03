package com.cisco.pegaserverstatusclient.data;

import android.content.Context;

import com.cisco.pegaserverstatusclient.fragments.PegaBaseFragment;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jonathanhavstad on 10/25/16.
 */

public abstract class BaseLayoutInfo {
    protected String friendlyName;
    protected String key;

    public interface AddLayoutViewAdapter {
        void add(PegaBaseFragment fragment);
    }

    public interface ReplaceLayoutViewAdapter {
        void replace(boolean recreateView, PegaBaseFragment newFragment);
    }

    public String getFriendlyName(String key, boolean appendParent) {
        return key;
    }

    public abstract Object getValue(Map<String, Object> appData, String childKey);

    @Override
    public String toString() {
        return getFriendlyName();
    }

    public abstract String getFriendlyName();
    public abstract void setFriendlyName(String friendlyName);
    public abstract String getKey();
    public abstract void setKey(String key);
    public abstract BaseLayoutInfo createChildLayout(String parentKey);
    public abstract PegaBaseFragment addLayoutToView(Context context,
                                                     String parentKey,
                                                     ArrayList<String> keyPath,
                                                     Object appData,
                                                     AddLayoutViewAdapter addLayoutViewAdapter);
    public abstract PegaBaseFragment replaceLayoutToView(Context context,
                                                     String parentKey,
                                                     ArrayList<String> keyPath,
                                                     Object appData,
                                                     ReplaceLayoutViewAdapter replaceLayoutViewAdapter);

    public static class Builder {
        private BaseLayoutInfo baseLayoutInfo;
        private String parentKey;
        private String key;
        private String friendlyName;

        public Builder layout(BaseLayoutInfo baseLayoutInfo) {
            this.baseLayoutInfo = baseLayoutInfo;
            return this;
        }

        public Builder parentKey(String parentKey) {
            this.parentKey = parentKey;
            return this;
        }

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder friendlyName(String friendlyName) {
            this.friendlyName = friendlyName;
            return this;
        }

        public BaseLayoutInfo build() {
            BaseLayoutInfo childInfoLayout = null;

            if (baseLayoutInfo != null) {
                childInfoLayout = baseLayoutInfo.createChildLayout(parentKey);
            } else {
                childInfoLayout = new DomainLayoutInfo();
            }

            if (key != null) {
                childInfoLayout.setKey(key);
            }

            if (friendlyName != null) {
                childInfoLayout.setFriendlyName(friendlyName);
            }

            return childInfoLayout;
        }
    }
}
