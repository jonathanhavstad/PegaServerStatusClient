package com.cisco.pegaserverstatusclient.parcelables;

import android.os.Parcel;
import android.os.Parcelable;

import com.cisco.pegaserverstatusclient.binders.PegaServerNetworkBinder;

/**
 * Created by jonathanhavstad on 10/20/16.
 */

public class PegaServerNetworkParcelable implements Parcelable {
    private PegaServerNetworkBinder binder = new PegaServerNetworkBinder();

    public PegaServerNetworkParcelable() {}

    protected PegaServerNetworkParcelable(Parcel in) {
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

    public static final Creator<PegaServerNetworkParcelable> CREATOR =
            new Creator<PegaServerNetworkParcelable>() {
        @Override
        public PegaServerNetworkParcelable createFromParcel(Parcel in) {
            return new PegaServerNetworkParcelable(in);
        }

        @Override
        public PegaServerNetworkParcelable[] newArray(int size) {
            return new PegaServerNetworkParcelable[size];
        }
    };

    public PegaServerNetworkBinder getBinder() {
        return binder;
    }

    public void setBinder(PegaServerNetworkBinder binder) {
        this.binder = binder;
    }
}
