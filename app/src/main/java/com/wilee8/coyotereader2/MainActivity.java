package com.wilee8.coyotereader2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.wilee8.coyotereader2.containers.ArticleItem;
import com.wilee8.coyotereader2.containers.TagItem;
import com.wilee8.coyotereader2.gson.Category;
import com.wilee8.coyotereader2.gson.GsonRequest;
import com.wilee8.coyotereader2.gson.StreamPref;
import com.wilee8.coyotereader2.gson.StreamPrefs;
import com.wilee8.coyotereader2.gson.Subscription;
import com.wilee8.coyotereader2.gson.SubscriptionList;
import com.wilee8.coyotereader2.gson.Tag;
import com.wilee8.coyotereader2.gson.TagList;
import com.wilee8.coyotereader2.gson.UnreadCounts;
import com.wilee8.coyotereader2.gson.UserInfo;

import org.parceler.Parcels;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import rx.Observable;
import rx.Subscriber;
import rx.android.app.AppObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends ActionBarActivity implements NavFragment.NavFragmentListener,
															   FeedFragment.FeedFragmentListener {

	private SharedPreferences mAuthPreferences;
	private String            mAuthToken;
	private Context           mContext;

	private SharedPreferences mSettings;
	private Boolean           mSortAlpha;
	private Boolean           mShowUnreadOnly;
	private Boolean           mConfirm;
	private DefPrefListener   mPrefListener;

	private UnreadCounts       mUnreadCounts;
	private TagList            mTagList;
	private SubscriptionList   mSubscriptionList;
	private String             mUserId;
	private StreamPrefs        mStreamPrefs;
	private ArrayList<TagItem> mNavList;

	private ArrayList<ArticleItem> mItems;

	private Toolbar mToolbar;

	private Boolean   mDualPane;
	private int       mContentFrame;
	private ViewGroup mSceneRoot;

	private static int FRAME_IDS[] = {R.id.frame0, R.id.frame1, R.id.frame2, R.id.frame3};
	private FrameLayout[] mFrames;
	private String        mTitles[];

	private Boolean mShowRefresh;

	// needed to determine when all the main volley requests have returned and can be processed
	private static int NUMBER_MAIN_REQUESTS = 5;

	private RequestQueue mQueue;
	private final String mainActivityQueueTag = "MainActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = this;

		// Get the saved login credentials
		mAuthPreferences = getSharedPreferences(getString(R.string.auth_prefs), MODE_PRIVATE);
		mAuthToken = mAuthPreferences.getString(getString(R.string.auth_token), "");

		if ((mAuthToken == null) || (mAuthToken.equals(""))) {
			// logout and skip the rest of this function
			logout();
			return;
		}

		// get the saved settings
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		mSettings = PreferenceManager.getDefaultSharedPreferences(this);
		mSortAlpha = mSettings.getBoolean("pref_alpha", true);
		mShowUnreadOnly = mSettings.getBoolean("pref_unread", false);
		mConfirm = mSettings.getBoolean("pref_confirm", true);

		// listen for changed settings
		mPrefListener = new DefPrefListener();
		mSettings.registerOnSharedPreferenceChangeListener(mPrefListener);

		setContentView(R.layout.activity_main);

		mToolbar = (Toolbar) findViewById(R.id.toolbar);
		if (mToolbar != null) {
			setSupportActionBar(mToolbar);
		}
		getSupportActionBar().setDisplayShowHomeEnabled(true);

		Boolean needToFetchData = false;

		if (savedInstanceState != null) {
			mContentFrame = savedInstanceState.getInt("mContentFrame", 0);

			if (savedInstanceState.containsKey("mUnreadCounts")) {
				mUnreadCounts = Parcels.unwrap(savedInstanceState.getParcelable("mUnreadCounts"));
			} else {
				needToFetchData = true;
				mUnreadCounts = null;
			}

			if (savedInstanceState.containsKey("mTagList")) {
				mTagList = Parcels.unwrap(savedInstanceState.getParcelable("mTagList"));
			} else {
				needToFetchData = true;
				mTagList = null;
			}

			if (savedInstanceState.containsKey("mSubscriptionList")) {
				mSubscriptionList = Parcels.unwrap(savedInstanceState.getParcelable("mSubscriptionList"));
			} else {
				needToFetchData = true;
				mSubscriptionList = null;
			}

			if (savedInstanceState.containsKey("mUserId")) {
				mUserId = savedInstanceState.getString("mUserId");
			} else {
				needToFetchData = true;
				mUserId = null;
			}

			if (savedInstanceState.containsKey("mStreamPrefs")) {
				mStreamPrefs = Parcels.unwrap(savedInstanceState.getParcelable("mStreamPrefs"));
			} else {
				needToFetchData = true;
				mStreamPrefs = null;
			}

			if (savedInstanceState.containsKey("mNavList")) {
				mNavList = Parcels.unwrap(savedInstanceState.getParcelable("mNavList"));
			} else {
				needToFetchData = true;
				mNavList = null;
			}

			if (savedInstanceState.containsKey("mTitles")) {
				mTitles = savedInstanceState.getStringArray("mTitles");
			} else {
				mTitles = new String[FRAME_IDS.length];
				mTitles[0] = getResources().getString(R.string.app_name);
			}

			if (savedInstanceState.containsKey("mItems")) {
				mItems = Parcels.unwrap(savedInstanceState.getParcelable("mItems"));
			} else {
				mItems = null;
			}
		} else {
			needToFetchData = true;
			mContentFrame = 0;
			mUnreadCounts = null;
			mTagList = null;
			mSubscriptionList = null;
			mUserId = null;
			mStreamPrefs = null;
			mNavList = null;
			mTitles = new String[FRAME_IDS.length];
			mTitles[0] = getResources().getString(R.string.app_name);
			mItems = null;
		}

		mDualPane = getResources().getBoolean(R.bool.dual_pane);

		mSceneRoot = (ViewGroup) findViewById(R.id.sceneRoot);

		mShowRefresh = (mContentFrame == 0);

		mFrames = new FrameLayout[FRAME_IDS.length];

		for (int i = 0; i < FRAME_IDS.length; i++) {
			mFrames[i] = (FrameLayout) findViewById(FRAME_IDS[i]);
		}

		if (mDualPane) {
			LinearLayout.LayoutParams lp;

			// everything left of the sidebar is gone
			for (int i = 0; i < mContentFrame - 1; i++) {
				mFrames[i].setVisibility(View.GONE);
			}

			// no sidebar if mContentFrame is 0
			if (mContentFrame > 0) {
				// side bar frame is visible with weight of 1
				lp = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f);
				mFrames[mContentFrame - 1].setLayoutParams(lp);
				mFrames[mContentFrame - 1].setVisibility(View.VISIBLE);
			}

			// content frame is visible with weight of 2
			lp = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 2.0f);
			mFrames[mContentFrame].setLayoutParams(lp);
			mFrames[mContentFrame].setVisibility(View.VISIBLE);

			// everything right of the content frame is gone
			for (int i = mContentFrame + 1; i < FRAME_IDS.length; i++) {
				mFrames[i].setVisibility(View.GONE);
			}
		} else {
			LinearLayout.LayoutParams lp;

			// everything left of the content frame is gone
			for (int i = 0; i < mContentFrame; i++) {
				mFrames[i].setVisibility(View.GONE);
			}

			// content frame is visible with weight of 1
			lp = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f);
			mFrames[mContentFrame].setLayoutParams(lp);
			mFrames[mContentFrame].setVisibility(View.VISIBLE);

			// everything right of the content frame is gone
			for (int i = mContentFrame + 1; i < FRAME_IDS.length; i++) {
				mFrames[i].setVisibility(View.GONE);
			}
		}

		mQueue = Volley.newRequestQueue(this);

		if (needToFetchData) {
			FragmentManager fragmentManager = getSupportFragmentManager();

			Fragment fragment1 = new ProgressFragment();
			fragmentManager.beginTransaction().replace(R.id.frame0, fragment1).commit();

			InitFinishedSubscriber initFinishedSubscriber = new InitFinishedSubscriber();
			AppObservable.bindActivity(
				this,
				Observable.create(new FetchDataObserver())
					.skip(NUMBER_MAIN_REQUESTS - 1)
					.lift(new ProcessDataOperator())
					.subscribeOn(Schedulers.io()))
				.subscribe(initFinishedSubscriber);
		}
	}

	private class FetchDataObserver implements Observable.OnSubscribe<Void> {

		@Override
		public void call(final Subscriber<? super Void> subscriber) {
			String url;
			Map<String, String> headers = new HashMap<>();
			headers.put("Authorization", "GoogleLogin auth=" + mAuthToken);
			headers.put("AppId", getString(R.string.app_id));
			headers.put("AppKey", getString(R.string.app_key));

			// get unread counts
			if (mUnreadCounts == null) {
				url = "https://www.inoreader.com/reader/api/0/unread-count?output=json";
				@SuppressWarnings("unchecked")
				GsonRequest<UnreadCounts> unreadCountsGsonRequest =
					new GsonRequest<>(
						url, UnreadCounts.class, headers,
						new Response.Listener<UnreadCounts>() {
							@Override
							public void onResponse(UnreadCounts response) {
								mUnreadCounts = response;
								subscriber.onNext(null);
							}
						}, new Response.ErrorListener() {
						@Override
						public void onErrorResponse(VolleyError error) {
							subscriber.onError(error);
						}
					});

				unreadCountsGsonRequest.setTag(mainActivityQueueTag);
				mQueue.add(unreadCountsGsonRequest);
			} else {
				subscriber.onNext(null);
			}

			// get list of tags
			if (mTagList == null) {
				url = "https://www.inoreader.com/reader/api/0/tag/list";
				@SuppressWarnings("unchecked")
				GsonRequest<TagList> tagListGsonRequest =
					new GsonRequest<>(
						url, TagList.class, headers,
						new Response.Listener<TagList>() {
							@Override
							public void onResponse(TagList response) {
								mTagList = response;
								subscriber.onNext(null);
							}
						}, new Response.ErrorListener() {

						@Override
						public void onErrorResponse(VolleyError error) {
							subscriber.onError(error);
						}
					});

				tagListGsonRequest.setTag(mainActivityQueueTag);
				mQueue.add(tagListGsonRequest);
			} else {
				subscriber.onNext(null);
			}

			// get subscription list
			if (mSubscriptionList == null) {
				url = "https://www.inoreader.com/reader/api/0/subscription/list";
				@SuppressWarnings("unchecked")
				GsonRequest<SubscriptionList> subscriptionListGsonRequest =
					new GsonRequest<>(
						url, SubscriptionList.class, headers,
						new Response.Listener<SubscriptionList>() {
							@Override
							public void onResponse(SubscriptionList response) {
								mSubscriptionList = response;
								subscriber.onNext(null);
							}
						}, new Response.ErrorListener() {

						@Override
						public void onErrorResponse(VolleyError error) {
							subscriber.onError(error);
						}
					});

				subscriptionListGsonRequest.setTag(mainActivityQueueTag);
				mQueue.add(subscriptionListGsonRequest);
			} else {
				subscriber.onNext(null);
			}

			// get user info
			if (mUserId == null) {
				url = "https://www.inoreader.com/reader/api/0/user-info";
				@SuppressWarnings("unchecked")
				GsonRequest<UserInfo> userInfoGsonRequest =
					new GsonRequest<>(
						url, UserInfo.class, headers,
						new Response.Listener<UserInfo>() {
							@Override
							public void onResponse(UserInfo response) {
								mUserId = response.getUserId();
								subscriber.onNext(null);
							}
						}, new Response.ErrorListener() {
						@Override
						public void onErrorResponse(VolleyError error) {
							subscriber.onError(error);
						}
					});

				userInfoGsonRequest.setTag(mainActivityQueueTag);
				mQueue.add(userInfoGsonRequest);
			} else {
				subscriber.onNext(null);
			}

			// get stream preferences
			if (mStreamPrefs == null) {
				url = "https://www.inoreader.com/reader/api/0/preference/stream/list";
				@SuppressWarnings("unchecked")
				GsonRequest<StreamPrefs> streamPrefsGsonRequest =
					new GsonRequest<>(
						url, StreamPrefs.class, headers,
						new Response.Listener<StreamPrefs>() {
							@Override
							public void onResponse(StreamPrefs response) {
								mStreamPrefs = response;
								subscriber.onNext(null);
							}
						}, new Response.ErrorListener() {
						@Override
						public void onErrorResponse(VolleyError error) {
							subscriber.onError(error);
						}
					});

				streamPrefsGsonRequest.setTag(mainActivityQueueTag);
				mQueue.add(streamPrefsGsonRequest);
			} else {
				subscriber.onNext(null);
			}

		}
	}

	private class ProcessDataOperator implements Observable.Operator<Void, Void> {
		@Override
		public Subscriber<? super Void> call(final Subscriber<? super Void> s) {
			return new Subscriber<Void>(s) {
				@Override
				public void onCompleted() {
					if (!s.isUnsubscribed()) {
						s.onCompleted();
					}
				}

				@Override
				public void onError(Throwable throwable) {
					if (!s.isUnsubscribed()) {
						s.onError(throwable);
					}
				}

				@Override
				public void onNext(Void aVoid) {
					if (!s.isUnsubscribed()) {
						mNavList = new ArrayList<>();

						// set all items at the start
						TagItem tagItemAllItems = new TagItem();
						String allItemsId = "user/" + mUserId + "/state/com.google/reading-list";
						tagItemAllItems.setName("All Items");
						tagItemAllItems.setId(allItemsId);

						try {
							tagItemAllItems.setUnreadCount(mUnreadCounts.getUnreadCount(allItemsId));
						} catch (InvalidParameterException e) {
							tagItemAllItems.setUnreadCount(0);
						}
						tagItemAllItems.setIsFeed(true);
						tagItemAllItems.setIsTopLevel(true);

						tagItemAllItems.setResId(R.drawable.clear_favicon);

						mNavList.add(tagItemAllItems);

						// set starred
						TagItem tagItemStarred = new TagItem();
						String starredId = "user/" + mUserId + "/state/com.google/starred";
						tagItemStarred.setName("Favorites");
						tagItemStarred.setId(starredId);

						try {
							tagItemStarred.setUnreadCount(mUnreadCounts.getUnreadCount("user/-/state/com.google/starred"));
						} catch (InvalidParameterException e) {
							tagItemStarred.setUnreadCount(0);
						}
						tagItemStarred.setIsFeed(true);
						tagItemStarred.setIsTopLevel(true);

						tagItemStarred.setResId(R.drawable.ic_star_grey600_48dp);

						mNavList.add(tagItemStarred);

						// Add tags to tag list
						ArrayList<Tag> tags = mTagList.getTags();

						// skip first three tags
						for (int i = 3; i < tags.size(); i++) {
							Tag tag = tags.get(i);
							TagItem tagItem = new TagItem();

							String tagId = tag.getId();
							tagItem.setId(tagId);

							int lastSlash = tagId.lastIndexOf("/");
							tagItem.setName(tagId.substring(lastSlash + 1));

							tagItem.setUnreadCount(mUnreadCounts.getUnreadCount(tagId));
							tagItem.setIsFeed(false);
							tagItem.setIsTopLevel(true);

							tagItem.setResId(R.drawable.ic_folder_grey600_48dp);

							// Get feeds for this tag
							if (!tagItem.getIsFeed()) {
								ArrayList<TagItem> subFeedList = new ArrayList<>();
								tagItem.setFeeds(subFeedList);
								getTagFeeds(tagId, tagItem.getFeeds());
							} else {
								tagItem.setFeeds(null);
							}

							mNavList.add(tagItem);
						}

						// add untagged subscriptions
						ArrayList<Subscription> subscriptions = mSubscriptionList.getSubscriptions();

						for (int i = 0; i < subscriptions.size(); i++) {
							Subscription subscription = subscriptions.get(i);

							// only add a subscription to the nav bar if it has no tags
							if (subscription.getCategories().size() == 0) {
								TagItem tagItem = new TagItem();

								String tagId = subscription.getId();
								tagItem.setId(tagId);
								tagItem.setName(subscription.getTitle());
								tagItem.setUnreadCount(mUnreadCounts.getUnreadCount(tagId));
								tagItem.setIsFeed(true);
								tagItem.setIsTopLevel(true);
								tagItem.setIconUrl(subscription.getIconUrl());

								mNavList.add(tagItem);
							}
						}

						s.onNext(null);
					}
				}
			};
		}
	}

	private class InitFinishedSubscriber extends Subscriber<Void> {
		@Override
		public void onCompleted() {
		}

		@Override
		public void onError(Throwable throwable) {
			mQueue.cancelAll(mainActivityQueueTag);
			Toast.makeText(mContext, R.string.error_fetch_data, Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onNext(Void aVoid) {
			// start NavFragment
			Fragment navFragment = new NavFragment();
			FragmentManager fragmentManager = getSupportFragmentManager();
			fragmentManager.beginTransaction().replace(R.id.frame0, navFragment).commit();
		}
	}

	private void getTagFeeds(String tagId, ArrayList<TagItem> feedList) {
		ArrayList<Subscription> subscriptions = mSubscriptionList.getSubscriptions();
		ArrayList<StreamPref> preferences = mStreamPrefs.getStreamPrefs().get(tagId);

		String subscriptionOrderingString = "";

		if (!mSortAlpha) {
			for (int i = 0; i < preferences.size(); i++) {
				StreamPref pref = preferences.get(i);
				if (pref.getId().matches("subscription-ordering")) {
					subscriptionOrderingString = pref.getValue();
				}
			}
		}

		ArrayList<String> subscriptionOrdering = new ArrayList<>();

		if (!mSortAlpha) {
			for (int i = 0; i < subscriptionOrderingString.length() / 8; i++) {
				subscriptionOrdering.add(subscriptionOrderingString.substring(i * 8, (i * 8) + 8));
			}

			// Populate view data with blank items so we can replace them in
			// sort order
			TagItem blank = new TagItem();
			// <= if we need room for All Items row
			//for (int i = 0; i <= subscriptionOrdering.size(); i++) {
			for (int i = 0; i < subscriptionOrdering.size(); i++) {
				feedList.add(blank);
			}
		}

		// add tag feeds to the list
		for (int i = 0; i < subscriptions.size(); i++) {
			Subscription sub = subscriptions.get(i);

			ArrayList<Category> categories = sub.getCategories();

			for (int j = 0; j < categories.size(); j++) {
				Category category = categories.get(j);
				String subCategory = category.getId();
				if (subCategory.matches(tagId)) {
					// add to mViewData
					TagItem feedItem = new TagItem();

					String feedId = sub.getId();
					feedItem.setId(feedId);
					feedItem.setName(sub.getTitle());
					feedItem.setIconUrl(sub.getIconUrl());
					feedItem.setIsFeed(true);
					feedItem.setIsTopLevel(false);
					feedItem.setFeeds(null);

					try {
						feedItem.setUnreadCount(mUnreadCounts.getUnreadCount(feedId));
					} catch (InvalidParameterException e) {
						feedItem.setUnreadCount(0);
					}

					if (mSortAlpha) {
						feedList.add(feedItem);
					} else {
						// Get subscription ordering
						String ordering = sub.getSortId();
						for (int k = 0; k < subscriptionOrdering.size(); k++) {
							if (ordering.matches(subscriptionOrdering.get(k))) {
								feedList.set(k + 1, feedItem);
							}
						}
					}

					break;
				}
			}
		}

		// alphabetize the list if needed
		if (mSortAlpha) {
			Collections.sort(feedList, new Comparator<TagItem>() {

				@Override
				public int compare(TagItem lhs, TagItem rhs) {
					Locale locale = getResources().getConfiguration().locale;
					return lhs.getName().toLowerCase(locale).compareTo(rhs.getName().toLowerCase(locale));
				}
			});
		}
	}

	@Override
	public void onBackPressed() {
		if (mContentFrame == 0) {
			super.onBackPressed();
		} else {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				TransitionManager.beginDelayedTransition(mSceneRoot);
			}

			if (mDualPane) {
				LinearLayout.LayoutParams lp;

				// hide content frame
				mFrames[mContentFrame].setVisibility(View.GONE);

				// change weight of sidebar
				lp = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 2.0f);
				mFrames[mContentFrame - 1].setLayoutParams(lp);

				// no sidebar if mContentFrame will be 0
				if (mContentFrame > 1) {
					// unhide frame to the left of sidebar
					mFrames[mContentFrame - 2].setVisibility(View.VISIBLE);
				}
			} else {
				// hide content frame
				mFrames[mContentFrame].setVisibility(View.GONE);

				// unhide frame to the left of content frame
				mFrames[mContentFrame - 1].setVisibility(View.VISIBLE);

			}

			// remove old fragment so it isn't taking up memory
			FragmentManager fm = getSupportFragmentManager();
			Fragment fragment = fm.findFragmentById(FRAME_IDS[mContentFrame]);
			fm.beginTransaction().remove(fragment).commit();

			mContentFrame--;

			mToolbar.setTitle(mTitles[mContentFrame]);
		}

		if (mContentFrame == 0) {
			addRefreshButton();
			getSupportActionBar().setDisplayHomeAsUpEnabled(false);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		try {
			mSettings.unregisterOnSharedPreferenceChangeListener(mPrefListener);
		} catch (Exception e) {
			// can't really do anything, we're exiting
		}

		if (mQueue != null) {
			mQueue.cancelAll(mainActivityQueueTag);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		switch (id) {
			case R.id.action_settings:
				return true;
			case android.R.id.home:
				onBackPressed();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected boolean onPrepareOptionsPanel(View view, Menu menu) {

		// show refresh button if mShowRefresh is true
		menu.findItem(R.id.action_refresh).setVisible(mShowRefresh);
		return super.onPrepareOptionsPanel(view, menu);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putInt("mContentFrame", mContentFrame);

		if (mUnreadCounts != null) {
			outState.putParcelable("mUnreadCounts", Parcels.wrap(mUnreadCounts));
		}

		if (mTagList != null) {
			outState.putParcelable("mTagList", Parcels.wrap(mTagList));
		}

		if (mSubscriptionList != null) {
			outState.putParcelable("mSubscriptionList", Parcels.wrap(mSubscriptionList));
		}

		if (mUserId != null) {
			outState.putString("mUserId", mUserId);
		}

		if (mStreamPrefs != null) {
			outState.putParcelable("mStreamPrefs", Parcels.wrap(mStreamPrefs));
		}

		if (mNavList != null) {
			outState.putParcelable("mNavList", Parcels.wrap(mNavList));
		}

		if (mItems != null) {
			outState.putParcelable("mItems", Parcels.wrap(mItems));
		}
	}

	public void logout(MenuItem item) {
		logout();
	}

	private void logout() {
		// clear old auth token
		SharedPreferences.Editor editor = mAuthPreferences.edit();
		editor.remove(getString(R.string.auth_token));
		editor.apply();

		// launch login activity
		Intent intent = new Intent(this, LoginActivity.class);
		startActivity(intent);
		finish();

	}

	public void refreshOnClick(MenuItem item) {
		refreshOnClick();
	}

	private void refreshOnClick() {
		// put progress fragment in frame 0
		FragmentManager fragmentManager = getSupportFragmentManager();

		Fragment fragment1 = new ProgressFragment();
		fragmentManager.beginTransaction().replace(R.id.frame0, fragment1).commit();

		// reset all data and reload
		mUnreadCounts = null;
		mTagList = null;
		mSubscriptionList = null;
		mUserId = null;
		mStreamPrefs = null;

		InitFinishedSubscriber initFinishedSubscriber = new InitFinishedSubscriber();
		Observable.create(new FetchDataObserver())
			.skip(NUMBER_MAIN_REQUESTS - 1)
			.lift(new ProcessDataOperator())
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(initFinishedSubscriber);
	}

	@Override
	public ArrayList<TagItem> getNavList() {
		return mNavList;
	}

	@Override
	public RequestQueue getRequestQueue() {
		return mQueue;
	}

	@Override
	public void selectNav(String id, String title) {
		// NavFragment only ever shows up in frame0
		// Open selected tag in frame1

		Bundle args = new Bundle();
		args.putString("id", id);

		FeedFragment fragment = new FeedFragment();
		fragment.setArguments(args);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			TransitionManager.beginDelayedTransition(mSceneRoot);
		}

		FragmentManager fragmentManager = getSupportFragmentManager();
		Fragment oldFragment = fragmentManager.findFragmentById(FRAME_IDS[1]);
		if (oldFragment == null) {
			fragmentManager.beginTransaction()
				.replace(FRAME_IDS[1], fragment)
				.commit();
		} else {
			fragmentManager.beginTransaction()
				.remove(oldFragment)
				.replace(FRAME_IDS[1], fragment)
				.commit();
		}

		if (mDualPane) {
			if (mContentFrame == 0) {
				// change weight of content frame
				LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
																			 LayoutParams.MATCH_PARENT,
																			 1.0f);
				mFrames[mContentFrame].setLayoutParams(lp);

				// unhide frame to the right of content frame
				lp = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 2.0f);
				mFrames[mContentFrame + 1].setLayoutParams(lp);
				mFrames[mContentFrame + 1].setVisibility(View.VISIBLE);

				mContentFrame++;
			}
			// else no need to shift frames or increment mContentFrame
		} else {
			// hide content frame
			mFrames[mContentFrame].setVisibility(View.GONE);

			// unhide frame to the right of content frame
			mFrames[mContentFrame + 1].setVisibility(View.VISIBLE);

			mContentFrame++;
		}

		mTitles[mContentFrame] = title;
		mToolbar.setTitle(title);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		removeRefreshButton();
	}

	@Override
	public String getAuthToken() {
		return mAuthToken;
	}

	@Override
	public Boolean getUnreadOnly() {
		return mShowUnreadOnly;
	}

	@Override
	public RequestQueue getQueue() {
		return mQueue;
	}

	@Override
	public void clearStreamContents() {
		if (mItems != null) {
			mItems.clear();
		}
	}

	@Override
	public void setFeedContents(ArrayList<ArticleItem> items) {
		mItems = items;
	}

	@Override
	public String getUserId() {
		return mUserId;
	}

	private void addRefreshButton() {
		mShowRefresh = true;

		invalidateOptionsMenu();
		supportInvalidateOptionsMenu();
	}

	private void removeRefreshButton() {
		mShowRefresh = false;

		invalidateOptionsMenu();
		supportInvalidateOptionsMenu();
	}

	public class DefPrefListener implements OnSharedPreferenceChangeListener {

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if (key.matches("pref_alpha")) {
				mSortAlpha = sharedPreferences.getBoolean("pref_alpha", true);
			} else if (key.matches("pref_unread")) {
				mShowUnreadOnly = sharedPreferences.getBoolean("pref_unread", false);
			} else if (key.matches("pref_confirm")) {
				mConfirm = sharedPreferences.getBoolean("pref_confirm", false);
			}
		}

	}
}
