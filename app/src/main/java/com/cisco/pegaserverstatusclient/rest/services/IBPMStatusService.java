package com.cisco.pegaserverstatusclient.rest.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Url;

/**
 * Created by jonathanhavstad on 10/27/16.
 */

public interface IBPMStatusService {
    @Headers("Cache-Control: no-cache")
    @GET
    Call<JsonArray> getStatusWithJsonArray(@Url String path);
    @Headers("Cache-Control: no-cache")
    @GET
    Call<JsonObject> getStatusWithJsonObject(@Url String path);
}
