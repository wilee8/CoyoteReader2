package com.wilee8.coyotereader2;

import android.accounts.AccountAuthenticatorActivity;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.util.ArrayMap;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.wilee8.coyotereader2.retrofitservices.AuthHeaderInterceptor;
import com.wilee8.coyotereader2.retrofitservices.InoreaderRxService;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


/**
 * A login screen that offers login via email/password.
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity {

	// UI references.
	private View    mProgressView;
	private WebView mWebView;

	private Boolean mAuthInProgress;

	private InoreaderRxService       mService;
	private Subscription             mLoginSubscription;
	private Observable<ResponseBody> mRequest;

	@SuppressWarnings("FieldCanBeLocal")
	private static String OAUTH_URL       = "https://www.inoreader.com/oauth2/auth";
	@SuppressWarnings("FieldCanBeLocal")
	private static String REDIRECT_URI    = "http://localhost";
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
		mWebView = (WebView) findViewById(R.id.webv);

		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setWebViewClient(new WebViewClient() {

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);

				if ((url.matches("^http://localhost.*")) && (!mAuthInProgress)) {
					mAuthInProgress = true;
					mWebView.setVisibility(View.GONE);
					mProgressView.setVisibility(View.VISIBLE);
					// parse the URL to get the query parameters
					Map<String, String> params = new HashMap<>();
					try {
						String query = new URL(url).getQuery();
						String[] pairs = query.split("&");
						for (String pair : pairs) {
							int idx = pair.indexOf("=");
							params.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
								URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
						}

						if (params.get("state").matches(BuildConfig.CSRF_PROTECTION_STRING)) {
							Map<String, String> queryMap = new ArrayMap<>();
							queryMap.put("code", params.get("code"));
							queryMap.put("redirect_uri", URLEncoder.encode(REDIRECT_URI, "utf-8"));
							queryMap.put("client_id", BuildConfig.INOREADER_APP_ID);
							queryMap.put("client_secret", BuildConfig.INOREADER_APP_KEY);
							queryMap.put("scope", "");
							queryMap.put("grant_type", "authorization_code");

							AuthReplyHandler authReplyHandler = new AuthReplyHandler();
							mRequest = mService.oauth2GetToken(queryMap)
								.subscribeOn(Schedulers.io())
								.observeOn(AndroidSchedulers.mainThread());
							mLoginSubscription = mRequest.subscribe(authReplyHandler);

						} else {
							restartAuth(true);
						}
					} catch (MalformedURLException | UnsupportedEncodingException e) {
						restartAuth(true);
					}
				}

			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				view.loadUrl(url);
				return true;
			}
		});

		restartAuth(false);
	}

	@Override
	protected void onDestroy() {
		if ((mLoginSubscription != null) && (!mLoginSubscription.isUnsubscribed())) {
			mLoginSubscription.unsubscribe();
		}

		super.onDestroy();
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
//			String response;
//			try {
//				response = responseBody.string();
//			} catch (IOException e) {
//				onError(e);
//				return;
//			}
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

	private void restartAuth(Boolean showErrorMessage) {
		mAuthInProgress = false;
		mProgressView.setVisibility(View.GONE);
		mWebView.setVisibility(View.VISIBLE);

		try {
			mWebView.loadUrl(OAUTH_URL +
							 "?client_id=" + URLEncoder.encode(BuildConfig.INOREADER_APP_ID, "utf-8") +
							 "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, "utf-8") +
							 "&response_type=code" +
							 "&scope=" + URLEncoder.encode(OPTIONAL_SCOPES, "utf-8") +
							 "&state=" + URLEncoder.encode(BuildConfig.CSRF_PROTECTION_STRING, "utf-8"));
		} catch (UnsupportedEncodingException e) {
			// encoding is hard coded, so fix it
		}

		if (showErrorMessage) {
			Snackbar
				.make(mWebView, R.string.error_login, Snackbar.LENGTH_LONG)
				.show();
		}
	}
}



