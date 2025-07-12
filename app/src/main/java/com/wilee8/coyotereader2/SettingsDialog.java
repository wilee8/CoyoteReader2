package com.wilee8.coyotereader2;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;

public class SettingsDialog extends DialogFragment {

	private int mWidth;

	public SettingsDialog() {
		// Empty constructor required for DialogFragment
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		mWidth = getArguments().getInt("width", -1);
		super.onCreate(savedInstanceState);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// PreferenceFragmentCompat hides the window title in Marshmallow for some reason
		// Create a title view in the fragment layout, but hide the window title for pre-marshmallow
		Dialog dialog =  super.onCreateDialog(savedInstanceState);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		return dialog;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		LayoutInflater i = getActivity().getLayoutInflater();

		LinearLayout ll = (LinearLayout) i.inflate(R.layout.dialog_settings, container, false);

		SettingsFragment settingsFragment = new SettingsFragment();
		FragmentManager fm = getChildFragmentManager();
		FragmentTransaction transaction = fm.beginTransaction();
		transaction.replace(R.id.settings_frame, settingsFragment);
		transaction.commit();

		Button okButton = (Button) ll.findViewById(R.id.settings_button);
		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});
		return ll;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mWidth != -1) {
			Window window = getDialog().getWindow();
			window.setLayout(mWidth, window.getAttributes().height);
			window.setGravity(Gravity.CENTER);
		}
	}
}
