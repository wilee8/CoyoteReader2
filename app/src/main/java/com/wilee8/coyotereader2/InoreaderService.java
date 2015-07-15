package com.wilee8.coyotereader2;

import com.wilee8.coyotereader2.gson.UnreadCounts;

import java.util.Map;

import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.QueryMap;
import rx.Observable;

public interface InoreaderService {
	@GET("/reader/api/0/edit-tag")
	Observable<Response> editTag(@QueryMap Map<String, String> options);

	@GET("/reader/api/0/unread-count")
	UnreadCounts unreadCounts();
}
