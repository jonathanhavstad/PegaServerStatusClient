package com.cisco.pegaserverstatusclient.rest.services;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

/**
 * Created by jonathanhavstad on 10/27/16.
 */

public interface IBPMStatusService {
    @GET
    Call<ResponseBody> getStatus(@Url String path);
}
