package com.cisco.pegaserverstatusclient.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by jonathanhavstad on 10/21/16.
 */

public class NotificationMessage {
    @Expose
    @SerializedName("title")
    private String title;
    @Expose
    @SerializedName("body")
    private String body;
    @Expose
    @SerializedName("icon")
    private String icon;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}
