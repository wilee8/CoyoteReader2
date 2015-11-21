package com.wilee8.coyotereader2;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.util.Map;

import retrofit.Retrofit;
import retrofit.RxJavaCallAdapterFactory;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


/**
 * A login screen that offers login via email/password.
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity {

	// UI references.
	private AutoCompleteTextView mEmailView;
	private EditText             mPasswordView;
	private View                 mProgressView;
	private View                 mLoginFormView;

	private InoreaderRxService mService;
	private Subscription       mLoginSubscription;
	private String             mUsername;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		// Set up the login form.
		mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
		String accountName = getIntent().getStringExtra(AccountAuthenticator.ARG_ACCOUNT_NAME);
		if (accountName != null) {
			mEmailView.setText(accountName);
		}

		mPasswordView = (EditText) findViewById(R.id.password);
		mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
				if (id == R.id.login || id == EditorInfo.IME_NULL) {
					attemptLogin();
					return true;
				}
				return false;
			}
		});

		Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
		mEmailSignInButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				attemptLogin();
			}
		});

		mLoginFormView = findViewById(R.id.login_form);
		mProgressView = findViewById(R.id.login_progress);

		Retrofit restAdapter = new Retrofit.Builder()
			.baseUrl("https://www.inoreader.com")
			.addCallAdapterFactory(RxJavaCallAdapterFactory.create())
			.build();

		restAdapter.client()
			.networkInterceptors()
			.add(new HeaderInterceptor(null));

		mService = restAdapter.create(InoreaderRxService.class);
	}

	@Override
	protected void onDestroy() {
		if ((mLoginSubscription != null) && (!mLoginSubscription.isUnsubscribed())) {
			mLoginSubscription.unsubscribe();
		}

		super.onDestroy();
	}

	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	@SuppressWarnings("unchecked")
	public void attemptLogin() {

		// Reset errors.
		mEmailView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		String email = mEmailView.getText().toString();
		String password = mPasswordView.getText().toString();

		boolean cancel = false;
		View focusView = null;


		// Check for a valid password, if the user entered one.
		if (TextUtils.isEmpty(password)) {
			mPasswordView.setError(getString(R.string.error_field_required));
			focusView = mPasswordView;
			cancel = true;
		}

		// Check for a valid email address.
		if (TextUtils.isEmpty(email)) {
			mEmailView.setError(getString(R.string.error_field_required));
			focusView = mEmailView;
			cancel = true;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			showProgress(true);

			mUsername = email;

			Map queryMap = new ArrayMap<>();
			queryMap.put("Email", email);
			queryMap.put("Passwd", password);

			AuthReplyHandler authReplyHandler = new AuthReplyHandler();
			mLoginSubscription = mService.clientLogin(queryMap)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(authReplyHandler);
		}
	}

	private class AuthReplyHandler extends Subscriber<ResponseBody> {

		@Override
		public void onCompleted() {
			unsubscribe();
		}

		@Override
		public void onError(Throwable throwable) {
			String message = throwable.getMessage();

			showProgress(false);

			if (message.equalsIgnoreCase("401 Authorization Required")) {
				// Failed login, post an error
				mPasswordView.setError(getString(R.string.error_incorrect_password));
			} else {
				mPasswordView.setError(getString(R.string.error_network));
			}

			mPasswordView.requestFocus();

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

			// Get the authentication token
			String holder[] = response.split("Auth=");
			String token = holder[1].replaceAll("\n", "");

			// return username and authToken to authenticator
			String accountType = getIntent().getStringExtra(AccountAuthenticator.ARG_ACCOUNT_TYPE);
			String authTokenType = getIntent().getStringExtra(AccountAuthenticator.ARG_AUTH_TYPE);
			if (authTokenType == null) {
				authTokenType = AccountAuthenticator.AUTHTOKEN_TYPE_STANDARD;
			}
			Bundle result = new Bundle();
			result.putString(AccountManager.KEY_ACCOUNT_NAME, mUsername);
			result.putString(AccountManager.KEY_AUTHTOKEN, token);
			result.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);

			final Account account = new Account(mUsername, accountType);
			AccountManager accountManager = AccountManager.get(getBaseContext());
			if (getIntent().getBooleanExtra(AccountAuthenticator.ARG_IS_ADDING_NEW_ACCOUNT, false)) {
				// we don't need to save the password, so pass null
				accountManager.addAccountExplicitly(account, null, null);
			}

			accountManager.setAuthToken(account, authTokenType, token);

			setAccountAuthenticatorResult(result);
			setResult(RESULT_OK);
			unsubscribe();
			finish();
		}

	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	public void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
			mLoginFormView.animate().setDuration(shortAnimTime).alpha(
				show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
				}
			});

			mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
			mProgressView.animate().setDuration(shortAnimTime).alpha(
				show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
				}
			});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}
}



