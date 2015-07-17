package com.wilee8.coyotereader2;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

public class SettingsDialog extends DialogFragment {
	public SettingsDialog() {
		// Empty constructor required for DialogFragment
	}

	public static SettingsDialog newInstance(String title) {
		return new SettingsDialog();
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		getDialog().setTitle(R.string.pref_dialog_title);
		LayoutInflater i = getActivity().getLayoutInflater();

		LinearLayout ll = (LinearLayout) i.inflate(R.layout.dialog_settings, null);

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
}
