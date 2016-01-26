package com.wilee8.coyotereader2;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AlertDialog;

import com.squareup.okhttp.ResponseBody;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;
import com.trello.rxlifecycle.components.support.RxDialogFragment;
import com.wilee8.coyotereader2.containers.TagItem;
import com.wilee8.coyotereader2.gson.Category;
import com.wilee8.coyotereader2.gson.Subscription;
import com.wilee8.coyotereader2.gson.SubscriptionList;
import com.wilee8.coyotereader2.retrofitservices.InoreaderRxService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class ChangeSubscriptionFolderDialog extends RxDialogFragment {

	private ChangeSubsciptionFolderListener mCallback;

	private InoreaderRxService mRxService;

	private String mId;
	private Vector<FolderHolder> mFolderList;

	private RxAppCompatActivity mContext;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			mCallback = (ChangeSubsciptionFolderListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() +
				" must implement ChangeSubscriptionFolderListener");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mRxService = mCallback.getRxService();

		mContext = (RxAppCompatActivity) getActivity();

		mId = getArguments().getString("id");
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		// create folder list
		ArrayList<TagItem> navList = mCallback.getNavList();
		mFolderList = new Vector<>();

		for (int i = 0; i < navList.size(); i++) {
			TagItem item = navList.get(i);
			if (!item.getIsFeed()) {
				FolderHolder folder = new FolderHolder();
				folder.setFolderName(item.getName());
				folder.setFolderId(item.getId());

				// initiailize folder list with all unselected
				folder.uncheckAll();

				mFolderList.add(folder);
			}
		}

		// get category list for subscription and check all it belongs to
		SubscriptionList subscriptionList = mCallback.getSubscriptionList();

		for (Subscription subscription : subscriptionList.getSubscriptions()) {
			// we only care about the subscription the active subscription
			if (subscription.getId().equals(mId)) {
				ArrayList<Category> categories = subscription.getCategories();

				// go through each of the categories that the subscription belongs to
				for (Category category : categories) {
					String categoryId = category.getId();

					// check all the folders that match
					for (FolderHolder folder : mFolderList) {
						if (folder.getFolderId().equals(categoryId)) {
							folder.checkAll();
						}
					}
				}
				break;
			}
		}
		// create arrays
		String[] folderNames = new String[mFolderList.size()];
		boolean[] folderChecked = new boolean[mFolderList.size()];

		for (int i = 0; i < mFolderList.size(); i++) {
			FolderHolder folder = mFolderList.get(i);
			folderNames[i] = folder.getFolderName();
			folderChecked[i] = folder.getWasChecked();
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.MyAlertDialogStyle);
		builder.setMultiChoiceItems(folderNames, folderChecked, new FolderClickListener());
		builder.setPositiveButton(R.string.alert_ok, new PositiveButtonListener());
		builder.setNegativeButton(R.string.alert_cancel, new NegativeButtonListener());
		return builder.create();
	}

	public interface ChangeSubsciptionFolderListener {
		void refreshOnClick();

		InoreaderRxService getRxService();

		ArrayList<TagItem> getNavList();

		SubscriptionList getSubscriptionList();
	}

	private class FolderHolder {
		private String folderName;
		private String folderId;
		private Boolean wasChecked;
		private Boolean isChecked;

		public String getFolderName() {
			return folderName;
		}

		public void setFolderName(String folderName) {
			this.folderName = folderName;
		}

		public String getFolderId() {
			return folderId;
		}

		public void setFolderId(String folderId) {
			this.folderId = folderId;
		}

		public Boolean getIsChecked() {
			return isChecked;
		}

		public void setIsChecked(Boolean isChecked) {
			this.isChecked = isChecked;
		}

		public Boolean getWasChecked() {
			return wasChecked;
		}

		public void setWasChecked(Boolean wasChecked) {
			this.wasChecked = wasChecked;
		}

		public void checkAll() {
			this.setIsChecked(true);
			this.setWasChecked(true);
		}

		public void uncheckAll() {
			this.setIsChecked(false);
			this.setWasChecked(false);
		}
	}

	private class FolderClickListener implements DialogInterface.OnMultiChoiceClickListener {

		@Override
		public void onClick(DialogInterface dialog, int which, boolean isChecked) {
			FolderHolder folder = mFolderList.get(which);
			folder.setIsChecked(isChecked);
		}
	}

	private class NegativeButtonListener implements DialogInterface.OnClickListener {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			dismiss();
		}
	}

	private class PositiveButtonListener implements DialogInterface.OnClickListener {

		@SuppressWarnings("unchecked")
		@Override
		public void onClick(DialogInterface dialog, int which) {
			// don't make a network call if nothing was changed
			boolean changedFolder = false;

			Map queryMap = new ArrayMap<>();
			queryMap.put("s", mId);

			for (FolderHolder folder : mFolderList) {
				if (folder.getIsChecked() != folder.getWasChecked()) {
					changedFolder = true;
					if (folder.getIsChecked()) {
						queryMap.put("a", folder.getFolderId());
					} else {
						queryMap.put("r", folder.getFolderId());
					}
				}
			}

			if (changedFolder) {
				ChangeFolderSubscriber changeFolderSubscriber = new ChangeFolderSubscriber();
				mRxService.editSubscription(queryMap)
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
					.compose(mContext.<ResponseBody>bindToLifecycle())
					.subscribe(changeFolderSubscriber);
			}
		}
	}

	private class ChangeFolderSubscriber extends Subscriber<ResponseBody> {
		@Override
		public void onCompleted() {
			System.out.println("ChangeFolderSubscriber onCompleted");
		}

		@Override
		public void onError(Throwable e) {
			Snackbar
				.make(mContext.findViewById(R.id.sceneRoot),
					R.string.error_unsubscribe,
					Snackbar.LENGTH_SHORT)
				.show();
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
						R.string.change_folder_successful,
						Snackbar.LENGTH_SHORT)
					.show();

				mCallback.refreshOnClick();
			} else {
				onError(null);
			}

			unsubscribe();
		}
	}
}
