package com.cisco.pegaserverstatusclient.parcelables;

import android.os.Parcel;
import android.os.Parcelable;

import com.cisco.pegaserverstatusclient.binders.ServerDataBinder;

/**
 * Created by jonathanhavstad on 10/20/16.
 */

public class PegaServerNetworkParcelable implements Parcelable {
    private ServerDataBinder binder = new ServerDataBinder();

    public PegaServerNetworkParcelable() {}

    protected PegaServerNetworkParcelable(Parcel in) {
        binder = (ServerDataBinder) in.readStrongBinder();
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

    public ServerDataBinder getBinder() {
        return binder;
    }

    public void setBinder(ServerDataBinder binder) {
        this.binder = binder;
    }
}
