package com.wilee8.coyotereader2;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class AuthenticationService extends Service {
	public AuthenticationService() {
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.d("coyotereader2", "AuthenticatorService onBind");
		AccountAuthenticator accountAuthenticator = new AccountAuthenticator(this);
		return accountAuthenticator.getIBinder();
	}
}
