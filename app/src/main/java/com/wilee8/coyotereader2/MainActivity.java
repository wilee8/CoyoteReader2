package com.wilee8.coyotereader2;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.ShareActionProvider;
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

import com.trello.rxlifecycle.components.support.RxAppCompatActivity;
import com.wilee8.coyotereader2.accounts.AccountAuthenticator;
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
import com.wilee8.coyotereader2.retrofitservices.HeaderInterceptor;
import com.wilee8.coyotereader2.retrofitservices.InoreaderGsonService;
import com.wilee8.coyotereader2.retrofitservices.InoreaderRxGsonService;
import com.wilee8.coyotereader2.retrofitservices.InoreaderRxService;

import org.parceler.Parcels;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func5;
import rx.schedulers.Schedulers;

public class MainActivity extends RxAppCompatActivity implements NavFragment.NavFragmentListener,
																 FeedFragment.FeedFragmentListener,
																 ArticlePagerFragment.ArticlePagerFragmentListener,
																 AddSubscriptionDialog.AddSubscriptionListener,
																 ChangeSubscriptionFolderDialog.ChangeSubsciptionFolderListener,
																 ChangeNameDialog.ChangeSubscriptionNameListener,
																 NewFolderDialog.NewFolderListener {
	private Context mContext;

	private AccountManager mAccountManager;

	private SharedPreferences mSettings;
	private Boolean           mSortAlpha;
	private Boolean           mShowUnreadOnly;
	private Boolean           mConfirm;
	private Boolean           mAdvance;
	private String            mBrowser;
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

	private static int FRAME_IDS[]            = {R.id.frame0, R.id.frame1, R.id.frame2};
	// constant for what frame to go back to when automatically advancing, points to frame1
	@SuppressWarnings("FieldCanBeLocal")
	private static int NAV_FRAGMENT_FRAME     = 0;
	private static int FEED_FRAGMENT_FRAME    = 1;
	private static int ARTICLE_FRAGMENT_FRAME = 2;
	private FrameLayout[] mFrames;
	private String        mTitles[];

	private Boolean              mShowRefresh;
	private Boolean              mShowMarkAllRead;
	private Boolean              mShowMarkUnread;
	private int                  mMarkUnreadPosition;
	private String               mMarkAllReadFeed;
	private String               mMarkAllReadFeedName;
	private long                 mUpdated;
	private FloatingActionButton mFab;
	private ShareActionProvider  mShareActionProvider;
	private String               mShareUrl;

	private InoreaderRxGsonService mRxGsonService;
	private InoreaderRxService     mRxService;
	private InoreaderGsonService   mGsonService;

	@SuppressWarnings("FieldCanBeLocal")
	private static String PACKAGE_NAME = "com.android.chrome";
	private CustomTabsServiceConnection mCustomTabsServiceConnection;
	private CustomTabsClient            mCustomTabsClient;
	private CustomTabsSession           mCustomTabsSession;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = this;

		mAccountManager = AccountManager.get(this);
		Account[] accounts = mAccountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);

		if (accounts.length == 0) {
			Intent intent = new Intent(mContext, LoginActivity.class);
			startActivity(intent);
			finish();
			return;
		}

		// get the saved settings
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		mSettings = PreferenceManager.getDefaultSharedPreferences(this);
		mSortAlpha = mSettings.getBoolean("pref_alpha", true);
		mShowUnreadOnly = mSettings.getBoolean("pref_unread", false);
		mConfirm = mSettings.getBoolean("pref_confirm", true);
		mAdvance = mSettings.getBoolean("pref_advance", false);
		mBrowser = mSettings.getString("pref_browser",
			getResources().getString(R.string.pref_browser_default_value));

		// listen for changed settings
		mPrefListener = new DefPrefListener();
		mSettings.registerOnSharedPreferenceChangeListener(mPrefListener);

		setContentView(R.layout.activity_main);

		Boolean needToFetchData = false;

		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey("mContentFrame")) {
				mContentFrame = savedInstanceState.getInt("mContentFrame");
				mShowMarkUnread = mContentFrame == ARTICLE_FRAGMENT_FRAME;
				mMarkUnreadPosition = savedInstanceState.getInt("mMarkUnreadPosition", -1);
			} else {
				mContentFrame = NAV_FRAGMENT_FRAME;
				mShowMarkUnread = false;
				mMarkUnreadPosition = -1;
			}

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
				mMarkAllReadFeedName = savedInstanceState.getString("mMarkAllReadFeedName", "");
				mUpdated = savedInstanceState.getLong("mUpdated", -1);
			} else {
				mShowMarkAllRead = false;
			}

			mShareUrl = savedInstanceState.getString("mShareUrl", null);
		} else {
			needToFetchData = true;
			mContentFrame = NAV_FRAGMENT_FRAME;
			mUnreadCounts = null;
			mTagList = null;
			mSubscriptionList = null;
			mUserId = null;
			mStreamPrefs = null;
			mNavList = null;
			mTitles = new String[FRAME_IDS.length];
			mTitles[0] = getResources().getString(R.string.app_name);
			mShowMarkAllRead = false;
			mShowMarkUnread = false;
			mMarkUnreadPosition = -1;
			mShareUrl = null;
		}

		mDualPane = getResources().getBoolean(R.bool.dual_pane);

		mSceneRoot = (ViewGroup) findViewById(R.id.sceneRoot);

		mShowRefresh = (mContentFrame == NAV_FRAGMENT_FRAME);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (toolbar != null) {
			setSupportActionBar(toolbar);
		}
		mActionBar = getSupportActionBar();
		if (mActionBar != null) {
			mActionBar.setDisplayShowHomeEnabled(true);
			mActionBar.setDisplayShowTitleEnabled(true);
		}

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

		// start custom tabs if needed
		if (mBrowser.matches(
			mContext.getResources().getString(R.string.pref_browser_chrome_tabs))) {
			startCustomTabs();
		}

		createRetrofitServices();

		if (needToFetchData) {
			FragmentManager fragmentManager = getSupportFragmentManager();

			Fragment fragment1 = new ProgressFragment();
			fragmentManager.beginTransaction().replace(R.id.frame0, fragment1).commit();

			InitFinishedSubscriber initFinishedSubscriber = new InitFinishedSubscriber();
			Observable.zip(mRxGsonService.unreadCounts(),
				mRxGsonService.tagList(),
				mRxGsonService.subscriptionList(),
				mRxGsonService.userInfo(),
				mRxGsonService.streamPrefs(),
				new ProcessDataZip())
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.compose(this.<Void>bindToLifecycle())
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

			tagItemStarred.setResId(R.drawable.ic_star_24dp);

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

				tagItem.setResId(R.drawable.ic_folder_24dp);

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

			for (Subscription subscription : subscriptions) {
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
			for (StreamPref pref : preferences) {
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
		for (Subscription sub : subscriptions) {
			ArrayList<Category> categories = sub.getCategories();

			for (Category category : categories) {
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
		if (mContentFrame == NAV_FRAGMENT_FRAME) {
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

			if (mContentFrame == NAV_FRAGMENT_FRAME) {
				mShowMarkAllRead = false;
				mFab.setVisibility(View.GONE);
			}

			mActionBar.setTitle(mTitles[mContentFrame]);

			mShowMarkUnread = false;
			mMarkUnreadPosition = -1;
		}

		mShareUrl = null;
		invalidateOptionsMenu();
		supportInvalidateOptionsMenu();

		if (mContentFrame == NAV_FRAGMENT_FRAME) {
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

		stopCustomTabs();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);

		// get the share menu item so we can add the share action
		MenuItem item = menu.findItem(R.id.action_article_share);
		mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);

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

				Bundle args = new Bundle();
				if (!mDualPane) {
					View view = findViewById(FRAME_IDS[mContentFrame]);
					args.putInt("width", view.getWidth());
				}

				fragment.setArguments(args);
				fragment.show(fm, null);

				return true;
			case R.id.action_article_mark_unread:
				markUnread();
				return true;
			case R.id.action_feed_unsubscribe:
				unsubscribe();
				return true;
			case R.id.action_feed_change_folders:
				changeFoldersOnClick();
				return true;
			case R.id.action_feed_change_name:
				changeNameOnClick(true);
				return true;
			case android.R.id.home:
				onBackPressed();
				return true;
			case R.id.action_folder_change_name:
				changeNameOnClick(false);
				return true;
			case R.id.action_folder_delete:
				deleteFolder();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	protected boolean onPrepareOptionsPanel(View view, Menu menu) {

		// show refresh button if mShowRefresh is true
		menu.findItem(R.id.action_refresh).setVisible(mShowRefresh);
		// both refresh and add are shown only on first content frame
		menu.findItem(R.id.action_add).setVisible(mShowRefresh);

		menu.findItem(R.id.action_article_mark_unread).setVisible(mShowMarkUnread);

		MenuItem menuItem = menu.findItem(R.id.action_article_share);

		if (mShareUrl != null) {
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
			} else {
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			}
			intent.putExtra(Intent.EXTRA_TEXT, mShareUrl);
			mShareActionProvider.setShareIntent(intent);

			menuItem.setVisible(true);
		} else {
			menuItem.setVisible(false);
		}

		// since the "mark all read" feed will be the same as the one to unsubscribe from, reuse it
		menu.findItem(R.id.action_feed_unsubscribe)
			.setVisible((mContentFrame == FEED_FRAGMENT_FRAME)
						&& (mMarkAllReadFeed.startsWith("feed")));

		// only show change folders and name at the same time unsubscribe is visible
		menu.findItem(R.id.action_feed_change_folders)
			.setVisible((mContentFrame == FEED_FRAGMENT_FRAME)
						&& (mMarkAllReadFeed.startsWith("feed")));

		menu.findItem(R.id.action_feed_change_name)
			.setVisible((mContentFrame == FEED_FRAGMENT_FRAME)
						&& (mMarkAllReadFeed.startsWith("feed")));

		// only show change folder name if in feed fragment but not a feed
		menu.findItem(R.id.action_folder_change_name)
			.setVisible((mContentFrame == FEED_FRAGMENT_FRAME)
						&& (!mMarkAllReadFeed.startsWith("feed")));

		menu.findItem(R.id.action_folder_delete)
			.setVisible((mContentFrame == FEED_FRAGMENT_FRAME)
						&& (!mMarkAllReadFeed.startsWith("feed")));

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

		if (mShareUrl != null) {
			outState.putString("mShareUrl", mShareUrl);
		}

		outState.putBoolean("mShowMarkAllRead", mShowMarkAllRead);
		if (mShowMarkAllRead) {
			outState.putString("mMarkAllReadFeed", mMarkAllReadFeed);
			outState.putString("mMarkAllReadFeedName", mMarkAllReadFeedName);
			outState.putLong("mUpdated", mUpdated);
		}

		outState.putInt("mMarkUnreadPosition", mMarkUnreadPosition);
	}

	public void logout(MenuItem item) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setMessage(R.string.alert_logout);

		builder.setPositiveButton(
			R.string.alert_ok,
			new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					logout();
				}
			});

		builder.setNegativeButton(R.string.alert_cancel, null);

		builder.show();
	}

	private void logout() {
		// log out in background task
		HandleLogoutResult handleLogoutResult = new HandleLogoutResult();
		Observable<Void> doRemoveAccount = Observable.create(new DoRemoveAccount());

		doRemoveAccount
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.compose(this.<Void>bindToLifecycle())
			.subscribe(handleLogoutResult);
	}

	public void refreshOnClick(MenuItem item) {
		refreshOnClick();
	}

	public void refreshOnClick() {
		// go back to nav frame
		while (mContentFrame > NAV_FRAGMENT_FRAME) {
			onBackPressed();
		}

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
		Observable.zip(mRxGsonService.unreadCounts(),
			mRxGsonService.tagList(),
			mRxGsonService.subscriptionList(),
			mRxGsonService.userInfo(),
			mRxGsonService.streamPrefs(),
			new ProcessDataZip())
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.compose(this.<Void>bindToLifecycle())
			.subscribe(initFinishedSubscriber);
	}

	public void addOnClick(MenuItem item) {
		FragmentManager fm = getSupportFragmentManager();
		AddSubscriptionDialog fragment = new AddSubscriptionDialog();
		fragment.show(fm, null);
	}

	@Override
	public ArrayList<TagItem> getNavList() {
		return mNavList;
	}

	@Override
	public SubscriptionList getSubscriptionList() {
		return mSubscriptionList;
	}

	@Override
	public void addNewFolder(ArrayList<String> newFolderList) {
		Bundle args = new Bundle();
		// mMarkAllRead feed will be the same as the feed we wish to change
		args.putStringArrayList("newFolderList", newFolderList);

		FragmentManager fm = getSupportFragmentManager();
		NewFolderDialog fragment = new NewFolderDialog();
		fragment.setArguments(args);
		fragment.show(fm, null);


	}

	@Override
	public void launchChangeFolder(ArrayList<String> newFolderList) {
		Bundle args = new Bundle();
		// mMarkAllRead feed will be the same as the feed we wish to change
		args.putString("id", mMarkAllReadFeed);
		args.putStringArrayList("newFolderList", newFolderList);

		FragmentManager fm = getSupportFragmentManager();
		ChangeSubscriptionFolderDialog fragment = new ChangeSubscriptionFolderDialog();
		fragment.setArguments(args);
		fragment.show(fm, null);
	}

	@Override
	public void selectNav(String id, String title) {
		// in case of advance after marking all as read, roll back to FeedFragment in content frame
		while (mContentFrame > FEED_FRAGMENT_FRAME) {
			onBackPressed();
		}

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
			if (mContentFrame == NAV_FRAGMENT_FRAME) {
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
			if (mContentFrame == NAV_FRAGMENT_FRAME) {
				crossfade(mFrames[mContentFrame + 1], mFrames[mContentFrame]);

				mContentFrame++;
			}
		}

		mMarkAllReadFeed = id;
		mMarkAllReadFeedName = title;
		mTitles[mContentFrame] = title;
		mActionBar.setTitle(title);
		ActionBar ab = getSupportActionBar();
		if (ab != null) {
			ab.setDisplayHomeAsUpEnabled(true);
		}

		removeRefreshButton();
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

		mUpdated = updated;
		mShowMarkAllRead = true;
		mFab.setVisibility(View.VISIBLE);

		// Update article pager - only exists if pager is content frame
		if (mContentFrame == ARTICLE_FRAGMENT_FRAME) {
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
			if (mContentFrame == FEED_FRAGMENT_FRAME) {
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

	@Override
	public void onArticleSelected(int position) {
		ArticleItem item = mItems.get(position);

		// update FeedFragment position
		FeedFragment fragment =
			(FeedFragment) getSupportFragmentManager().findFragmentById(FRAME_IDS[mContentFrame - 1]);
		fragment.changeSelected(position);

		mTitles[mContentFrame] = Html.fromHtml(item.getOrigin()).toString();
		mActionBar.setTitle(mTitles[mContentFrame]);

		if (mCustomTabsSession != null) {
			mCustomTabsSession.mayLaunchUrl(Uri.parse(item.getCanonical()), null, null);
		}

		updateArticleUnreadStatus(item, false);

		mShareUrl = item.getCanonical();
		mShowMarkUnread = true;
		mMarkUnreadPosition = position;
		invalidateOptionsMenu();
		supportInvalidateOptionsMenu();
	}

	private void markUnread() {
		if (mMarkUnreadPosition != -1) {
			updateArticleUnreadStatus(mItems.get(mMarkUnreadPosition), true);
		}
	}

	private void updateArticleUnreadStatus(ArticleItem item, boolean unread) {
		// check if already unread before marking unread
		if (item.getUnread() != unread) {
			Map<String, String> queryMap = new ArrayMap<>();
			if (unread) {
				queryMap.put("r", "user/-/state/com.google/read");
			} else {
				queryMap.put("a", "user/-/state/com.google/read");
			}
			queryMap.put("i", item.getId());

			// mark item as read
			UpdateUnreadDisplays updateUnreadDisplays = new UpdateUnreadDisplays();
			mRxService.editTag(queryMap)
				.lift(new GetUnreadCountsOperator())
				.lift(new UpdateUnreadCounts())
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.compose(this.<String>bindToLifecycle())
				.subscribe(updateUnreadDisplays);

			FeedFragment fragment =
				(FeedFragment) getSupportFragmentManager().findFragmentById(FRAME_IDS[mContentFrame - 1]);
			fragment.updateUnreadStatus(item.getId(), unread);
		}
	}

	private class GetUnreadCountsOperator implements Observable.Operator<UnreadCounts, ResponseBody> {
		@Override
		public Subscriber<? super ResponseBody> call(final Subscriber<? super UnreadCounts> subscriber) {
			return new Subscriber<ResponseBody>() {
				@Override
				public void onCompleted() {
					subscriber.onCompleted();
				}

				@Override
				public void onError(Throwable e) {
					subscriber.onError(e);
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
						Call<UnreadCounts> call = mGsonService.unreadCountsObject();
						try {
							subscriber.onNext(call.execute().body());
						} catch (IOException e) {
							subscriber.onError(e);
						}
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
					for (TagItem tagItem : mNavList) {
						// Skip updating unread for favorites
						if (!tagItem.getId().matches("user/" + mUserId + "/state/com.google/starred")) {
							// check if unread count changed on top level item
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
								for (TagItem subTagItem : subNavList) {
									id = subTagItem.getId();

									oldUnreadNumber = subTagItem.getUnreadCount();
									newUnreadNumber = unreadCounts.getUnreadCount(id);
									if (oldUnreadNumber != newUnreadNumber) {
										subTagItem.setUnreadCount(newUnreadNumber);
										subscriber.onNext(id);
									}
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

		private boolean advance;

		public UpdateUnreadDisplays() {
			advance = false;
		}

		public UpdateUnreadDisplays(boolean advance) {
			this.advance = advance;
		}

		@Override
		public void onCompleted() {

			// Now that we're done updating unread count, advance to next unread feed if necessary
			if (advance) {
				FragmentManager fm = getSupportFragmentManager();
				NavFragment fragment = (NavFragment) fm.findFragmentById(FRAME_IDS[0]);
				fragment.advanceToNextUnreadFeed();
			}

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
				mConfirm = sharedPreferences.getBoolean("pref_confirm", true);
			} else if (key.matches("pref_advance")) {
				mAdvance = sharedPreferences.getBoolean("pref_advance", false);
			} else if (key.matches("pref_browser")) {
				mBrowser = sharedPreferences.getString("pref_browser",
					getResources().getString(R.string.pref_browser_default_value));
				if (mBrowser.matches(
					mContext.getResources().getString(R.string.pref_browser_default_value))) {
					startCustomTabs();
				} else {
					stopCustomTabs();
				}
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

	private void markAllAsReadConfirmed() {
		Map<String, String> queryMap = new ArrayMap<>();
		queryMap.put("ts", Long.toString(mUpdated));
		queryMap.put("s", mMarkAllReadFeed);

		UpdateUnreadDisplays updateUnreadDisplays = new UpdateUnreadDisplays(mAdvance);
		mRxService.markAllAsRead(queryMap)
			.lift(new GetUnreadCountsOperator())
			.lift(new UpdateUnreadCounts())
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.compose(this.<String>bindToLifecycle())
			.subscribe(updateUnreadDisplays);

		FeedFragment fragment =
			(FeedFragment) getSupportFragmentManager().findFragmentById(FRAME_IDS[1]);
		fragment.markAllAsRead();
	}

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

		Map<String, String> queryMap = new ArrayMap<>();

		// item has already been updated, so set string to match status
		if (item.getStarred()) {
			queryMap.put("a", "user/-/state/com.google/starred");
		} else {
			queryMap.put("r", "user/-/state/com.google/starred");
		}
		queryMap.put("i", item.getId());

		StarredSubscriber starredSubscriber = new StarredSubscriber();
		mRxService.editTag(queryMap)
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.compose(this.<ResponseBody>bindToLifecycle())
			.subscribe(starredSubscriber);
	}

	@Override
	public InoreaderRxGsonService getRxGsonService() {
		return mRxGsonService;
	}

	@Override
	public InoreaderRxService getRxService() {
		return mRxService;
	}

	@Override
	public CustomTabsSession getCustomTabsSession() {
		return mCustomTabsSession;
	}

	@Override
	public String getBrowser() {
		return mBrowser;
	}

	private class StarredSubscriber extends Subscriber<ResponseBody> {

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

			unsubscribe();
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

			if (!response.equalsIgnoreCase("OK")) {
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

	private void createRetrofitServices() {
		OkHttpClient client = new OkHttpClient.Builder()
			.addInterceptor(new HeaderInterceptor(mAccountManager, MainActivity.this))
			.build();

		Retrofit restAdapter = new Retrofit.Builder()
			.baseUrl("https://www.inoreader.com")
			.client(client)
			.addConverterFactory(GsonConverterFactory.create())
			.addCallAdapterFactory(RxJavaCallAdapterFactory.create())
			.build();

		mRxGsonService = restAdapter.create(InoreaderRxGsonService.class);

		restAdapter = new Retrofit.Builder()
			.baseUrl("https://www.inoreader.com")
			.client(client)
			.addCallAdapterFactory(RxJavaCallAdapterFactory.create())
			.build();

		mRxService = restAdapter.create(InoreaderRxService.class);

		restAdapter = new Retrofit.Builder()
			.baseUrl("https://www.inoreader.com")
			.client(client)
			.addConverterFactory(GsonConverterFactory.create())
			.build();

		mGsonService = restAdapter.create(InoreaderGsonService.class);
	}

	private class DoRemoveAccount implements Observable.OnSubscribe<Void> {

		@SuppressWarnings("deprecation")
		@Override
		public void call(Subscriber<? super Void> subscriber) {
			Account[] accounts = mAccountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);

			for (Account account : accounts) {

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
					AccountManagerFuture<Bundle> accountManagerFuture =
						mAccountManager.removeAccount(account, null, null, null);

					Bundle result;
					try {
						result = accountManagerFuture.getResult();
					} catch (OperationCanceledException | IOException | AuthenticatorException e) {
						subscriber.onError(e);
						return;
					}

					Boolean success =
						result.getBoolean(AccountManager.KEY_BOOLEAN_RESULT, Boolean.FALSE);

					if (success) {
						// account was successfully removed
						subscriber.onNext(null);
					} else {
						subscriber.onError(null);
					}

					subscriber.onCompleted();
				} else {
					// this is deprecated, but need something for before API 22
					AccountManagerFuture<Boolean> accountManagerFuture =
						mAccountManager.removeAccount(account, null, null);

					Boolean result;
					try {
						result = accountManagerFuture.getResult();
					} catch (OperationCanceledException | IOException | AuthenticatorException e) {
						subscriber.onError(e);
						return;
					}

					if (result) {
						// account was successfully removed
						subscriber.onNext(null);
					} else {
						subscriber.onError(null);
					}
				}
			}

			subscriber.onCompleted();
		}
	}

	private class HandleLogoutResult extends Subscriber<Void> {

		@Override
		public void onCompleted() {
			// launch login activity
			Intent intent = new Intent(mContext, LoginActivity.class);
			startActivity(intent);
			finish();
			unsubscribe();
		}

		@Override
		public void onError(Throwable e) {
			Snackbar
				.make(findViewById(R.id.sceneRoot),
					R.string.error_login,
					Snackbar.LENGTH_LONG)
				.show();
		}

		@Override
		public void onNext(Void aVoid) {
			// do nothing
		}
	}

	private void startCustomTabs() {
		mCustomTabsServiceConnection = new CustomTabsServiceConnection() {
			@Override
			public void onCustomTabsServiceConnected(ComponentName componentName, CustomTabsClient customTabsClient) {
				mCustomTabsClient = customTabsClient;

				mCustomTabsClient.warmup(0L);
				mCustomTabsSession = mCustomTabsClient.newSession(null);
			}

			@Override
			public void onServiceDisconnected(ComponentName componentName) {
			}
		};

		if (!CustomTabsClient.bindCustomTabsService(this, PACKAGE_NAME, mCustomTabsServiceConnection)) {
			mCustomTabsServiceConnection = null;
		}
	}

	private void stopCustomTabs() {
		if (mCustomTabsServiceConnection != null) {
			unbindService(mCustomTabsServiceConnection);
		}
		mCustomTabsServiceConnection = null;
		mCustomTabsClient = null;
		mCustomTabsSession = null;
	}

	private void unsubscribe() {
		final RxAppCompatActivity activity = this;
		new AlertDialog.Builder(this, R.style.MyAlertDialogStyle)
			.setTitle(R.string.alert_unsubscribe_title)
			.setMessage(R.string.alert_unsubscribe_prompt)
			.setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialogInterface, int i) {
					// since the "mark all read" feed will be the same as the one to unsubscribe from, reuse it
					Map<String, String> queryMap = new ArrayMap<>();
					queryMap.put("ac", "unsubscribe");
					queryMap.put("s", mMarkAllReadFeed);

					UnsubscribeSubscriber unsubscribeSubscriber = new UnsubscribeSubscriber();
					mRxService.editSubscription(queryMap)
						.subscribeOn(Schedulers.io())
						.observeOn(AndroidSchedulers.mainThread())
						.compose(activity.<ResponseBody>bindToLifecycle())
						.subscribe(unsubscribeSubscriber);
				}
			})
			.setNegativeButton(R.string.alert_cancel, null)
			.show();
	}

	private class UnsubscribeSubscriber extends Subscriber<ResponseBody> {

		@Override
		public void onCompleted() {
			unsubscribe();
		}

		@Override
		public void onError(Throwable e) {
			Snackbar
				.make(findViewById(R.id.sceneRoot),
					R.string.error_unsubscribe,
					Snackbar.LENGTH_LONG)
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
					.make(findViewById(R.id.sceneRoot),
						R.string.unsubscribe_successful,
						Snackbar.LENGTH_SHORT)
					.show();

				refreshOnClick();
			} else {
				onError(null);
			}

			unsubscribe();
		}
	}

	private void deleteFolder() {
		final RxAppCompatActivity activity = this;
		new AlertDialog.Builder(this, R.style.MyAlertDialogStyle)
			.setTitle(R.string.alert_delete_folder_title)
			.setMessage(R.string.alert_delete_folder_prompt)
			.setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialogInterface, int i) {
					// since the "mark all read" feed will be the same as the one to unsubscribe from, reuse it
					Map<String, String> queryMap = new ArrayMap<>();
					queryMap.put("s", mMarkAllReadFeed);

					DeleteFolderSubscriber deleteFolderSubscriber = new DeleteFolderSubscriber();
					mRxService.disableTag(queryMap)
						.subscribeOn(Schedulers.io())
						.observeOn(AndroidSchedulers.mainThread())
						.compose(activity.<ResponseBody>bindToLifecycle())
						.subscribe(deleteFolderSubscriber);
				}
			})
			.setNegativeButton(R.string.alert_cancel, null)
			.show();
	}

	private class DeleteFolderSubscriber extends Subscriber<ResponseBody> {

		@Override
		public void onCompleted() {
			unsubscribe();
		}

		@Override
		public void onError(Throwable e) {
			Snackbar
				.make(findViewById(R.id.sceneRoot),
					R.string.error_delete_folder,
					Snackbar.LENGTH_LONG)
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
					.make(findViewById(R.id.sceneRoot),
						R.string.delete_folder_successful,
						Snackbar.LENGTH_SHORT)
					.show();

				refreshOnClick();
			} else {
				onError(null);
			}

			unsubscribe();
		}
	}

	private void changeFoldersOnClick() {
		Bundle args = new Bundle();
		// mMarkAllRead feed will be the same as the feed we wish to change
		args.putString("id", mMarkAllReadFeed);
		FragmentManager fm = getSupportFragmentManager();
		ChangeSubscriptionFolderDialog fragment = new ChangeSubscriptionFolderDialog();
		fragment.setArguments(args);
		fragment.show(fm, null);
	}

	private void changeNameOnClick(boolean changeSubscription) {
		Bundle args = new Bundle();
		// mMarkAllRead feed will be the same as the feed we wish to change
		args.putString("id", mMarkAllReadFeed);
		args.putString("name", mMarkAllReadFeedName);
		args.putBoolean("changeSubscription", changeSubscription);
		FragmentManager fm = getSupportFragmentManager();
		ChangeNameDialog fragment = new ChangeNameDialog();
		fragment.setArguments(args);
		fragment.show(fm, null);
	}
}