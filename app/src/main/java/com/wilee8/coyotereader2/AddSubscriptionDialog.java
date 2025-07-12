package com.wilee8.coyotereader2;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.trello.rxlifecycle.components.support.RxDialogFragment;
import com.wilee8.coyotereader2.gson.AddedFeed;
import com.wilee8.coyotereader2.retrofitservices.InoreaderRxGsonService;

import java.util.Map;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class AddSubscriptionDialog extends RxDialogFragment {
	private AddSubscriptionListener mCallback;

	private InoreaderRxGsonService mService;

	private EditText    mEditText;
	private ProgressBar mProgressBar;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			mCallback = (AddSubscriptionListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() +
											 " must implement AddSubscriptionListener");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mService = mCallback.getRxGsonService();
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),
															  R.style.MyAlertDialogStyle);
		LayoutInflater inflater = getActivity().getLayoutInflater();

		builder.setTitle(R.string.alert_add_subscription);
		View view = inflater.inflate(R.layout.fragment_add_subscription_dialog,
									 (ViewGroup) getView(),
									 true);

		mEditText = (EditText) view.findViewById(R.id.subscription);
		mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar1);

		builder.setView(view);
		builder.setNegativeButton(R.string.alert_cancel, new NegativeOnClickListener());
		builder.setPositiveButton(R.string.alert_ok, null);

		final AlertDialog alert = builder.create();
		final RxDialogFragment fragment = this;

		alert.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				Button b = alert.getButton(AlertDialog.BUTTON_POSITIVE);
				b.setOnClickListener(new PositiveOnClickListener(fragment));
			}
		});

		return alert;
	}

	public interface AddSubscriptionListener {
		void refreshOnClick();

		InoreaderRxGsonService getRxGsonService();
	}

	private class NegativeOnClickListener implements DialogInterface.OnClickListener {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			dismiss();
		}
	}

	private class PositiveOnClickListener implements View.OnClickListener {
		private RxDialogFragment fragment;

		public PositiveOnClickListener(RxDialogFragment fragment) {
			this.fragment = fragment;
		}

		@Override
		public void onClick(View v) {

			// hide the focus so any error snackbars appear
			if (getDialog().getCurrentFocus() != null) {
				InputMethodManager inputManager =
					(InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
				inputManager.hideSoftInputFromWindow(getDialog().getCurrentFocus().getWindowToken(),
													 InputMethodManager.HIDE_NOT_ALWAYS);
			}

			String feedUrl = mEditText.getText().toString();
			if (feedUrl.length() == 0) {
				Toast.makeText(getActivity(),
							   getActivity().getString(R.string.error_subscription_empty),
							   Toast.LENGTH_SHORT).show();
				return;
			}

			mEditText.setVisibility(View.GONE);
			mProgressBar.setVisibility(View.VISIBLE);

			Map<String, String> queryMap = new ArrayMap<>();
			queryMap.put("quickadd", feedUrl);
			HandleResult handleResult = new HandleResult();

			mService.quickAdd(queryMap)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.compose(fragment.<AddedFeed>bindToLifecycle())
				.subscribe(handleResult);

		}
	}

	private class HandleResult extends Subscriber<AddedFeed> {

		@Override
		public void onCompleted() {
			mCallback.refreshOnClick();
			dismiss();
			unsubscribe();
		}

		@Override
		public void onError(Throwable e) {
			Toast.makeText(getActivity(),
						   getActivity().getString(R.string.error_adding_subscription),
						   Toast.LENGTH_SHORT).show();
			mProgressBar.setVisibility(View.GONE);
			mEditText.setVisibility(View.VISIBLE);

		}

		@Override
		public void onNext(AddedFeed addedFeed) {
			if (addedFeed.getNumResults() == 0) {
				Toast.makeText(getActivity(),
							   getActivity().getString(R.string.error_adding_subscription),
							   Toast.LENGTH_SHORT).show();
				mProgressBar.setVisibility(View.GONE);
				mEditText.setVisibility(View.VISIBLE);
			} else {
				mCallback.refreshOnClick();
				dismiss();
			}
			unsubscribe();
		}
	}
}
