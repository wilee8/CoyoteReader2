package com.wilee8.coyotereader2;

import android.accounts.AccountAuthenticatorActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.wilee8.coyotereader2.retrofitservices.InoreaderRxService;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import rx.Subscription;


/**
 * A login screen that offers login via email/password.
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity {

	// UI references.
	private View                 mProgressView;
	private WebView              mWebView;

	private InoreaderRxService mService;
	private Subscription       mLoginSubscription;
	private String             mUsername;

	private static String OAUTH_URL = "https://www.inoreader.com/oauth2/auth";
	private static String REDIRECT_URI = "http://localhost";
	private static String OPTIONAL_SCOPES = "read write";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_authenticator);
		mProgressView = findViewById(R.id.login_progress);
		mWebView = (WebView) findViewById(R.id.webv);

		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				view.loadUrl(url);
				return true;
			}
		});
		try {
			mWebView.loadUrl(OAUTH_URL +
							 "?client_id=" + URLEncoder.encode(BuildConfig.APPLICATION_ID, "utf-8") +
							 "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, "utf-8") +
							 "&response_type=code" +
							 "&scope=" + URLEncoder.encode(OPTIONAL_SCOPES, "utf-8") +
							 "&state=" + URLEncoder.encode(BuildConfig.CSRF_PROTECTION_STRING, "utf-8"));
		} catch (UnsupportedEncodingException e) {
			// encoding is hard coded, so fix it
		}



//		OkHttpClient client = new OkHttpClient.Builder()
//			.addInterceptor(new HeaderInterceptor(null))
//			.build();
//
//		Retrofit restAdapter = new Retrofit.Builder()
//			.baseUrl("https://www.inoreader.com")
//			.client(client)
//			.addCallAdapterFactory(RxJavaCallAdapterFactory.create())
//			.build();
//
//		mService = restAdapter.create(InoreaderRxService.class);
	}

	@Override
	protected void onDestroy() {
		if ((mLoginSubscription != null) && (!mLoginSubscription.isUnsubscribed())) {
			mLoginSubscription.unsubscribe();
		}

		super.onDestroy();
	}

//	private class AuthReplyHandler extends Subscriber<ResponseBody> {
//
//		@Override
//		public void onCompleted() {
//			unsubscribe();
//		}
//
//		@Override
//		public void onError(Throwable throwable) {
//			String message = throwable.getMessage();
//
//			showProgress(false);
//
//			if (message.equalsIgnoreCase("401 Authorization Required")) {
//				// Failed login, post an error
//				mPasswordView.setError(getString(R.string.error_incorrect_password));
//			} else {
//				mPasswordView.setError(getString(R.string.error_network));
//			}
//
//			mPasswordView.requestFocus();
//
//			unsubscribe();
//		}
//
//		@Override
//		public void onNext(ResponseBody responseBody) {
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
//			unsubscribe();
//			finish();
//		}
//	}
}



