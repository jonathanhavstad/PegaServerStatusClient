package com.ciscozensarpegateam.pegaserverstatusclient.rest.services;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Created by jonathanhavstad on 10/19/16.
 */

public interface OauthRedirectService {
    @GET("/oauth/authorize/?redirect_uri=https://www.google.com&response_type=token&client_id=e28ac3623e9b4fb2b27b6b5703b482f1")
    Call<ResponseBody> getAccessToken();
}
