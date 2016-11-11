package com.cisco.pegaserverstatusclient.listeners;

import com.cisco.pegaserverstatusclient.utilities.DataCallbackHolder;

import java.util.Map;

/**
 * Created by jonathanhavstad on 11/10/16.
 */

public interface OnDataReadyListener {
    void sendDataLoadResult(DataCallbackHolder dataCallbackHolder, int dataLoadResult);
    void sendData(DataCallbackHolder dataCallbackHolder, Map<String, Object> appData);
}
