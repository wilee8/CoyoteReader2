package com.wilee8.coyotereader2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.wilee8.coyotereader2.gson.StreamContents;

import rx.Observable;
import rx.Subscriber;

public class FeedFragment extends Fragment {
	private static final String AUTH_PREFS = "AuthPrefsFile";

	private FeedFragmentListener mCallback;

	private Activity mContext;

	private String mAuthToken;

	private String mFeedId;

	private FeedAdapter  mAdapter;
	private ProgressBar  mProgress;
	private RecyclerView mItemRecyclerView;

	private int mSelected;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = getActivity();

		// Get token from shared preferences
		SharedPreferences preferences = mContext.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE);
		mAuthToken = preferences.getString("authToken", "");

		// if it doesn't exist, go to login activity
		if ((mAuthToken == null) || (mAuthToken.equals(""))) {
			Intent intent = new Intent(mContext, LoginActivity.class);
			startActivity(intent);
			mContext.finish();
			return;
		}

		if (savedInstanceState != null) {
			mSelected = savedInstanceState.getInt("mSelected", -1);
		} else {
			mSelected = -1;
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			mCallback = (FeedFragmentListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement FeedFragmentListener");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		mFeedId = getArguments().getString("id");

		View view = inflater.inflate(R.layout.fragment_feed, container, false);
		mProgress = (ProgressBar) view.findViewById(R.id.progressbar_loading);
		mItemRecyclerView = (RecyclerView) view.findViewById(R.id.feed_recycler_view);

		mItemRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
		mItemRecyclerView.setItemAnimator(new DefaultItemAnimator());

		mAdapter = new FeedAdapter();
		mItemRecyclerView.setAdapter(mAdapter);

		mProgress.setVisibility(View.VISIBLE);
		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putInt("mSelected", mSelected);
	}

	private class FetchItems implements Observable.OnSubscribe<StreamContents> {

		@Override
		public void call(Subscriber<? super StreamContents> subscriber) {

		}
	}

	public interface FeedFragmentListener {

	}

	private class FeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

		private static final int VIEW_TYPE_NORMAL = 0;
		private static final int VIEW_TYPE_FOOTER = 1;

		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			return null;
		}

		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

		}

		@Override
		public int getItemCount() {
			return 0;
		}
	}
}
