package com.wilee8.coyotereader2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.transition.TransitionManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.wilee8.coyotereader2.containers.ArticleItem;
import com.wilee8.coyotereader2.containers.TagItem;
import com.wilee8.coyotereader2.gson.Category;
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

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;
import rx.Observable;
import rx.Subscriber;
import rx.android.app.AppObservable;
import rx.functions.Func5;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements NavFragment.NavFragmentListener,
															   FeedFragment.FeedFragmentListener,
															   ArticlePagerFragment.ArticlePagerFragmentListener,
															   ArticleFragment.ArticleFragmentListener {
	private Context mContext;

	private SharedPreferences mAuthPreferences;
	private String            mAuthToken;

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

	private ActionBar mActionBar;

	private Boolean   mDualPane;
	private int       mContentFrame;
	private ViewGroup mSceneRoot;

	private static int FRAME_IDS[] = {R.id.frame0, R.id.frame1, R.id.frame2};
	private FrameLayout[] mFrames;
	private String        mTitles[];

	private Boolean mShowRefresh;

	private Boolean              mShowMarkAllRead;
	private String               mMarkAllReadFeed;
	private long                 mUpdated;
	private FloatingActionButton mFab;

	private InoreaderService mService;
	private RequestQueue     mQueue;
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

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (toolbar != null) {
			setSupportActionBar(toolbar);
		}
		mActionBar = getSupportActionBar();
		if (mActionBar != null) {
			mActionBar.setDisplayShowHomeEnabled(true);
			mActionBar.setDisplayShowTitleEnabled(true);
		}

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
			}
			// else punt so we don't write over data from FeedFragment

			// if we don't have a feed, can't mark anything read
			if (savedInstanceState.containsKey("mMarkAllReadFeed")) {
				mShowMarkAllRead = savedInstanceState.getBoolean("mShowMarkAllRead", false);
				mMarkAllReadFeed = savedInstanceState.getString("mMarkAllReadFeed", "");
				mUpdated = savedInstanceState.getLong("mUpdated", -1);
			} else {
				mShowMarkAllRead = false;
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
			mShowMarkAllRead = false;
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
			// everything left of the content frame is gone
			for (int i = 0; i < mContentFrame; i++) {
				mFrames[i].setVisibility(View.GONE);
			}

			// content frame is visible
			mFrames[mContentFrame].setVisibility(View.VISIBLE);

			// everything right of the content frame is gone
			for (int i = mContentFrame + 1; i < FRAME_IDS.length; i++) {
				mFrames[i].setVisibility(View.GONE);
			}
		}

		if ((mActionBar != null) && (mContentFrame > 0)) {
			mActionBar.setDisplayHomeAsUpEnabled(true);
		}

		mActionBar.setTitle(mTitles[mContentFrame]);

		mFab = (FloatingActionButton) findViewById(R.id.feed_mark_all_read_button);
		if (mShowMarkAllRead) {
			mFab.setVisibility(View.VISIBLE);
		} else {
			mFab.setVisibility(View.GONE);
		}

		mFab.setOnClickListener(new MarkAllReadClickListener());

		mQueue = Volley.newRequestQueue(this);

		RequestInterceptor requestInterceptor = new RequestInterceptor() {
			@Override
			public void intercept(RequestFacade request) {
				request.addHeader("Authorization", "GoogleLogin auth=" + mAuthToken);
				request.addHeader("AppId", getString(R.string.app_id));
				request.addHeader("AppKey", getString(R.string.app_key));
			}
		};

		RestAdapter restAdapter = new RestAdapter.Builder()
			.setEndpoint("https://www.inoreader.com")
			.setRequestInterceptor(requestInterceptor)
			.build();

		mService = restAdapter.create(InoreaderService.class);

		if (needToFetchData) {
			FragmentManager fragmentManager = getSupportFragmentManager();

			Fragment fragment1 = new ProgressFragment();
			fragmentManager.beginTransaction().replace(R.id.frame0, fragment1).commit();

			InitFinishedSubscriber initFinishedSubscriber = new InitFinishedSubscriber();
			AppObservable.bindActivity(
				this,
				Observable.zip(mService.unreadCounts(),
							   mService.tagList(),
							   mService.subscriptionList(),
							   mService.userInfo(),
							   mService.streamPrefs(),
							   new ProcessDataZip())
					.subscribeOn(Schedulers.io()))
				.subscribe(initFinishedSubscriber);
		}
	}

	private class ProcessDataZip implements Func5<UnreadCounts, TagList, SubscriptionList, UserInfo, StreamPrefs, Void> {
		@Override
		public Void call(UnreadCounts unreadCounts, TagList tagList, SubscriptionList subscriptionList, UserInfo userInfo, StreamPrefs streamPrefs) {
			mUnreadCounts = unreadCounts;
			mTagList = tagList;
			mSubscriptionList = subscriptionList;
			mUserId = userInfo.getUserId();
			mStreamPrefs = streamPrefs;
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

			return null;
		}
	}

	private class InitFinishedSubscriber extends Subscriber<Void> {
		@Override
		public void onCompleted() {
		}

		@Override
		public void onError(Throwable throwable) {
			mQueue.cancelAll(mainActivityQueueTag);
			Snackbar
				.make(findViewById(R.id.sceneRoot),
					  R.string.error_fetch_data,
					  Snackbar.LENGTH_LONG)
				.setAction(R.string.action_refresh, new SnackbarRefreshOnClickListener())
				.show();
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

	private class SnackbarRefreshOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View view) {
			refreshOnClick();
		}
	}

	@Override
	public void onBackPressed() {
		if (mContentFrame == 0) {
			super.onBackPressed();
		} else {
			if (mDualPane) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
					TransitionManager.beginDelayedTransition(mSceneRoot);
				}

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
				crossfade(mFrames[mContentFrame - 1], mFrames[mContentFrame]);
			}

			// remove old fragment so it isn't taking up memory
			FragmentManager fm = getSupportFragmentManager();
			Fragment fragment = fm.findFragmentById(FRAME_IDS[mContentFrame]);
			fm.beginTransaction().remove(fragment).commit();

			mContentFrame--;

			if (mContentFrame == 0) {
				mShowMarkAllRead = false;
				mFab.setVisibility(View.GONE);
			}

			mActionBar.setTitle(mTitles[mContentFrame]);
		}

		if (mContentFrame == 0) {
			addRefreshButton();
			ActionBar ab = getSupportActionBar();
			if (ab != null) {
				ab.setDisplayHomeAsUpEnabled(false);
			}
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
				FragmentManager fm = getSupportFragmentManager();
				SettingsDialog fragment = new SettingsDialog();
				fragment.show(fm, null);

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

		if (mTitles != null) {
			outState.putStringArray("mTitles", mTitles);
		}

		outState.putBoolean("mShowMarkAllRead", mShowMarkAllRead);
		if (mShowMarkAllRead) {
			outState.putString("mMarkAllReadFeed", mMarkAllReadFeed);
			outState.putLong("mUpdated", mUpdated);
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
		AppObservable.bindActivity(
			this,
			Observable.zip(mService.unreadCounts(),
						   mService.tagList(),
						   mService.subscriptionList(),
						   mService.userInfo(),
						   mService.streamPrefs(),
						   new ProcessDataZip())
				.subscribeOn(Schedulers.io()))
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
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
					TransitionManager.beginDelayedTransition(mSceneRoot);
				}

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
			crossfade(mFrames[mContentFrame + 1], mFrames[mContentFrame]);

			mContentFrame++;
		}

		mTitles[mContentFrame] = title;
		mActionBar.setTitle(title);
		ActionBar ab = getSupportActionBar();
		if (ab != null) {
			ab.setDisplayHomeAsUpEnabled(true);
		}

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
	public void clearStreamContents() {
		if (mItems != null) {
			mItems.clear();
		}
	}

	@Override
	public void setFeedContents(ArrayList<ArticleItem> items, String id, long updated) {
		mItems = items;

		mMarkAllReadFeed = id;
		mUpdated = updated;
		mShowMarkAllRead = true;
		mFab.setVisibility(View.VISIBLE);

		// Update article pager - only exists if pager is content frame
		if (mContentFrame == 2) {
			ArticlePagerFragment fragment =
				(ArticlePagerFragment) getSupportFragmentManager().findFragmentById(FRAME_IDS[mContentFrame]);
			fragment.updateItems();
		}
	}

	@Override
	public String getUserId() {
		return mUserId;
	}

	@Override
	public void selectArticle(int position) {
		if (mItems == null) {
			// should never happen if we get to this point
			Snackbar
				.make(findViewById(R.id.sceneRoot),
					  R.string.error_null_items,
					  Snackbar.LENGTH_LONG)
				.show();
		}

		Bundle args = new Bundle();
		args.putInt("position", position);

		// article is always selected in frame 1
		if (mDualPane) {
			if (mContentFrame == 1) {
				// selection made in main content frame
				// need to shift frames
				Fragment fragment = new ArticlePagerFragment();
				fragment.setArguments(args);

				FragmentManager fragmentManager = getSupportFragmentManager();
				fragmentManager.beginTransaction().replace(FRAME_IDS[mContentFrame + 1], fragment).commit();
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
					TransitionManager.beginDelayedTransition(mSceneRoot);
				}

				// hide sidebar
				mFrames[mContentFrame - 1].setVisibility(View.GONE);

				// change weight of content frame
				LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f);
				mFrames[mContentFrame].setLayoutParams(lp);

				// unhide frame to the right of content frame
				lp = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 2.0f);
				mFrames[mContentFrame + 1].setLayoutParams(lp);
				mFrames[mContentFrame + 1].setVisibility(View.VISIBLE);

				mContentFrame++;
			} else {
				// selection made in side bar
				// just change article in main content frame
				ArticlePagerFragment fragment =
					(ArticlePagerFragment) getSupportFragmentManager().findFragmentById(FRAME_IDS[mContentFrame]);
				fragment.changeSelected(position);
			}
		} else {
			crossfade(mFrames[mContentFrame + 1], mFrames[mContentFrame]);

			Fragment fragment = new ArticlePagerFragment();
			fragment.setArguments(args);

			FragmentManager fragmentManager = getSupportFragmentManager();
			fragmentManager.beginTransaction()
				.replace(FRAME_IDS[mContentFrame + 1], fragment)
				.commit();

			mContentFrame++;
		}

		// for some reason the PageChangeListener isn't called if and only if the 0th element is selected
		// call the callback function if this is the case
		if (position == 0) {
			onArticleSelected(position);
		}
	}

	@Override
	public ArrayList<ArticleItem> getItems() {
		return mItems;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onArticleSelected(int position) {
		ArticleItem item = mItems.get(position);

		// update FeedFragment position
		FeedFragment fragment =
			(FeedFragment) getSupportFragmentManager().findFragmentById(FRAME_IDS[mContentFrame - 1]);
		fragment.changeSelected(position);

		mTitles[mContentFrame] = Html.fromHtml(item.getOrigin()).toString();
		mActionBar.setTitle(mTitles[mContentFrame]);

		// check if already unread before marking unread
		if (item.getUnread()) {
			Map queryMap = new HashMap<>();
			queryMap.put("a", "user/-/state/com.google/read");
			queryMap.put("i", item.getId());

			// mark item as read
			AppObservable.bindActivity(
				this,
				mService.editTag(queryMap)
					.lift(new GetUnreadCountsOperator())
					.lift(new UpdateUnreadCounts())
					.subscribeOn(Schedulers.io()))
				.subscribe(new UpdateUnreadDisplays());

			fragment.updateUnreadStatus(item.getId(), false);
		}
	}

	private class GetUnreadCountsOperator implements Observable.Operator<UnreadCounts, Response> {
		@Override
		public Subscriber<? super Response> call(final Subscriber<? super UnreadCounts> subscriber) {
			return new Subscriber<Response>() {
				@Override
				public void onCompleted() {
					subscriber.onCompleted();
				}

				@Override
				public void onError(Throwable e) {
					subscriber.onError(e);
				}

				@Override
				public void onNext(Response response) {
					String reponseBody = new String(((TypedByteArray) response.getBody()).getBytes());

					if (reponseBody.equalsIgnoreCase("OK")) {
						subscriber.onNext(mService.unreadCountsObject());
					} else {
						subscriber.onError(null);
					}
				}
			};
		}
	}

	private class UpdateUnreadCounts implements Observable.Operator<String, UnreadCounts> {
		@Override
		public Subscriber<? super UnreadCounts> call(final Subscriber<? super String> subscriber) {
			return new Subscriber<UnreadCounts>() {
				@Override
				public void onCompleted() {
					subscriber.onCompleted();
				}

				@Override
				public void onError(Throwable e) {
					subscriber.onError(e);
				}

				@Override
				public void onNext(UnreadCounts unreadCounts) {
					// update unread displays for every id that changed
					for (int i = 0; i < mNavList.size(); i++) {
						// check if unread count changed on top level item
						TagItem tagItem = mNavList.get(i);
						String id = tagItem.getId();

						int oldUnreadNumber = tagItem.getUnreadCount();
						int newUnreadNumber = unreadCounts.getUnreadCount(id);
						if (oldUnreadNumber != newUnreadNumber) {
							tagItem.setUnreadCount(newUnreadNumber);
							subscriber.onNext(id);
						}

						// update unread counts on child items
						ArrayList<TagItem> subNavList = tagItem.getFeeds();
						if (subNavList != null) {
							for (int j = 0; j < subNavList.size(); j++) {
								tagItem = subNavList.get(j);
								id = tagItem.getId();

								oldUnreadNumber = tagItem.getUnreadCount();
								newUnreadNumber = unreadCounts.getUnreadCount(id);
								if (oldUnreadNumber != newUnreadNumber) {
									tagItem.setUnreadCount(newUnreadNumber);
									subscriber.onNext(id);
								}
							}
						}
					}

					mUnreadCounts = unreadCounts;

					subscriber.onCompleted();
				}
			};
		}
	}

	private class UpdateUnreadDisplays extends Subscriber<String> {
		@Override
		public void onCompleted() {
			unsubscribe();
		}

		@Override
		public void onError(Throwable e) {
			Snackbar
				.make(findViewById(R.id.sceneRoot),
					  R.string.error_mark_unread,
					  Snackbar.LENGTH_LONG)
				.show();
		}

		@Override
		public void onNext(String id) {
			// Update NavFragment
			NavFragment fragment = (NavFragment) getSupportFragmentManager().findFragmentById(FRAME_IDS[0]);
			fragment.updateUnreadCount(id);
		}
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

	private class MarkAllReadClickListener implements View.OnClickListener {
		@Override
		public void onClick(View view) {
			if (mConfirm) {
				AlertDialog.Builder builder =
					new AlertDialog.Builder(mContext, R.style.MyAlertDialogStyle);

				builder.setMessage(R.string.alert_mark_all_as_read);

				builder.setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						markAllAsReadConfirmed();
					}
				});

				builder.setNegativeButton(R.string.alert_cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// nothing, just go back
					}
				});

				AlertDialog dialog = builder.create();
				dialog.show();
			} else {
				Snackbar
					.make(findViewById(R.id.sceneRoot),
						  R.string.notify_marking_all_read,
						  Snackbar.LENGTH_SHORT)
					.show();
				markAllAsReadConfirmed();
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void markAllAsReadConfirmed() {
		Map queryMap = new HashMap<>();
		queryMap.put("ts", Long.toString(mUpdated));
		queryMap.put("s", mMarkAllReadFeed);

		AppObservable.bindActivity(
			this,
			mService.markAllAsRead(queryMap)
				.lift(new GetUnreadCountsOperator())
				.lift(new UpdateUnreadCounts())
				.subscribeOn(Schedulers.io()))
			.subscribe(new UpdateUnreadDisplays());

		FeedFragment fragment =
			(FeedFragment) getSupportFragmentManager().findFragmentById(FRAME_IDS[1]);
		fragment.markAllAsRead();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onStarClicked(int position, Boolean starred) {
		// update FeedFragment star status
		FeedFragment feedFragment =
			(FeedFragment) getSupportFragmentManager().findFragmentById(FRAME_IDS[1]);
		feedFragment.updateStarredStatus(position, starred);

		// update ArticleFragment star status
		ArticlePagerFragment pagerFragment =
			(ArticlePagerFragment) getSupportFragmentManager().findFragmentById(FRAME_IDS[2]);
		if (pagerFragment != null) {
			pagerFragment.updateStarredStatus(position, starred);
		}

		ArticleItem item = mItems.get(position);

		Map queryMap = new HashMap<>();

		// item has already been updated, so set string to match status
		if (item.getStarred()) {
			queryMap.put("a", "user/-/state/com.google/starred");
		} else {
			queryMap.put("r", "user/-/state/com.google/starred");
		}
		queryMap.put("i", item.getId());

		AppObservable.bindActivity(
			this,
			mService.editTag(queryMap)
				.subscribeOn(Schedulers.io()))
			.subscribe(new StarredSubscriber());
	}

	private class StarredSubscriber extends Subscriber<Response> {

		@Override
		public void onCompleted() {
			unsubscribe();
		}

		@Override
		public void onError(Throwable e) {
			Snackbar
				.make(findViewById(R.id.sceneRoot),
					  R.string.error_update_starred,
					  Snackbar.LENGTH_LONG)
				.show();
		}

		@Override
		public void onNext(Response response) {
			String reponseBody = new String(((TypedByteArray) response.getBody()).getBytes());

			if (!reponseBody.equalsIgnoreCase("OK")) {
				onError(null);
			}

			onCompleted();
		}
	}

	private void crossfade(View fadeIn, final View fadeOut) {
		int shortAnimationDuration = getResources().getInteger(
			android.R.integer.config_shortAnimTime);

		// set fade in view so that it is visible but fully transparent
		fadeIn.setAlpha(0f);
		fadeIn.setVisibility(View.VISIBLE);

		// animate the fade in view to 100% opacity
		fadeIn.animate()
			.alpha(1f)
			.setDuration(shortAnimationDuration)
			.setListener(null);

		// animate the fade out view to 0% opacity, then set visibility to gone
		fadeOut.animate()
			.alpha(0f)
			.setDuration(shortAnimationDuration)
			.setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					super.onAnimationEnd(animation);
					fadeOut.setVisibility(View.GONE);
					fadeOut.setAlpha(1f);
				}
			});
	}
}