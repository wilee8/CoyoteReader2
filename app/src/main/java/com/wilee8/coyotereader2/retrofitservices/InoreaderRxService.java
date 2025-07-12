package com.wilee8.coyotereader2.retrofitservices;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.http.POST;
import retrofit2.http.QueryMap;
import rx.Observable;

public interface InoreaderRxService {
	@POST("/accounts/ClientLogin")
	Observable<ResponseBody> clientLogin(@QueryMap Map<String, String> options);

	@POST("/reader/api/0/edit-tag")
	Observable<ResponseBody> editTag(@QueryMap Map<String, String> options);

	@POST("/reader/api/0/mark-all-as-read")
	Observable<ResponseBody> markAllAsRead(@QueryMap Map<String, String> options);

	@POST("reader/api/0/subscription/edit")
	Observable<ResponseBody> editSubscription(@QueryMap Map<String, String> options);

	@POST("reader/api/0/rename-tag")
	Observable<ResponseBody> renameTag(@QueryMap Map<String, String> options);

	@POST("reader/api/0/disable-tag")
	Observable<ResponseBody> disableTag(@QueryMap Map<String, String> options);
}
