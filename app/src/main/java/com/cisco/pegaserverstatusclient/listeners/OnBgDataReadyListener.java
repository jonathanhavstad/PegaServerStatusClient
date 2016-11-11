package com.cisco.pegaserverstatusclient.listeners;

import com.cisco.pegaserverstatusclient.utilities.BgServiceInfo;

import java.util.Map;

/**
 * Created by jonathanhavstad on 11/10/16.
 */

public interface OnBgDataReadyListener {
    void send(BgServiceInfo bgServiceInfo, Map<String, Object> appData);
}
