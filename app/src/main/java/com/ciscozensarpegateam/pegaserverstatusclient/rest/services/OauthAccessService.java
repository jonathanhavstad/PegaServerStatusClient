package com.ciscozensarpegateam.pegaserverstatusclient.rest.services;

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
            "origin:https://www.instagram.com",
            "referer:https://www.instagram.com/accounts/login/?force_classic_login=&next=/oauth/authorize/%3Fredirect_uri%3Dhttps%3A//www.google.com%26response_type%3Dtoken%26client_id%3De28ac3623e9b4fb2b27b6b5703b482f1"
    })
    @POST
    Call<ResponseBody> authorizeUser(@Url String url, @Field(value = "username") String username, @Field(value = "password") String password, @Field(value = "csrfmiddlewaretoken") String csrfToken, @Header("cookie") String cookie);
}
