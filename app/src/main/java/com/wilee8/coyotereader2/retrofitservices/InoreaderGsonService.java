package com.wilee8.coyotereader2.retrofitservices;

import com.wilee8.coyotereader2.gson.TokenResponse;
import com.wilee8.coyotereader2.gson.UnreadCounts;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface InoreaderGsonService {
	@GET("/reader/api/0/unread-count")
	Call<UnreadCounts> unreadCountsObject();

	@FormUrlEncoded
	@POST("oauth2/token")
	Call<TokenResponse> oauth2GetToken(@FieldMap Map<String, String> options);
}
