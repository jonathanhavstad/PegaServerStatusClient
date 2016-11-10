package com.cisco.pegaserverstatusclient.listeners;

import org.json.JSONArray;

/**
 * Created by jonathanhavstad on 11/8/16.
 */

public interface OnDataLoadedListener {
    void send(JSONArray jsonArray);
    void error(String error);
}
