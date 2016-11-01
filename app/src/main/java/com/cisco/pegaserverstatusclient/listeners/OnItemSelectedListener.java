package com.cisco.pegaserverstatusclient.listeners;

/**
 * Created by jonathanhavstad on 10/31/16.
 */

public interface OnItemSelectedListener {
    void receiveData(String parentKey, String key, Object data);
}