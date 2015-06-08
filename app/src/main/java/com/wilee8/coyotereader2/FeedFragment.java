package com.wilee8.coyotereader2;

import android.app.Activity;
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

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.wilee8.coyotereader2.containers.ArticleItem;
import com.wilee8.coyotereader2.gson.GsonRequest;
import com.wilee8.coyotereader2.gson.StreamContents;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import rx.Observable;
import rx.Subscriber;
import rx.android.app.AppObservable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

public class FeedFragment extends Fragment {
	private FeedFragmentListener mCallback;

	private Activity mContext;

	private RequestQueue mQueue;

	private String mAuthToken;

	private String                  mFeedId;
	private String                  mContinuation;
	private Subject<String, String> emitter;

	private ArrayList<ArticleItem> mItems;
	private FeedAdapter            mAdapter;
	private ProgressBar            mProgress;
	private RecyclerView           mItemRecyclerView;

	private int mSelected;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = getActivity();

		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey("mItems")) {
				mSelected = savedInstanceState.getInt("mSelected", -1);
				mContinuation = savedInstanceState.getString("mContinuation", null);
			} else {
				mItems = new ArrayList<>();
				mCallback.clearStreamContents();
				mSelected = -1;
				mContinuation = null;
			}
		} else {
			mItems = new ArrayList<>();
			mCallback.clearStreamContents();
			mSelected = -1;
			mContinuation = null;
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

		mAuthToken = mCallback.getAuthToken();

		mQueue = mCallback.getQueue();

		PublishSubject<String> emitterSubject = PublishSubject.create();
		emitter = new SerializedSubject<>(emitterSubject);
		AddItems addItems = new AddItems();

		AppObservable.bindActivity(mContext,
								   emitter
									   .lift(new FetchItems())
									   .subscribeOn(Schedulers.io()))
			.subscribe(addItems);

		View view = inflater.inflate(R.layout.fragment_feed, container, false);
		mProgress = (ProgressBar) view.findViewById(R.id.progressbar_loading);
		mItemRecyclerView = (RecyclerView) view.findViewById(R.id.feed_recycler_view);

		mItemRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
		mItemRecyclerView.setItemAnimator(new DefaultItemAnimator());

		mAdapter = new FeedAdapter();
		mItemRecyclerView.setAdapter(mAdapter);

		if(mItems.size() == 0) {
			mProgress.setVisibility(View.VISIBLE);

			emitter.onNext(mContinuation);
		} else {

		}
		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putInt("mSelected", mSelected);
	}

	private class FetchItems implements Observable.Operator<String, String> {

		@Override
		public Subscriber<? super String> call(final Subscriber<? super String> subscriber) {
			return new Subscriber<String>() {
				@Override
				public void onCompleted() {
					subscriber.onCompleted();
				}

				@Override
				public void onError(Throwable e) {
					subscriber.onError(e);
				}

				@Override
				public void onNext(String s) {
					String showUnreadOnly;
					String contParam;

					Map<String, String> headers = new HashMap<>();
					headers.put("Authorization", "GoogleLogin auth=" + mAuthToken);
					headers.put("AppId", getString(R.string.app_id));
					headers.put("AppKey", getString(R.string.app_key));

					if (mCallback.getUnreadOnly()) {
						showUnreadOnly = "?xt=user/-/state/com.google/read";
					} else {
						showUnreadOnly = "";
					}

					if (s == null) {
						contParam = "";
					} else {
						if (showUnreadOnly.length() == 0) {
							contParam = "?";
						} else {
							contParam = "&";
						}

						contParam = contParam.concat("c=" + s);
					}

					String fullUrl;
					try {
						fullUrl = "https://www.inoreader.com/reader/api/0/stream/contents" +
							URLEncoder.encode(mFeedId, "utf-8") + showUnreadOnly + contParam;
					} catch (IOException e) {
						subscriber.onError(e);
						return;
					}

					GsonRequest<StreamContents> streamRequest =
						new GsonRequest<>(
							fullUrl, StreamContents.class, headers,
							new Response.Listener<StreamContents>() {
								@Override
								public void onResponse(StreamContents response) {
									//TODO parse the response
									System.out.println(response.getContinuation());
								}
							}, new Response.ErrorListener() {
							@Override
							public void onErrorResponse(VolleyError error) {
								subscriber.onError(error);
							}
						});

					mQueue.add(streamRequest);

				}
			};
		}
	}

	private class AddItems extends Subscriber<String> {

		@Override
		public void onCompleted() {

		}

		@Override
		public void onError(Throwable e) {

		}

		@Override
		public void onNext(String s) {

		}
	}

	public interface FeedFragmentListener {
		String getAuthToken();

		Boolean getUnreadOnly();

		RequestQueue getQueue();

		void clearStreamContents();
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
