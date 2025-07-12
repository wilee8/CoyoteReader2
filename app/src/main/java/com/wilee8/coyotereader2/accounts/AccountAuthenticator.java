package com.wilee8.coyotereader2.accounts;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.wilee8.coyotereader2.BuildConfig;
import com.wilee8.coyotereader2.R;
import com.wilee8.coyotereader2.gson.TokenResponse;
import com.wilee8.coyotereader2.retrofitservices.InoreaderGsonService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AccountAuthenticator extends AbstractAccountAuthenticator {

	// public strings
	public static final String ACCOUNT_TYPE              = "com.wilee8.coyotereader2";
	//	public static final String AUTHTOKEN_TYPE_STANDARD = "Standard";
	public static final String AUTHTOKEN_TYPE_OAUTH2     = "OAuth2";
	public static final String ARG_ACCOUNT_TYPE          = "ACCOUNT_TYPE";
	public static final String ARG_AUTH_TYPE             = "AUTH_TYPE";
	public static final String ARG_ACCOUNT_NAME          = "OAUTH_ACCOUNT";
	public static final String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";
	public static final String USER_DATA_TOKEN_TYPE      = "token_type";
	public static final String USER_DATA_REFRESH_TOKEN   = "refresh_token";
	public static final String USER_DATA_EXPIRATION_TIME = "expiration_time";

	// a buffer period to make sure oauth token doesn't expire in the middle of requests
	public static final int BUFFER_SECONDS = 15;

	private final Context mContext;

	public AccountAuthenticator(Context context) {
		super(context);

		this.mContext = context;
	}

	@Override
	public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
		return null;
	}

	@Override
	public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
		// only allow one account
		// if an account of this type already exists, return error
		AccountManager accountManager = AccountManager.get(mContext);
		Account[] accounts = accountManager.getAccountsByType(accountType);

		if (accounts.length > 0) {
			final Bundle result = new Bundle();
			result.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION);
			result.putString(AccountManager.KEY_ERROR_MESSAGE, mContext.getString(R.string.error_one_account_allowed));

			return result;
		}

		final Intent intent = new Intent(this.mContext, AuthenticatorActivity.class);
		intent.putExtra(ARG_ACCOUNT_TYPE, ACCOUNT_TYPE);
		intent.putExtra(ARG_AUTH_TYPE, authTokenType);
		intent.putExtra(ARG_IS_ADDING_NEW_ACCOUNT, true);
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
		final Bundle bundle = new Bundle();
		bundle.putParcelable(AccountManager.KEY_INTENT, intent);
		return bundle;
	}

	@Override
	public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
		return null;
	}

	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
		final AccountManager accountManager = AccountManager.get(mContext);

		String authToken = accountManager.peekAuthToken(account, authTokenType);

		if (!TextUtils.isEmpty(authToken)) {
			// we have an auth token, check if it is expired
			Long expirationTime = Long.valueOf(accountManager.getUserData(account,
				AccountAuthenticator.USER_DATA_EXPIRATION_TIME));

			if ((System.currentTimeMillis() / 1000) > expirationTime) {
				try {
					renewToken(accountManager, account);
					authToken = accountManager.peekAuthToken(account, authTokenType);
				} catch (IOException e) {
					throw new NetworkErrorException();
				}
			}
		} else {
			// need to renew auth token
			try {
				renewToken(accountManager, account);
				authToken = accountManager.peekAuthToken(account, authTokenType);
			} catch (IOException e) {
				throw new NetworkErrorException();
			}
		}

		final Bundle result = new Bundle();
		result.putString(AccountAuthenticator.USER_DATA_TOKEN_TYPE,
			accountManager.getUserData(account, AccountAuthenticator.USER_DATA_TOKEN_TYPE));
		result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
		result.putString(AccountManager.KEY_ACCOUNT_NAME, AccountAuthenticator.ARG_ACCOUNT_NAME);
		result.putString(AccountManager.KEY_ACCOUNT_TYPE, AccountAuthenticator.ACCOUNT_TYPE);
		return result;
	}

	@Override
	public String getAuthTokenLabel(String authTokenType) {
		return AUTHTOKEN_TYPE_OAUTH2;
	}

	@Override
	public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
		return null;
	}

	@Override
	public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
		final Bundle result = new Bundle();
		result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
		return result;
	}

	private void renewToken(AccountManager accountManager, Account account) throws IOException {
		Map<String, String> queryMap = new HashMap<>();
		queryMap.put("client_id", BuildConfig.INOREADER_APP_ID);
		queryMap.put("client_secret", BuildConfig.INOREADER_APP_KEY);
		queryMap.put("grant_type", "refresh_token");
		queryMap.put("refresh_token",
			accountManager.getUserData(account, AccountAuthenticator.USER_DATA_REFRESH_TOKEN));

		OkHttpClient client = new OkHttpClient.Builder()
			.build();

		Retrofit restAdapter = new Retrofit.Builder()
			.baseUrl("https://www.inoreader.com")
			.client(client)
			.addConverterFactory(GsonConverterFactory.create())
			.build();

		InoreaderGsonService service = restAdapter.create(InoreaderGsonService.class);
		TokenResponse tokenResponse = service.oauth2GetToken(queryMap).execute().body();

		accountManager.setAuthToken(account, AUTHTOKEN_TYPE_OAUTH2, tokenResponse.getAccessToken());
		accountManager.setUserData(account,
			AccountAuthenticator.USER_DATA_TOKEN_TYPE,
			tokenResponse.getTokenType());
		accountManager.setUserData(account,
			AccountAuthenticator.USER_DATA_REFRESH_TOKEN,
			tokenResponse.getRefreshToken());
		// figure out expiration time
		// expires_in is seconds, need to divide milliseconds by 1000 to get current time in seconds
		accountManager.setUserData(account,
			AccountAuthenticator.USER_DATA_EXPIRATION_TIME,
			Long.toString((System.currentTimeMillis() / 1000) + tokenResponse.getExpiresIn() - BUFFER_SECONDS));
	}
}
