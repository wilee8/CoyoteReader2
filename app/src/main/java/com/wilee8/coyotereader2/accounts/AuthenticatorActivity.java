package com.wilee8.coyotereader2.accounts;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.view.View;

import com.wilee8.coyotereader2.BuildConfig;
import com.wilee8.coyotereader2.R;
import com.wilee8.coyotereader2.gson.TokenResponse;
import com.wilee8.coyotereader2.retrofitservices.AuthHeaderInterceptor;
import com.wilee8.coyotereader2.retrofitservices.InoreaderRxGsonService;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
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

	private InoreaderRxGsonService mService;
	private Subscription           mLoginSubscription;

	@SuppressWarnings("FieldCanBeLocal")
	private static String OAUTH_URL       = "https://www.inoreader.com/oauth2/auth";
	@SuppressWarnings("FieldCanBeLocal")
	private static String REDIRECT_URI    = "https://coyotereaderrss.androidapp";
	@SuppressWarnings("FieldCanBeLocal")
	private static String OPTIONAL_SCOPES = "read write";

	@SuppressWarnings("FieldCanBeLocal")
	private static String PACKAGE_NAME = "com.android.chrome";
	private CustomTabsServiceConnection mCustomTabsServiceConnection;
	private CustomTabsClient            mCustomTabsClient;
	private CustomTabsSession           mCustomTabsSession;

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
			.addConverterFactory(GsonConverterFactory.create())
			.build();

		mService = restAdapter.create(InoreaderRxGsonService.class);

		setContentView(R.layout.activity_authenticator);
		mProgressView = findViewById(R.id.login_progress);

		startCustomTabs();

		mIntent = getIntent();
		restartAuth(false);
	}

	@Override
	protected void onDestroy() {
		if ((mLoginSubscription != null) && (!mLoginSubscription.isUnsubscribed())) {
			mLoginSubscription.unsubscribe();
		}

		stopCustomTabs();

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

	private class AuthReplyHandler extends Subscriber<TokenResponse> {

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
		public void onNext(TokenResponse tokenResponse) {

			Bundle userData = new Bundle();
			userData.putString(AccountAuthenticator.USER_DATA_TOKEN_TYPE,
				tokenResponse.getTokenType());
			userData.putString(AccountAuthenticator.USER_DATA_REFRESH_TOKEN,
				tokenResponse.getRefreshToken());
			// figure out expiration time
			// expires_in is seconds, need to divide milliseconds by 1000 to get current time in seconds
			String expireString = Long.toString((System.currentTimeMillis() / 1000) + tokenResponse.getExpiresIn()
												- AccountAuthenticator.BUFFER_SECONDS);
			userData.putString(AccountAuthenticator.USER_DATA_EXPIRATION_TIME, expireString);


			final Account account = new Account(AccountAuthenticator.ARG_ACCOUNT_NAME,
				AccountAuthenticator.ACCOUNT_TYPE);
			AccountManager accountManager = AccountManager.get(getBaseContext());
			if (getIntent().getBooleanExtra(AccountAuthenticator.ARG_IS_ADDING_NEW_ACCOUNT, false)) {
				// we don't need to save the password, so pass null
				accountManager.addAccountExplicitly(account, null, userData);
			}

			accountManager.setAuthToken(account,
				AccountAuthenticator.AUTHTOKEN_TYPE_OAUTH2,
				tokenResponse.getAccessToken());

			Bundle result = new Bundle();
			result.putString(AccountManager.KEY_AUTHTOKEN, tokenResponse.getAccessToken());
			result.putString(AccountAuthenticator.USER_DATA_TOKEN_TYPE,
				tokenResponse.getTokenType());

			setAccountAuthenticatorResult(result);
			setResult(RESULT_OK);
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
			launchURL(OAUTH_URL +
					  "?client_id=" + URLEncoder.encode(BuildConfig.INOREADER_APP_ID, "utf-8") +
					  "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, "utf-8") +
					  "&response_type=code" +
					  "&scope=" + URLEncoder.encode(OPTIONAL_SCOPES, "utf-8") +
					  "&state=" + URLEncoder.encode(BuildConfig.CSRF_PROTECTION_STRING, "utf-8"));
		} catch (UnsupportedEncodingException e) {
			// encoding is hard coded, so fix it
		}
	}

	private void startCustomTabs() {
		mCustomTabsServiceConnection = new CustomTabsServiceConnection() {
			@Override
			public void onCustomTabsServiceConnected(ComponentName componentName, CustomTabsClient customTabsClient) {
				mCustomTabsClient = customTabsClient;

				mCustomTabsClient.warmup(0L);
				mCustomTabsSession = mCustomTabsClient.newSession(null);
			}

			@Override
			public void onServiceDisconnected(ComponentName componentName) {
			}
		};

		if (!CustomTabsClient.bindCustomTabsService(this, PACKAGE_NAME, mCustomTabsServiceConnection)) {
			mCustomTabsServiceConnection = null;
		}
	}

	private void stopCustomTabs() {
		if (mCustomTabsServiceConnection != null) {
			unbindService(mCustomTabsServiceConnection);
		}
		mCustomTabsServiceConnection = null;
		mCustomTabsClient = null;
		mCustomTabsSession = null;
	}

	@SuppressWarnings("deprecation")
	private void launchURL(String url) {
		Uri uri = Uri.parse(url);
		// right now only alternative is chrome tabs
		// create pending intent for share action button
		Intent actionIntent = new Intent(Intent.ACTION_SEND);
		actionIntent.setType("text/plain");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			actionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
		} else {
			actionIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		}
		actionIntent.putExtra(Intent.EXTRA_TEXT, url);

		// launch custom tab
		CustomTabsIntent.Builder customTabsIntentBuilder =
			new CustomTabsIntent.Builder(mCustomTabsSession)
				.setToolbarColor(
					ContextCompat.getColor(AuthenticatorActivity.this, R.color.primary))
				.setShowTitle(true);

		CustomTabsIntent customTabsIntent = customTabsIntentBuilder.build();
		customTabsIntent.launchUrl(AuthenticatorActivity.this, uri);
	}
}



