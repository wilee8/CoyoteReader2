package com.wilee8.coyotereader2;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;

public class NewFolderDialog extends DialogFragment {
	private NewFolderListener mCallback;

	private ArrayList<String> mNewFolderList;

	private EditText    mEditText;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			mCallback = (NewFolderListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() +
											 " must implement NewFolderListener");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mNewFolderList = getArguments().getStringArrayList("newFolderList");
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),
															  R.style.MyAlertDialogStyle);
		LayoutInflater inflater = getActivity().getLayoutInflater();

		builder.setTitle(R.string.alert_new_folder);
		View view = inflater.inflate(R.layout.fragment_new_folder_dialog,
									 (ViewGroup) getView(),
									 true);

		mEditText = (EditText) view.findViewById(R.id.newFolderName);

		builder.setView(view);
		builder.setNegativeButton(R.string.alert_cancel, new NegativeOnClickListener());
		builder.setPositiveButton(R.string.alert_ok, null);

		final AlertDialog alert = builder.create();

		alert.setOnShowListener(dialog -> {
			Button b = alert.getButton(AlertDialog.BUTTON_POSITIVE);
			b.setOnClickListener(new PositiveOnClickListener());
		});

		return alert;
	}

	public interface NewFolderListener {
		void launchChangeFolder(ArrayList<String> newFolderList);
	}

	private class NegativeOnClickListener implements DialogInterface.OnClickListener {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			dismiss();
		}
	}

	private class PositiveOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View view) {
			// hide the focus so any error snackbars appear
			if (getDialog().getCurrentFocus() != null) {
				InputMethodManager inputManager =
					(InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
				inputManager.hideSoftInputFromWindow(getDialog().getCurrentFocus().getWindowToken(),
													 InputMethodManager.HIDE_NOT_ALWAYS);
			}

			String newFolder = mEditText.getText().toString();
			if (newFolder.length() == 0) {
				Toast.makeText(getActivity(),
							   getActivity().getString(R.string.error_new_folder_empty),
							   Toast.LENGTH_SHORT).show();
			} else {
				mNewFolderList.add(newFolder);

				mCallback.launchChangeFolder(mNewFolderList);

				dismiss();
			}
		}
	}
}