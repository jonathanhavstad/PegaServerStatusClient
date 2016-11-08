package com.cisco.pegaserverstatusclient.rest.services;

import com.cisco.pegaserverstatusclient.layouts.AppLayoutInfo;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

/**
 * Created by jonathanhavstad on 11/2/16.
 */

public interface AppsService {
    @GET
    Call<List<AppLayoutInfo>> getStatusWithJsonArray(@Url String path);
}
