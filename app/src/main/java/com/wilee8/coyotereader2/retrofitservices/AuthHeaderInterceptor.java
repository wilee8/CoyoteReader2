package com.wilee8.coyotereader2.retrofitservices;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthHeaderInterceptor implements Interceptor {
	@Override
	public Response intercept(Chain chain) throws IOException {
		Request request = chain.request();

		Request.Builder builder = request.newBuilder();
//			.addHeader("Content-type", "application/x-www-form-urlencoded");

		Request newRequest = builder.build();
		return chain.proceed(newRequest);
	}
}
