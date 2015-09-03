package com.wilee8.coyotereader2;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wilee8.coyotereader2.containers.ArticleItem;
import com.wilee8.coyotereader2.gson.Item;
import com.wilee8.coyotereader2.gson.StreamContents;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Map;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class FeedFragment extends Fragment {
	private FeedFragmentListener mCallback;

	private Activity mContext;

	private String mAuthToken;

	private String           mFeedId;
	private String           mContinuation;
	private long             mUpdated;
	private InoreaderService mService;
	private Subscription     mSubscription;
	private Boolean          mFetchInProgress;

	private ArrayList<ArticleItem> mItems;
	private FeedAdapter            mAdapter;
	private RecyclerView           mRecyclerView;
	private LinearLayoutManager    mLayoutManager;
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
				mUpdated = savedInstanceState.getLong("mUpdated", -1);
				mFetchInProgress = savedInstanceState.getBoolean("mFetchInProgress", false);
			} else {
				mItems = new ArrayList<>();
				mCallback.clearStreamContents();
				mSelected = -1;
				mContinuation = null;
				mUpdated = -1;
				mFetchInProgress = false;
			}
		} else {
			mItems = new ArrayList<>();
			mCallback.clearStreamContents();
			mSelected = -1;
			mContinuation = null;
			mUpdated = -1;
			mFetchInProgress = false;
		}
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		try {
			mCallback = (FeedFragmentListener) context;
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString() + " must implement FeedFragmentListener");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		mFeedId = getArguments().getString("id");

		mAuthToken = mCallback.getAuthToken();

		View view = inflater.inflate(R.layout.fragment_feed, container, false);
		mProgress = (ProgressBar) view.findViewById(R.id.progressbar_loading);
		mRecyclerView = (RecyclerView) view.findViewById(R.id.feed_recycler_view);

		mLayoutManager = new LinearLayoutManager(mContext);
		mRecyclerView.setLayoutManager(mLayoutManager);
		mRecyclerView.setItemAnimator(new DefaultItemAnimator());

		mAdapter = new FeedAdapter();
		mRecyclerView.setAdapter(mAdapter);
		mRecyclerView.addOnScrollListener(new RecyclerScrollListener());

		RequestInterceptor requestInterceptor = new RequestInterceptor() {
			@Override
			public void intercept(RequestFacade request) {
				request.addHeader("Authorization", "GoogleLogin auth=" + mAuthToken);
				request.addHeader("AppId", BuildConfig.INOREADER_APP_ID);
				request.addHeader("AppKey", BuildConfig.INOREADER_APP_KEY);
			}
		};

		RestAdapter restAdapter = new RestAdapter.Builder()
			.setEndpoint("https://www.inoreader.com")
			.setRequestInterceptor(requestInterceptor)
			.build();

		mService = restAdapter.create(InoreaderService.class);
		if (mItems.size() == 0) {
			mProgress.setVisibility(View.VISIBLE);

			getMoreArticles();
		}

		return view;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if ((mSubscription != null) && (!mSubscription.isUnsubscribed())) {
			mSubscription.unsubscribe();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putParcelable("mItems", Parcels.wrap(mItems));
		outState.putString("mContinuation", mContinuation);
		outState.putInt("mSelected", mSelected);
		outState.putLong("mUpdated", mUpdated);
	}

	@SuppressWarnings("unchecked")
	private void getMoreArticles() {
		mFetchInProgress = true;

		Map queryMap = new ArrayMap<>();
		if (mCallback.getUnreadOnly()) {
			queryMap.put("xt", "user/-/state/com.google/read");
		}
		if (mContinuation != null) {
			queryMap.put("c", mContinuation);
		}

		UpdateItems updateItems = new UpdateItems();

		if ((mSubscription != null) && (!mSubscription.isUnsubscribed())) {
			mSubscription.unsubscribe();
		}
		mSubscription = mService.streamContents(mFeedId, queryMap)
			.lift(new AddArticles())
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(updateItems);
	}

	private class AddArticles implements Observable.Operator<Integer, StreamContents> {
		@Override
		public Subscriber<? super StreamContents> call(final Subscriber<? super Integer> subscriber) {
			return new Subscriber<StreamContents>() {
				@Override
				public void onCompleted() {
					subscriber.onCompleted();
				}

				@Override
				public void onError(Throwable e) {
					subscriber.onError(e);
				}

				@Override
				public void onNext(StreamContents streamContents) {
					mContinuation = streamContents.getContinuation();

					if (mContinuation == null) {
						mRecyclerView.clearOnScrollListeners();
					}

					mUpdated = streamContents.getUpdated();

					if (mItems.size() != 0) {
						// if footer is present, remove so we can append all
						ArticleItem lastItem = mItems.get(mItems.size() - 1);
						if (lastItem.getIsFooter()) {
							mItems.remove(mItems.size() - 1);
						}
					}

					int firstNewIndex = mItems.size();

					ArrayList<Item> items = streamContents.getItems();
					for (int i = 0; i < items.size(); i++) {
						ArticleItem article = new ArticleItem();
						Item item = items.get(i);

						article.setId(item.getId());
						article.setTitle(item.getTitle());
						article.setSummary(item.getSummary().getContent());
						article.setAuthor(item.getAuthor());
						article.setCanonical(item.getCanonical().get(0).getHref());
						article.setOrigin(item.getOrigin().getTitle());
						article.setIsFooter(false);

						ArrayList<String> categories = item.getCategories();
						article.setUnread(true);
						article.setStarred(false);

						for (int j = 0; j < categories.size(); j++) {
							String category = categories.get(j);
							if (category.equals("user/" + mCallback.getUserId() + "/state/com.google/read")) {
								article.setUnread(false);
							}

							if (category.equals("user/" + mCallback.getUserId() + "/state/com.google/starred")) {
								article.setStarred(true);
								break;
							}
						}

						mItems.add(article);
					}

					// add footer if necessary
					if (mContinuation != null) {
						ArticleItem footer = new ArticleItem();
						footer.setIsFooter(true);
						// no other fields matter
						mItems.add(footer);
					}

					subscriber.onNext(firstNewIndex);
					subscriber.onCompleted();
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
			Snackbar
				.make(mContext.findViewById(R.id.sceneRoot),
					  R.string.error_fetch_data,
					  Snackbar.LENGTH_LONG)
				.show();

			unsubscribe();
		}

		@Override
		public void onNext(Integer integer) {
			mCallback.setFeedContents(mItems, mFeedId, mUpdated);

			mProgress.setVisibility(View.GONE);

			mAdapter.notifyItemRangeChanged(integer, mItems.size() - integer);

			mFetchInProgress = false;
		}
	}

	public interface FeedFragmentListener {
		String getAuthToken();

		Boolean getUnreadOnly();

		void clearStreamContents();

		void setFeedContents(ArrayList<ArticleItem> items, String id, long updated);

		String getUserId();

		void selectArticle(int position);

		void onStarClicked(int position, Boolean starred);
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

		@SuppressWarnings("deprecation")
		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
			ArticleItem item = mItems.get(position);

			if (getItemViewType(position) == VIEW_TYPE_FOOTER) {
				// if we're binding the footer, we should start loading the remaining items
				return;
			}

			ArticleViewHolder viewHolder = (ArticleViewHolder) holder;

			if (item.getUnread()) {
				viewHolder.articleInfo.setText(
					Html.fromHtml("<b>" + item.getTitle() + "</b> - " + item.getOrigin()));
			} else {
				viewHolder.articleInfo.setText(
					Html.fromHtml(item.getTitle() + " - " + item.getOrigin()));
			}

			if (item.getStarred()) {
				viewHolder.articleStar.setImageDrawable(mContext.getResources().getDrawable(
					R.drawable.ic_star_grey600_48dp));
			} else {
				viewHolder.articleStar.setImageDrawable(mContext.getResources().getDrawable(
					R.drawable.ic_star_outline_grey600_48dp));
			}

			viewHolder.articleInfo.setOnClickListener(new FeedSelectClickListener(position));
			viewHolder.articleInfo.setOnTouchListener(new FeedSelectTouchListener(position));
			viewHolder.articleStar.setOnClickListener(new StarClickListener(position));

			viewHolder.articleInfo.setBackground(getResources().getDrawable(R.drawable.ripple_selector));
			viewHolder.articleStar.setBackground(getResources().getDrawable(R.drawable.ripple_selector));
			viewHolder.articleWrapper.setBackground(getResources().getDrawable(R.drawable.ripple_selector));
			if ((mSelected != -1) && (position == mSelected)) {
				viewHolder.articleWrapper.setSelected(true);
			} else {
				viewHolder.articleWrapper.setSelected(false);
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

		public RelativeLayout articleWrapper;
		public ImageView      articleStar;
		public TextView       articleInfo;

		public ArticleViewHolder(View itemView) {
			super(itemView);
			articleWrapper = (RelativeLayout) itemView.findViewById(R.id.articleWrapper);
			articleStar = (ImageView) itemView.findViewById(R.id.articleStar);
			articleInfo = (TextView) itemView.findViewById(R.id.articleInfo);
		}
	}

	private class RecyclerScrollListener extends RecyclerView.OnScrollListener {
		@Override
		public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
			if (!mFetchInProgress) {
				if (mItems.size() != 0) {
					int totalItemcount = mLayoutManager.getItemCount();
					int lastVisibleItem = mLayoutManager.findLastVisibleItemPosition();

					if (lastVisibleItem >= (totalItemcount - 1)) {
						getMoreArticles();
					}
				}
			}
		}
	}

	private class FeedSelectClickListener implements View.OnClickListener {

		int mPosition;

		public FeedSelectClickListener(int position) {
			mPosition = position;
		}

		@Override
		public void onClick(View view) {
			mCallback.selectArticle(mPosition);
		}
	}

	private class FeedSelectTouchListener implements View.OnTouchListener {

		int mPosition;

		public FeedSelectTouchListener(int position) {
			mPosition = position;
		}

		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
				int oldSelected = mSelected;
				mSelected = mPosition;
				changeSelected(mSelected,
							   oldSelected,
							   (int) motionEvent.getX(),
							   (int) motionEvent.getY());
			}
			return false;
		}
	}

	private void changeSelected(int newSelected, int oldSelected, int x, int y) {
		if (newSelected == oldSelected) return;

		if (newSelected != -1) {
			View view = mLayoutManager.findViewByPosition(newSelected);
			if (view != null) {
				RelativeLayout articleWrapper = (RelativeLayout) view.findViewById(R.id.articleWrapper);
				articleWrapper.setSelected(true);

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					if (x == -1) {
						x = articleWrapper.getWidth() / 2;
					}
					if (y == -1) {
						y = articleWrapper.getHeight() / 2;
					}

					// since we're calculating x from articleInfo but animating articleWrapper,
					// move x over by width of articleStar
					ImageView articleStar = (ImageView) view.findViewById(R.id.articleStar);
					x += articleStar.getWidth();

					ViewAnimationUtils.createCircularReveal(articleWrapper,
															x,
															y,
															0,
															articleWrapper.getWidth()).start();
				}
			}
		}
		if (oldSelected != -1) {
			View view = mLayoutManager.findViewByPosition(oldSelected);
			if (view != null) {
				RelativeLayout articleWrapper = (RelativeLayout) view.findViewById(R.id.articleWrapper);
				articleWrapper.setSelected(false);
			}
		}
	}

	public void changeSelected(int position) {
		int oldSelected = mSelected;
		mSelected = position;

		changeSelected(mSelected,
					   oldSelected,
					   -1,
					   -1);

		mLayoutManager.scrollToPosition(position);

		// this function is called if the article pager changed to a new article
		// pager needs to fetch items ahead of end to get next article fragments ready
		int totalItemcount = mLayoutManager.getItemCount();
		int lastVisibleItem = mLayoutManager.findLastVisibleItemPosition();

		if (lastVisibleItem >= (totalItemcount - 4)) {
			getMoreArticles();
		}
	}

	public void updateUnreadStatus(String id, boolean unread) {
		// size of mItems includes footer view we need to ignore
		for (int i = 0; i < mItems.size() - 1; i++) {
			ArticleItem item = mItems.get(i);
			if (item.getId().equalsIgnoreCase(id)) {

				item.setUnread(unread);
				mAdapter.notifyItemChanged(i);
				break;
			}
		}
	}

	public void markAllAsRead() {
		// size of mItems includes footer view we need to ignore
		for (int i = 0; i < mItems.size() - 1; i++) {
			ArticleItem item = mItems.get(i);

			if (item.getUnread()) {
				item.setUnread(false);
				mAdapter.notifyItemChanged(i);
			}
		}
	}

	private class StarClickListener implements View.OnClickListener {

		private int position;

		public StarClickListener(int position) {
			this.position = position;
		}

		@SuppressWarnings("deprecation")
		@Override
		public void onClick(View view) {
			ArticleItem item = mItems.get(position);
			item.setStarred(!(item.getStarred()));

			mAdapter.notifyItemChanged(position);

			mCallback.onStarClicked(this.position, item.getStarred());
		}
	}

	public void updateStarredStatus(int position, Boolean starred) {
		ArticleItem item = mItems.get(position);

		if (item.getStarred() != starred) {
			item.setStarred(starred);
		}

		mAdapter.notifyItemChanged(position);
	}
}