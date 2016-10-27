package com.cisco.pegaserverstatusclient.rest.services;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Created by jonathanhavstad on 10/19/16.
 */

public interface OauthRedirectService {
    @GET("/autho/forms/CDClogin.html")
    Call<ResponseBody> getAccessToken();
}
