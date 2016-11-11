package com.cisco.pegaserverstatusclient.utilities;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.cisco.pegaserverstatusclient.background.tasks.ServerDataRestTask;
import com.cisco.pegaserverstatusclient.binders.SubscriberBinder;

/**
 * Created by jonathanhavstad on 11/10/16.
 */

public class BgServiceConnection implements ServiceConnection {
    private ServerDataRestTask task;
    private BgServiceInfo bgServiceInfo;
    private String url;

    public BgServiceConnection(ServerDataRestTask task, BgServiceInfo bgServiceInfo, String url) {
        this.task = task;
        this.bgServiceInfo = bgServiceInfo;
        this.url = url;
    }
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        bgServiceInfo.setBinder(((SubscriberBinder) service));
        bgServiceInfo.getBinder().addSubscriber(bgServiceInfo.getSubscriber());
        bgServiceInfo.getBinder().addServiceConnection(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        bgServiceInfo.getBinder().removeSubscriber(bgServiceInfo.getSubscriber());
    }

    public String getUrl() {
        return url;
    }

    public ServerDataRestTask getTask() {
        return task;
    }
}
