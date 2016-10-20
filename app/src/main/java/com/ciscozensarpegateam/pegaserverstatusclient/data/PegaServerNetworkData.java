package com.ciscozensarpegateam.pegaserverstatusclient.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.ciscozensarpegateam.pegaserverstatusclient.binders.PegaServerNetworkBinder;

/**
 * Created by jonathanhavstad on 10/20/16.
 */

public class PegaServerNetworkData implements Parcelable {
    private PegaServerNetworkBinder binder = new PegaServerNetworkBinder();

    public PegaServerNetworkData() {}


    protected PegaServerNetworkData(Parcel in) {
        binder = (PegaServerNetworkBinder) in.readStrongBinder();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStrongBinder(binder);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PegaServerNetworkData> CREATOR = new Creator<PegaServerNetworkData>() {
        @Override
        public PegaServerNetworkData createFromParcel(Parcel in) {
            return new PegaServerNetworkData(in);
        }

        @Override
        public PegaServerNetworkData[] newArray(int size) {
            return new PegaServerNetworkData[size];
        }
    };

    public PegaServerNetworkBinder getBinder() {
        return binder;
    }

    public void setBinder(PegaServerNetworkBinder binder) {
        this.binder = binder;
    }
}
