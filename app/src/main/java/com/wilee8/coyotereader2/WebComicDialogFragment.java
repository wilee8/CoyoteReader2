package com.wilee8.coyotereader2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class WebComicDialogFragment extends DialogFragment {
	private String mMessage;

	public WebComicDialogFragment() {
		mMessage = "";
	}

	@Override
	public void setArguments(Bundle args) {
		super.setArguments(args);

		if (args.containsKey("message")) {
			mMessage = args.getString("message");
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(mMessage);

		return builder.create();
	}

}
