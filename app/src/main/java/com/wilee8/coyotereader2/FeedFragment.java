package com.wilee8.coyotereader2;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.wilee8.coyotereader2.containers.ArticleItem;
import com.wilee8.coyotereader2.gson.GsonRequest;
import com.wilee8.coyotereader2.gson.Item;
import com.wilee8.coyotereader2.gson.StreamContents;

import org.parceler.Parcels;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.app.AppObservable;
import rx.android.schedulers.AndroidSchedulers;
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
	private Subscription            mEmitterSubscription;
	private Boolean                 mFetchInProgress;

	private ArrayList<ArticleItem> mItems;
	private FeedAdapter            mAdapter;
	private ProgressBar            mProgress;

	private int mSelected;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = getActivity();

		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey("mItems")) {
				mItems = Parcels.unwrap(savedInstanceState.getParcelable("mItems"));
				mSelected = savedInstanceState.getInt("mSelected", -1);
				mContinuation = savedInstanceState.getString("mContinuation", null);
				mFetchInProgress = savedInstanceState.getBoolean("mFetchInProgress", false);
			} else {
				mItems = new ArrayList<>();
				mCallback.clearStreamContents();
				mSelected = -1;
				mContinuation = null;
				mFetchInProgress = false;
			}
		} else {
			mItems = new ArrayList<>();
			mCallback.clearStreamContents();
			mSelected = -1;
			mContinuation = null;
			mFetchInProgress = false;
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
		UpdateItems updateItems = new UpdateItems();

		mEmitterSubscription = AppObservable.bindFragment(this,
														  emitter
															  .lift(new FetchItems())
															  .subscribeOn(Schedulers.io())
															  .observeOn(AndroidSchedulers.mainThread()))
			.subscribe(updateItems);

		View view = inflater.inflate(R.layout.fragment_feed, container, false);
		mProgress = (ProgressBar) view.findViewById(R.id.progressbar_loading);
		RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.feed_recycler_view);

		recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
		recyclerView.setItemAnimator(new DefaultItemAnimator());

		mAdapter = new FeedAdapter();
		recyclerView.setAdapter(mAdapter);

		if (mItems.size() == 0) {
			mProgress.setVisibility(View.VISIBLE);

			mFetchInProgress = true;
			emitter.onNext(mContinuation);
		} else {

		}
		return view;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (mEmitterSubscription != null) {
			mEmitterSubscription.unsubscribe();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putParcelable("mItems", Parcels.wrap(mItems));
		outState.putString("mContinuation", mContinuation);
		outState.putInt("mSelected", mSelected);
	}

	private class FetchItems implements Observable.Operator<Integer, String> {

		@Override
		public Subscriber<? super String> call(final Subscriber<? super Integer> subscriber) {
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
									// function will take all articles in response and add them to mItems
									int firstNewIndex = addArticles(response);

									subscriber.onNext(firstNewIndex);

									if (mContinuation == null) {
										subscriber.onCompleted();
									}
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

	private class UpdateItems extends Subscriber<Integer> {

		@Override
		public void onCompleted() {
			unsubscribe();
		}

		@Override
		public void onError(Throwable e) {
			Toast.makeText(mContext, R.string.error_fetch_data, Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onNext(Integer integer) {
			mCallback.setFeedContents(mItems);

			mProgress.setVisibility(View.GONE);

			mAdapter.notifyItemRangeChanged(integer, mItems.size() - integer);

			mFetchInProgress = false;
		}
	}

	// function returns the index of the first new item
	private int addArticles(StreamContents contents) {
		mContinuation = contents.getContinuation();

		if (mItems.size() != 0) {
			// if footer is present, remove so we can append all
			ArticleItem lastItem = mItems.get(mItems.size() - 1);
			if (lastItem.getIsFooter()) {
				mItems.remove(mItems.size() - 1);
			}
		}

		int firstNewIndex = mItems.size();

		ArrayList<Item> items = contents.getItems();
		for (int i = 0; i < items.size(); i++) {
			ArticleItem article = new ArticleItem();
			Item item = items.get(i);

			article.setId(item.getId());
			article.setTitle(item.getTitle());
			article.setCategories(item.getCategories());
			article.setSummary(item.getSummary().getContent());
			article.setAuthor(item.getAuthor());
			article.setCanonical(item.getCanonical().get(0).getHref());
			article.setOrigin(item.getOrigin().getTitle());
			article.setIsFooter(false);

			mItems.add(article);
		}

		// add footer if necessary
		if (mContinuation != null) {
			ArticleItem footer = new ArticleItem();
			footer.setIsFooter(true);
			// no other fields matter
			mItems.add(footer);
		}

		return firstNewIndex;
	}

	public interface FeedFragmentListener {
		String getAuthToken();

		Boolean getUnreadOnly();

		RequestQueue getQueue();

		void clearStreamContents();

		void setFeedContents(ArrayList<ArticleItem> items);

		String getUserId();
	}

	private class FeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

		private static final int VIEW_TYPE_NORMAL = 0;
		private static final int VIEW_TYPE_FOOTER = 1;

		@Override
		public int getItemViewType(int position) {
			ArticleItem item = mItems.get(position);

			if (item.getIsFooter()) {
				return VIEW_TYPE_FOOTER;
			} else {
				return VIEW_TYPE_NORMAL;
			}
		}

		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			if (viewType == VIEW_TYPE_NORMAL) {
				return new ArticleViewHolder(LayoutInflater.from(mContext).
					inflate(R.layout.row_feed_card, parent, false));
			} else {
				return new FooterViewHolder(LayoutInflater.from(mContext).
					inflate(R.layout.row_feed_footer, parent, false));
			}
		}

		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
			ArticleItem item = mItems.get(position);

			if (getItemViewType(position) == VIEW_TYPE_FOOTER) {
				// if we're binding the footer, we should start loading the remaining items
				return;
			}

			ArticleViewHolder viewHolder = (ArticleViewHolder) holder;
			Boolean unread = true;
			Boolean starred = false;

			ArrayList<String> categories = item.getCategories();

			for (int i = 0; i < categories.size(); i++) {
				String category = categories.get(i);
				if (category.equals("user/" + mCallback.getUserId() + "/state/com.google/read")) {
					unread = false;
				}
				if (category.equals("user/" + mCallback.getUserId() + "/state/com.google/starred")) {
					starred = true;
				}
			}

			if (unread) {
				viewHolder.articleInfo.setText(
					Html.fromHtml("<b>" + item.getTitle() + "</b> - " + item.getOrigin()));
			} else {
				viewHolder.articleInfo.setText(
					Html.fromHtml(item.getTitle() + " - " + item.getOrigin()));
			}

			if (starred) {
				viewHolder.articleStar.setImageDrawable(mContext.getResources().getDrawable(
					R.drawable.ic_star_grey600_48dp));
			} else {
				viewHolder.articleStar.setImageDrawable(mContext.getResources().getDrawable(
					R.drawable.ic_star_outline_grey600_48dp));
			}

//			holder.itemData.setOnClickListener(new ArticleClickListener(position));
//			holder.itemStar.setOnClickListener(new StarClickListener(position));

			if ((mSelected != -1) && (position == mSelected)) {
				viewHolder.articleCardView.setCardBackgroundColor(
					getResources().getColor(R.color.accent));
			} else {
				viewHolder.articleCardView.setCardBackgroundColor(
					getResources().getColor(R.color.cardview_light_background));
			}
		}

		@Override
		public int getItemCount() {
			return mItems.size();
		}
	}

	private class FooterViewHolder extends RecyclerView.ViewHolder {

		public FooterViewHolder(View itemView) {
			super(itemView);
			// footer is just progress bar, so no fields to set
		}
	}

	private class ArticleViewHolder extends RecyclerView.ViewHolder {

		public CardView  articleCardView;
		public ImageView articleStar;
		public TextView  articleInfo;

		public ArticleViewHolder(View itemView) {
			super(itemView);
			articleCardView = (CardView) itemView.findViewById(R.id.articleCardView);
			articleStar = (ImageView) itemView.findViewById(R.id.articleStar);
			articleInfo = (TextView) itemView.findViewById(R.id.articleInfo);
		}
	}
}
