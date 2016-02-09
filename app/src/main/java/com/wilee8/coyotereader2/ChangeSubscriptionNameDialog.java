package com.wilee8.coyotereader2;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
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

import com.squareup.okhttp.ResponseBody;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;
import com.wilee8.coyotereader2.retrofitservices.InoreaderRxService;

import java.io.IOException;
import java.util.Map;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class ChangeSubscriptionNameDialog extends DialogFragment {

	private ChangeSubscriptionNameListener mCallback;

	private InoreaderRxService mRxService;

	private RxAppCompatActivity mContext;

	private String mId;
	private String mName;

	private EditText    mEditText;
	private ProgressBar mProgressBar;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		try {
			mCallback = (ChangeSubscriptionNameListener) context;
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString() +
											 " must implement ChangeSubscriptionNameListener");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mRxService = mCallback.getRxService();

		mContext = (RxAppCompatActivity) getActivity();

		mId = getArguments().getString("id");
		mName = getArguments().getString("name");
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),
															  R.style.MyAlertDialogStyle);
		LayoutInflater inflater = getActivity().getLayoutInflater();

		builder.setTitle(R.string.alert_subscription_name);
		View view = inflater.inflate(R.layout.fragment_change_subscription_name_dialog,
									 (ViewGroup) getView(),
									 true);

		mEditText = (EditText) view.findViewById(R.id.subscription);
		mEditText.setText(mName);
		mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar1);

		builder.setView(view);
		builder.setNegativeButton(R.string.alert_cancel, new NegativeOnClickListener());
		builder.setPositiveButton(R.string.alert_ok, null);

		final AlertDialog alert = builder.create();

		alert.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				Button b = alert.getButton(AlertDialog.BUTTON_POSITIVE);
				b.setOnClickListener(new PositiveOnClickListener());
			}
		});
		return alert;
	}

	private class NegativeOnClickListener implements DialogInterface.OnClickListener {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			dismiss();
		}
	}

	private class PositiveOnClickListener implements View.OnClickListener {

		@SuppressWarnings("unchecked")
		@Override
		public void onClick(View view) {

			// hide the focus so any error snackbars appear
			if (getDialog().getCurrentFocus() != null) {
				InputMethodManager inputManager =
					(InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
				inputManager.hideSoftInputFromWindow(getDialog().getCurrentFocus().getWindowToken(),
													 InputMethodManager.HIDE_NOT_ALWAYS);
			}

			String feedName = mEditText.getText().toString();
			if (feedName.length() == 0) {
				Toast.makeText(getActivity(),
							   getActivity().getString(R.string.error_subscription_name_empty),
							   Toast.LENGTH_SHORT).show();
				return;
			}

			if (!feedName.contentEquals(mName)) {
				mEditText.setVisibility(View.GONE);
				mProgressBar.setVisibility(View.VISIBLE);

				Map queryMap = new ArrayMap<>();
				queryMap.put("s", mId);
				queryMap.put("t", feedName);

				ChangeNameSubscriber changeFolderSubscriber = new ChangeNameSubscriber();
				mRxService.editSubscription(queryMap)
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
					.compose(mContext.<ResponseBody>bindToLifecycle())
					.subscribe(changeFolderSubscriber);
			} else {
				dismiss();
			}
		}
	}

	private class ChangeNameSubscriber extends Subscriber<ResponseBody> {
		@Override
		public void onCompleted() {
			dismiss();
			unsubscribe();
		}

		@Override
		public void onError(Throwable e) {
			Toast.makeText(getActivity(),
						   getActivity().getString(R.string.error_change_name),
						   Toast.LENGTH_SHORT).show();
			mProgressBar.setVisibility(View.GONE);
			mEditText.setVisibility(View.VISIBLE);
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

			if (response.equalsIgnoreCase("OK")) {
				Snackbar
					.make(mContext.findViewById(R.id.sceneRoot),
						  R.string.change_folder_name_successful,
						  Snackbar.LENGTH_SHORT)
					.show();

				mCallback.refreshOnClick();
			} else {
				onError(null);
			}

			dismiss();
			unsubscribe();
		}
	}

	public interface ChangeSubscriptionNameListener {
		void refreshOnClick();

		InoreaderRxService getRxService();
	}
}
