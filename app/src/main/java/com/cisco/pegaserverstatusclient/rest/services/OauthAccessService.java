package com.cisco.pegaserverstatusclient.rest.services;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Url;

/**
 * Created by jonathanhavstad on 10/19/16.
 */

public interface OauthAccessService {
    @GET
    Call<ResponseBody> redirect(@Url String url, @Header("cookie") String cookie);
    @FormUrlEncoded
    @Headers({
            "referer: https://sso.cisco.com/autho/forms/CDClogin.html"
    })
    @POST
    Call<ResponseBody> authorizeUser(@Url String url, @Field(value = "username") String username, @Field(value = "password") String password, @Field(value = "csrfmiddlewaretoken") String csrfToken, @Header("cookie") String cookie);
}
