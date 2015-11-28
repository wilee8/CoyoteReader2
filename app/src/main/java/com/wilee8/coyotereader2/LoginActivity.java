package com.wilee8.coyotereader2;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.trello.rxlifecycle.components.support.RxAppCompatActivity;


/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends RxAppCompatActivity {

	// UI references.
	private Context              mContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (toolbar != null) {
			setSupportActionBar(toolbar);
		}

		mContext = this;
	}
}



