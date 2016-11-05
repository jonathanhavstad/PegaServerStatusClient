package com.cisco.pegaserverstatusclient.parcelables;

import android.os.Parcel;
import android.os.Parcelable;

import com.cisco.pegaserverstatusclient.binders.LayoutInfoBinder;

/**
 * Created by jonathanhavstad on 10/25/16.
 */

public class BaseInfoParcelable implements Parcelable {

    private LayoutInfoBinder layoutInfoBinder;

    public BaseInfoParcelable() {}

    protected BaseInfoParcelable(Parcel in) {
        layoutInfoBinder = (LayoutInfoBinder) in.readStrongBinder();
    }

    public static final Creator<BaseInfoParcelable> CREATOR = new Creator<BaseInfoParcelable>() {
        @Override
        public BaseInfoParcelable createFromParcel(Parcel in) {
            return new BaseInfoParcelable(in);
        }

        @Override
        public BaseInfoParcelable[] newArray(int size) {
            return new BaseInfoParcelable[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStrongBinder(layoutInfoBinder);
    }

    public LayoutInfoBinder getLayoutInfoBinder() {
        return layoutInfoBinder;
    }

    public void setLayoutInfoBinder(LayoutInfoBinder layoutInfoBinder) {
        this.layoutInfoBinder = layoutInfoBinder;
    }
}
