package com.wilee8.coyotereader2.retrofitservices;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;

import com.wilee8.coyotereader2.CoyoteReaderApplication;
import com.wilee8.coyotereader2.R;
import com.wilee8.coyotereader2.accounts.AccountAuthenticator;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class HeaderInterceptor implements Interceptor {

	private AccountManager mAccountManager;
	private Activity       mActivity;

	private Account mAccount;

	public HeaderInterceptor(AccountManager accountManager, Activity activity) {
		mAccountManager = accountManager;
		mActivity = activity;

		// if we don't have account permissions we're hosed
		if (ContextCompat.checkSelfPermission(mActivity,
			Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED) {
			Account[] accounts = mAccountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);
			mAccount = accounts[0];
		} else {
			mAccount = null;
		}
	}

	@Override
	public Response intercept(Chain chain) throws IOException {

		// mutex here to make sure multiple
		CoyoteReaderApplication.headerLock();

		if (mAccount == null) {
			// no permissions to get account information, punt
			throw new IOException(CoyoteReaderApplication.getContext().getResources()
				.getString(R.string.error_account_permissions));
		}

		Request.Builder builder;

		try {
			// wipe the auth token from the cache if it's expired
			Long expirationTime = Long.valueOf(mAccountManager.getUserData(mAccount,
				AccountAuthenticator.USER_DATA_EXPIRATION_TIME));

			if ((System.currentTimeMillis() / 1000 ) > expirationTime) {
				// invalidate cached auth token so account manager knows to get a new one
				AccountManagerFuture<Bundle> accountManagerFuture =
					mAccountManager.getAuthToken(mAccount,
						AccountAuthenticator.AUTHTOKEN_TYPE_OAUTH2,
						null,
						mActivity,
						null,
						null);
				Bundle invalidateBundle = accountManagerFuture.getResult();

				// need to get old auth token to invalidate it
				mAccountManager.invalidateAuthToken(AccountAuthenticator.ACCOUNT_TYPE,
					invalidateBundle.getString(AccountManager.KEY_AUTHTOKEN));
			}

			// get data from existing account
			AccountManagerFuture<Bundle> accountManagerFuture =
				mAccountManager.getAuthToken(mAccount,
					AccountAuthenticator.AUTHTOKEN_TYPE_OAUTH2,
					null,
					mActivity,
					null,
					null);
			Bundle authTokenBundle = accountManagerFuture.getResult();

			Request request = chain.request();

			builder = request.newBuilder()
				.addHeader("Authorization",
					mAccountManager.getUserData(mAccount, AccountAuthenticator.USER_DATA_TOKEN_TYPE) +
					" " +
					authTokenBundle.getString(AccountManager.KEY_AUTHTOKEN));
		} catch (android.accounts.OperationCanceledException |
			IOException |
			AuthenticatorException e) {
			throw new IOException(CoyoteReaderApplication.getContext()
				.getResources().getString(R.string.error_login));
		} finally {
			CoyoteReaderApplication.headerUnlock();
		}

		return chain.proceed(builder.build());
	}
}
