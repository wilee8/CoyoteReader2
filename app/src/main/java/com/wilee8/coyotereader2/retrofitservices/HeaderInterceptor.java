package com.wilee8.coyotereader2.retrofitservices;

import com.wilee8.coyotereader2.BuildConfig;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class HeaderInterceptor implements Interceptor {

	private String mAuthToken;

	public HeaderInterceptor(String authToken) {
		mAuthToken = authToken;
	}

	@Override
	public Response intercept(Chain chain) throws IOException {
		Request request = chain.request();

		Request.Builder builder = request.newBuilder()
			.addHeader("AppId", BuildConfig.INOREADER_APP_ID)
			.addHeader("AppKey", BuildConfig.INOREADER_APP_KEY);

		if (mAuthToken != null) {
			builder.addHeader("Authorization", "GoogleLogin auth=" + mAuthToken);
		}

		return chain.proceed(builder.build());
	}
}
