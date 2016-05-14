package com.wilee8.coyotereader2.accounts;

import android.accounts.AccountAuthenticatorActivity;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.util.ArrayMap;
import android.view.View;

import com.wilee8.coyotereader2.BuildConfig;
import com.wilee8.coyotereader2.R;
import com.wilee8.coyotereader2.retrofitservices.AuthHeaderInterceptor;
import com.wilee8.coyotereader2.retrofitservices.InoreaderRxService;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


/**
 * A login screen that offers login via email/password.
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity {

	// UI references.
	private View mProgressView;

	private Intent mIntent;

	private InoreaderRxService mService;
	private Subscription       mLoginSubscription;

	@SuppressWarnings("FieldCanBeLocal")
	private static String OAUTH_URL       = "https://www.inoreader.com/oauth2/auth";
	@SuppressWarnings("FieldCanBeLocal")
	private static String REDIRECT_URI    = "https://coyotereaderrss.androidapp";
	@SuppressWarnings("FieldCanBeLocal")
	private static String OPTIONAL_SCOPES = "read write";

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		OkHttpClient client = new OkHttpClient.Builder()
			.addInterceptor(new AuthHeaderInterceptor())
			.build();

		Retrofit restAdapter = new Retrofit.Builder()
			.baseUrl("https://www.inoreader.com")
			.client(client)
			.addCallAdapterFactory(RxJavaCallAdapterFactory.create())
			.build();

		mService = restAdapter.create(InoreaderRxService.class);

		setContentView(R.layout.activity_authenticator);
		mProgressView = findViewById(R.id.login_progress);

		mIntent = getIntent();
		restartAuth(false);
	}

	@Override
	protected void onDestroy() {
		if ((mLoginSubscription != null) && (!mLoginSubscription.isUnsubscribed())) {
			mLoginSubscription.unsubscribe();
		}

		super.onDestroy();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		mIntent = intent;
	}

	@Override
	protected void onResume() {
		super.onResume();

		Uri uri = mIntent.getData();

		if ((uri != null) && (uri.toString().startsWith(REDIRECT_URI))) {
			try {
				if (uri.getQueryParameter("state").matches(BuildConfig.CSRF_PROTECTION_STRING)) {
					Map<String, String> queryMap = new ArrayMap<>();
					queryMap.put("code", uri.getQueryParameter("code"));
					queryMap.put("redirect_uri", URLEncoder.encode(REDIRECT_URI, "utf-8"));
					queryMap.put("client_id", BuildConfig.INOREADER_APP_ID);
					queryMap.put("client_secret", BuildConfig.INOREADER_APP_KEY);
					queryMap.put("scope", "");
					queryMap.put("grant_type", "authorization_code");

					AuthReplyHandler authReplyHandler = new AuthReplyHandler();
					mLoginSubscription = mService.oauth2GetToken(queryMap)
						.subscribeOn(Schedulers.io())
						.observeOn(AndroidSchedulers.mainThread())
						.subscribe(authReplyHandler);

				} else {
					restartAuth(true);
				}
			} catch (UnsupportedEncodingException e) {
				restartAuth(true);
			}
		}
	}

	private class AuthReplyHandler extends Subscriber<ResponseBody> {

		@Override
		public void onCompleted() {
			unsubscribe();
		}

		@Override
		public void onError(Throwable throwable) {
			restartAuth(true);

			unsubscribe();
		}

		@Override
		public void onNext(ResponseBody responseBody) {
			String response;
			try {
				response = responseBody.string();
			} catch (IOException e) {
				onError(e);
				return;
			}
//
//			// Get the authentication token
//			String holder[] = response.split("Auth=");
//			String token = holder[1].replaceAll("\n", "");
//
//			// return username and authToken to authenticator
//			String accountType = getIntent().getStringExtra(AccountAuthenticator.ARG_ACCOUNT_TYPE);
//			String authTokenType = getIntent().getStringExtra(AccountAuthenticator.ARG_AUTH_TYPE);
//			if (authTokenType == null) {
//				authTokenType = AccountAuthenticator.AUTHTOKEN_TYPE_STANDARD;
//			}
//			Bundle result = new Bundle();
//			result.putString(AccountManager.KEY_ACCOUNT_NAME, mUsername);
//			result.putString(AccountManager.KEY_AUTHTOKEN, token);
//			result.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);
//
//			final Account account = new Account(mUsername, accountType);
//			AccountManager accountManager = AccountManager.get(getBaseContext());
//			if (getIntent().getBooleanExtra(AccountAuthenticator.ARG_IS_ADDING_NEW_ACCOUNT, false)) {
//				// we don't need to save the password, so pass null
//				accountManager.addAccountExplicitly(account, null, null);
//			}
//
//			accountManager.setAuthToken(account, authTokenType, token);
//
//			setAccountAuthenticatorResult(result);
//			setResult(RESULT_OK);
			unsubscribe();
			finish();
		}
	}

	private void restartAuth(Boolean isError) {
		if (isError) {
			Snackbar.make(
				mProgressView,
				R.string.error_login,
				Snackbar.LENGTH_LONG)
				.show();
		}

		try {
			Intent intent = new Intent(
				Intent.ACTION_VIEW,
				Uri.parse(OAUTH_URL +
						  "?client_id=" + URLEncoder.encode(BuildConfig.INOREADER_APP_ID, "utf-8") +
						  "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, "utf-8") +
						  "&response_type=code" +
						  "&scope=" + URLEncoder.encode(OPTIONAL_SCOPES, "utf-8") +
						  "&state=" + URLEncoder.encode(BuildConfig.CSRF_PROTECTION_STRING, "utf-8")));
			startActivity(intent);
		} catch (UnsupportedEncodingException e) {
			// encoding is hard coded, so fix it
		}
	}
}



