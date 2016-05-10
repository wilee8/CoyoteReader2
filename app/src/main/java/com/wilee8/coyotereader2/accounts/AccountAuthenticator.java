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

import com.wilee8.coyotereader2.R;

public class AccountAuthenticator extends AbstractAccountAuthenticator {

	// public strings
	public static final String ACCOUNT_TYPE = "com.wilee8.coyotereader2";
	public static final String AUTHTOKEN_TYPE_STANDARD = "Standard";
	public static final String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";
	public static final String ARG_AUTH_TYPE = "AUTH_TYPE";
	public static final String ARG_ACCOUNT_NAME = "ACCOUNT_NAME";
	public static final String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";

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
			// we have an auth token, return it
			final Bundle result = new Bundle();
			result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
			result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
			result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
			return result;
		}

		// if we get here, we need to prompt the user for username/password to get authtoken
		final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
		intent.putExtra(ARG_ACCOUNT_TYPE, account.type);
		intent.putExtra(ARG_AUTH_TYPE, authTokenType);
		final Bundle bundle = new Bundle();
		bundle.putParcelable(AccountManager.KEY_INTENT, intent);
		return bundle;
	}

	@Override
	public String getAuthTokenLabel(String authTokenType) {
		return AUTHTOKEN_TYPE_STANDARD;
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
}
