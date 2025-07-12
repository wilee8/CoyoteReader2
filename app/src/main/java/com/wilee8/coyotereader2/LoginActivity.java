package com.wilee8.coyotereader2;

import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.trello.rxlifecycle.components.support.RxAppCompatActivity;
import com.wilee8.coyotereader2.accounts.AccountAuthenticator;

import java.io.IOException;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends RxAppCompatActivity {
	private static final int ACTIVITY_REQUEST_CODE = 1;

	// UI references.
	ProgressBar mProgress;
	Button      mButton;

	private Context        mContext;
	private AccountManager mAccountManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		mProgress = (ProgressBar) findViewById(R.id.login_progress);
		mButton = (Button) findViewById(R.id.login_button);

		mProgress.setVisibility(View.GONE);
		mButton.setVisibility(View.VISIBLE);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (toolbar != null) {
			setSupportActionBar(toolbar);
		}

		mContext = this;
		mAccountManager = AccountManager.get(this);

		mButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				mProgress.setVisibility(View.VISIBLE);
				mButton.setVisibility(View.GONE);

				// Create login intent in background task
				LaunchLoginIntent launchLoginIntent = new LaunchLoginIntent();
				Observable<Intent> doLogin = Observable.create(new DoLogin());

				doLogin
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
					.compose(LoginActivity.this.<Intent>bindToLifecycle())
					.subscribe(launchLoginIntent);
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode != RESULT_OK) {
			mProgress.setVisibility(View.GONE);
			mButton.setVisibility(View.VISIBLE);

			Snackbar
				.make(findViewById(R.id.relative_layout),
					R.string.error_login,
					Snackbar.LENGTH_LONG)
				.show();
		} else {
			// now that we have logged in, start main activity
			Intent intent = new Intent(mContext, MainActivity.class);
			startActivity(intent);
			finish();
		}
	}

	private class DoLogin implements Observable.OnSubscribe<Intent> {
		@Override
		public void call(Subscriber<? super Intent> subscriber) {
			AccountManagerFuture<Bundle> accountManagerFuture =
				mAccountManager.addAccount(AccountAuthenticator.ACCOUNT_TYPE,
					AccountAuthenticator.AUTHTOKEN_TYPE_OAUTH2,
					null,
					null,
					null,
					null,
					null);

			Bundle loginBundle;
			try {
				loginBundle = accountManagerFuture.getResult();
			} catch (AuthenticatorException | OperationCanceledException | IOException e) {
				subscriber.onError(e);
				return;
			}

			subscriber.onNext((Intent) loginBundle.getParcelable(AccountManager.KEY_INTENT));

			subscriber.onCompleted();
		}
	}

	private class LaunchLoginIntent extends Subscriber<Intent> {

		@Override
		public void onCompleted() {
			unsubscribe();
		}

		@Override
		public void onError(Throwable e) {
			Snackbar
				.make(findViewById(R.id.login_progress),
					R.string.error_login,
					Snackbar.LENGTH_LONG)
				.show();
		}

		@Override
		public void onNext(Intent intent) {
			// this will launch the AuthenticatorActivity
			startActivityForResult(intent, ACTIVITY_REQUEST_CODE);
		}
	}
}



