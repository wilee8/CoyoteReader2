package com.wilee8.coyotereader2;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.wilee8.coyotereader2.containers.FeedItem;
import com.wilee8.coyotereader2.gson.Category;
import com.wilee8.coyotereader2.gson.StreamPref;
import com.wilee8.coyotereader2.gson.Subscription;
import com.wilee8.coyotereader2.gson.SubscriptionList;

import org.parceler.Parcels;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import rx.Observable;
import rx.Subscriber;
import rx.android.app.AppObservable;
import rx.schedulers.Schedulers;

public class TagFragment extends Fragment {

	private TagFragmentListener mCallback;

	private Context     mContext;
	private ImageLoader mImageLoader;

	private TagAdapter mAdapter;

	private String mTagId;

	private ArrayList<FeedItem> mViewData;

	private int mSelected;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = getActivity();

		if (savedInstanceState != null) {
			mSelected = savedInstanceState.getInt("mSelected", -1);
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			mCallback = (TagFragmentListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement TagFragmentListener");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		mTagId = getArguments().getString("id");

		mImageLoader = new ImageLoader(mCallback.getRequestQueue(), new ImageLoader.ImageCache() {

			private final LruCache<String, Bitmap> cache = new LruCache<>(20);

			@Override
			public Bitmap getBitmap(String url) {
				return cache.get(url);
			}

			@Override
			public void putBitmap(String url, Bitmap bitmap) {
				cache.put(url, bitmap);
			}
		});

		if (savedInstanceState != null) {
			mSelected = savedInstanceState.getInt("mSelected", -1);

			if (savedInstanceState.containsKey("mViewData")) {
				mViewData = Parcels.unwrap(savedInstanceState.getParcelable("mViewData"));
			} else {
				mViewData = new ArrayList<>();
			}
		} else {
			mSelected = -1;
			mViewData = new ArrayList<>();
		}

		View view = inflater.inflate(R.layout.fragment_tag, container, false);

		RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.tag_recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
		recyclerView.setHasFixedSize(true);

		mAdapter = new TagAdapter();
		recyclerView.setAdapter(mAdapter);

		if(mViewData.size() == 0) {
			GetFeedsSubscriber subscriber = new GetFeedsSubscriber();
			AppObservable.bindFragment(
				this,
				Observable.create(new GetFeedsObserver())
					.subscribeOn(Schedulers.io()))
				.subscribe(subscriber);
		}

		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putInt("mSelected", mSelected);

		if (mViewData != null) {
			outState.putParcelable("mViewData", Parcels.wrap(mViewData));
		}
	}

	private class GetFeedsObserver implements Observable.OnSubscribe<Void> {

		@Override
		public void call(Subscriber<? super Void> subscriber) {
			SubscriptionList subscriptionList = mCallback.getSubscriptionList();
			ArrayList<Subscription> subscriptions = subscriptionList.getSubscriptions();
			ArrayList<StreamPref> preferences = mCallback.getPreferences(mTagId);
			Boolean sortAlpha = mCallback.getSortAlpha();

			String subscriptionOrderingString = "";

			if (!sortAlpha) {
				for (int i = 0; i < preferences.size(); i++) {
					StreamPref pref = preferences.get(i);
					if (pref.getId().matches("subscription-ordering")) {
						subscriptionOrderingString = pref.getValue();
					}
				}
			}

			ArrayList<String> subscriptionOrdering = new ArrayList<>();

			if (!sortAlpha) {
				for (int i = 0; i < subscriptionOrderingString.length() / 8; i++) {
					subscriptionOrdering.add(subscriptionOrderingString.substring(i * 8, (i * 8) + 8));
				}

				// Populate view data with blank items so we can replace them in
				// sort order
				FeedItem blank = new FeedItem();
				for (int i = 0; i <= subscriptionOrdering.size(); i++) {
					mViewData.add(blank);
				}
			}

			// add tag feeds to the list
			for (int i = 0; i < subscriptions.size(); i++) {
				Subscription sub = subscriptions.get(i);

				ArrayList<Category> categories = sub.getCategories();

				for (int j = 0; j < categories.size(); j++) {
					Category category = categories.get(j);
					String subCategory = category.getId();
					if (subCategory.matches(mTagId)) {
						// add to mViewData
						FeedItem feedItem = new FeedItem();

						String feedId = sub.getId();
						feedItem.setFeedId(feedId);
						feedItem.setFeedTitle(sub.getTitle());
						feedItem.setFeedIconUrl(sub.getIconUrl());

						try {
							feedItem.setUnreadCount(mCallback.getUnreadCount(feedId));
						} catch (InvalidParameterException e) {
							feedItem.setUnreadCount(0);
						}

						if (sortAlpha) {
							mViewData.add(feedItem);
						} else {
							// Get subscription ordering
							String ordering = sub.getSortId();
							for (int k = 0; k < subscriptionOrdering.size(); k++) {
								if (ordering.matches(subscriptionOrdering.get(k))) {
									mViewData.set(k + 1, feedItem);
								}
							}
						}

						break;
					}
				}
			}

			// alphabetize the list if needed
			if (sortAlpha) {
				Collections.sort(mViewData, new Comparator<FeedItem>() {

					@Override
					public int compare(FeedItem lhs, FeedItem rhs) {
						Locale locale = getResources().getConfiguration().locale;
						return lhs.getFeedTitle().toLowerCase(locale).compareTo(rhs.getFeedTitle().toLowerCase(locale));
					}
				});
			}

			// set all items at the start
			FeedItem feedItemHeader = new FeedItem();
			feedItemHeader.setFeedTitle("All Items");
			feedItemHeader.setFeedId(mTagId);
			feedItemHeader.setFeedIconUrl(null);
			try {
				feedItemHeader.setUnreadCount(mCallback.getUnreadCount(mTagId));
			} catch (InvalidParameterException e) {
				feedItemHeader.setUnreadCount(0);
			}

			if (sortAlpha) {
				mViewData.add(0, feedItemHeader);
			} else {
				mViewData.set(0, feedItemHeader);
			}

			subscriber.onCompleted();
		}
	}

	private class GetFeedsSubscriber extends Subscriber<Void> {

		@Override
		public void onCompleted() {
			// Fill in nav drawer
			mAdapter.notifyDataSetChanged();
		}

		@Override
		public void onError(Throwable throwable) {

		}

		@Override
		public void onNext(Void aVoid) {

		}
	}

	public interface TagFragmentListener {
		public int getUnreadCount(String feedId);

		public SubscriptionList getSubscriptionList();

		public ArrayList<StreamPref> getPreferences(String id);

		public Boolean getSortAlpha();

		public RequestQueue getRequestQueue();
	}

	private class TagAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

		public static final int VIEW_TYPE_LOCAL_RESOURCE = 0;
		public static final int VIEW_TYPE_REMOTE_URL     = 1;

		@Override
		public int getItemViewType(int position) {
			// local view only in first slot - all items

			if (position == 0) {
				return VIEW_TYPE_LOCAL_RESOURCE;
			} else {
				return VIEW_TYPE_REMOTE_URL;
			}
		}

		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			if (viewType == VIEW_TYPE_LOCAL_RESOURCE) {
				return new LocalViewHolder(LayoutInflater.from(mContext).inflate(R.layout.row_tag_card, parent, false));
			} else {
				return new NetworkViewHolder(LayoutInflater.from(mContext).inflate(R.layout.row_tag_card_network, parent, false));
			}
		}

		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
			FeedItem feedItem = mViewData.get(position);

			TagViewHolder tagViewHolder = (TagViewHolder) holder;

			switch (getItemViewType(position)) {
				case VIEW_TYPE_REMOTE_URL:
					String iconUrl = feedItem.getFeedIconUrl();
					NetworkViewHolder networkViewHolder = (NetworkViewHolder) holder;
					networkViewHolder.feedIcon.setImageUrl(iconUrl, mImageLoader);

					break;

				case VIEW_TYPE_LOCAL_RESOURCE:
					LocalViewHolder localViewHolder = (LocalViewHolder) holder;
					localViewHolder.feedIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.clear_favicon));

					break;
			}

			tagViewHolder.feedName.setText(feedItem.getFeedTitle());

			// unread count should only be visible if not zero
			// bold both name and unread count if not zero
			if (feedItem.getUnreadCount() != 0) {
				tagViewHolder.feedUnreadCount.setVisibility(View.VISIBLE);
				tagViewHolder.feedUnreadCount.setText(String.valueOf(feedItem.getUnreadCount()));
				tagViewHolder.feedUnreadCount.setTypeface(null, Typeface.BOLD);
				tagViewHolder.feedName.setTypeface(null, Typeface.BOLD);
			} else {
				tagViewHolder.feedUnreadCount.setVisibility(View.GONE);
				tagViewHolder.feedUnreadCount.setTypeface(null, Typeface.NORMAL);
				tagViewHolder.feedName.setTypeface(null, Typeface.NORMAL);
			}

			if (position == mSelected) {
				tagViewHolder.feedCardView.setCardBackgroundColor(getResources().getColor(R.color.accent));
			} else {
				tagViewHolder.feedCardView.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background));
			}

			tagViewHolder.itemView.setOnClickListener(new TagClickListener(position));
		}

		@Override
		public int getItemCount() {
			return mViewData.size();
		}
	}

	private class TagViewHolder extends RecyclerView.ViewHolder {
		public TextView feedName;
		public TextView feedUnreadCount;
		public CardView feedCardView;

		public TagViewHolder(View itemView) {
			super(itemView);
			feedName = (TextView) itemView.findViewById(R.id.feedName);
			feedUnreadCount = (TextView) itemView.findViewById(R.id.feedUnreadCount);
			feedCardView = (CardView) itemView.findViewById(R.id.feedCardView);
		}
	}

	private class LocalViewHolder extends TagViewHolder {
		public ImageView feedIcon;

		public LocalViewHolder(View itemView) {
			super(itemView);
			feedIcon = (ImageView) itemView.findViewById(R.id.feedIcon);
		}
	}

	private class NetworkViewHolder extends TagViewHolder {
		public NetworkImageView feedIcon;

		public NetworkViewHolder(View itemView) {
			super(itemView);
			feedIcon = (NetworkImageView) itemView.findViewById(R.id.feedIcon);
		}
	}

	private class TagClickListener implements View.OnClickListener {

		private int mPosition;

		public TagClickListener(int position) {
			mPosition = position;
		}

		@Override
		public void onClick(View view) {
			CardView newView = (CardView) view;
			newView.setCardBackgroundColor(getResources().getColor(R.color.accent));
			int oldSelected = mSelected;
			mSelected = mPosition;

			if (oldSelected != -1) {
				mAdapter.notifyItemChanged(oldSelected);
			}

			FeedItem item = mViewData.get(mPosition);

//			mCallback.selectNav(item.getId(), item.getIsFeed(), item.getName());
		}
	}
}
