package com.wilee8.coyotereader2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsSession;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.trello.rxlifecycle.components.support.RxFragment;
import com.wilee8.coyotereader2.containers.ArticleItem;
import com.wilee8.coyotereader2.containers.ArticleScrollState;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class ArticlePagerFragment extends RxFragment {

	private ArticlePagerFragmentListener mCallback;
	private Context                      mContext;
	private ViewPager                    mPager;
	private ArticlePagerAdapter          mPagerAdapter;
	private ArrayList<ArticleItem>       mItems;
	private int                          mPosition;
	private int                          mScrollX;
	private int                          mScrollY;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		try {
			mCallback = (ArticlePagerFragmentListener) context;
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString() + " must implement ArticlePagerFragmentListener");
		}
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = getActivity();

		// always assume no scroll, restoreState will change these if there is a scroll to restore
		mScrollX = -1;
		mScrollY = -1;

		if (savedInstanceState != null) {
			mPosition = savedInstanceState.getInt("mPosition", 0);
		} else {
			mPosition = getArguments().getInt("position", 0);
		}
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_article_pager, container, false);

		mPager = (ViewPager) rootView.findViewById(R.id.pager);

		Observable<Integer> initTask = Observable.create(new InitTask());
		InitFinish initFinish = new InitFinish();

		initTask
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.compose(this.<Integer>bindToLifecycle())
			.subscribe(initFinish);

		return rootView;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putInt("mPosition", mPosition);
	}

	public void updateItems() {
		mPagerAdapter.notifyDataSetChanged();
	}

	public void changeSelected(int position) {
		mPager.setCurrentItem(position);
		mPosition = position;
	}

	public interface ArticlePagerFragmentListener {
		ArrayList<ArticleItem> getItems();

		void onArticleSelected(int position);

		void onStarClicked(int position, Boolean starred);

		CustomTabsSession getCustomTabsSession();

		String getBrowser();
	}


	private class ArticlePagerAdapter extends PagerAdapter {

		private ViewGroup mCurrentPage;

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View) object);
		}

		@Override
		public int getCount() {
			return mItems.size();
		}

		public ViewGroup getCurrentPage() {
			return mCurrentPage;
		}

		@SuppressLint("SetJavaScriptEnabled")
		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			LayoutInflater inflater = LayoutInflater.from(mContext);
			ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_article, container, false);
			ArticleItem item = mItems.get(position);

			TextView titleFrame = (TextView) rootView.findViewById(R.id.title_frame);
			TextView authorFrame = (TextView) rootView.findViewById(R.id.author_frame);
			WebView summaryFrame = (WebView) rootView.findViewById(R.id.summary_frame);
			ImageView starFrame = (ImageView) rootView.findViewById(R.id.articleStar);

			setStarDrawable(starFrame, item.getStarred());
			StarClickListener starClickListener = new StarClickListener(position);
			starFrame.setOnClickListener(starClickListener);

			// Set title
			titleFrame.setClickable(false);
			titleFrame.setText(Html.fromHtml("<b><a href=\"" + item.getCanonical() + "\">"
												 + item.getTitle() + "</a></b>"));
			titleFrame.setOnClickListener(new TitleOnClickListener(item.getCanonical()));
			titleFrame.setBackground(ContextCompat.getDrawable(getActivity(),
															   R.drawable.ripple_selector));

			// Set author
			String author = item.getAuthor();
			if ((author != null) && (author.length() != 0)) {
				authorFrame.setText(
					String.format(getString(R.string.article_byline),
								  Html.fromHtml(Html.fromHtml(author).toString()).toString()));
				authorFrame.setVisibility(View.VISIBLE);
			}

			WebSettings ws = summaryFrame.getSettings();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				ws.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
			} else {
				ws.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
			}
			ws.setBuiltInZoomControls(true);
			ws.setDisplayZoomControls(true);
			ws.setJavaScriptEnabled(true);
			ws.setLoadWithOverviewMode(true);
			ws.setTextZoom(getResources().getInteger(R.integer.item_text_zoom));

			summaryFrame.setBackgroundColor(ContextCompat.getColor(getActivity(),
																   R.color.frame_background));
			if ((mScrollX != -1) && (mScrollY != -1)) {
				summaryFrame.setWebViewClient(new MyWebViewClient(mScrollX, mScrollY));
				mScrollX = -1;
				mScrollY = -1;
			} else {
				summaryFrame.setWebViewClient(new MyWebViewClient());
			}

			// get mouseover text for webcomics
			if (item.getOrigin().matches("xkcd.com")) {
				Pattern findTitle = Pattern.compile("title=\"(.*?)\"");
				Matcher matcher = findTitle.matcher(item.getSummary());
				while (matcher.find()) {
					String s = matcher.group(1);
					summaryFrame.setOnLongClickListener(new WebComicOnLongClickListener(s));
				}
			}

			// Set summary
			String imageCss = "<style>img{display: inline;max-width: 95%; height: auto;display: block;margin-left: auto;margin-right: auto;}</style>" +
				"<style>iframe{display: inline;max-width: 95%;display: block;margin-left: auto; margin-right: auto;}</style>";
			summaryFrame.loadUrl("about:blank");
			summaryFrame.loadData(imageCss + item.getSummary(), "text/html; charset=UTF-8", null);

			container.addView(rootView);
			return rootView;
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}

		@Override
		public void restoreState(Parcelable state, ClassLoader loader) {
			ArticleScrollState scrollState = (ArticleScrollState) state;
			mScrollX = scrollState.getScrollX();
			mScrollY = scrollState.getScrollY();
		}

		@Override
		public Parcelable saveState() {
			ViewGroup currentPage = mPagerAdapter.getCurrentPage();
			ScrollView articleScroll = (ScrollView) currentPage.findViewById(R.id.articleScroll);
			return new ArticleScrollState(articleScroll.getScrollX(),
															  articleScroll.getScrollY());
		}

		@Override
		public void setPrimaryItem(ViewGroup container, int position, Object object) {
			mCurrentPage = (ViewGroup) object;
			super.setPrimaryItem(container, position, object);
		}
	}

	private class ArticleOnPageChangeListener extends ViewPager.SimpleOnPageChangeListener {

		@Override
		public void onPageSelected(int position) {
			super.onPageSelected(position);
			mPosition = position;

			mCallback.onArticleSelected(position);
		}
	}

	public void updateStarredStatus(int position, Boolean starred) {
		// save starred status
		ArticleItem item = mItems.get(position);
		item.setStarred(starred);

		// If view is still visible, update star drawable
		if (mPager.getCurrentItem() == position) {
			ViewGroup currentPage = mPagerAdapter.getCurrentPage();
			ImageView starFrame = (ImageView) currentPage.findViewById(R.id.articleStar);
			setStarDrawable(starFrame, starred);
		}
	}

	private void setStarDrawable(ImageView starFrame, Boolean starred) {
		if (starred) {
			starFrame.setImageDrawable(
				ContextCompat.getDrawable(getActivity(), R.drawable.ic_star_24dp));
		} else {
			starFrame.setImageDrawable(
				ContextCompat.getDrawable(getActivity(), R.drawable.ic_star_outline_24dp));
		}
	}

	@SuppressWarnings("deprecation")
	private void launchURL(String url) {
		Uri uri = Uri.parse(url);
		if (mCallback.getBrowser().matches(
			mContext.getResources().getString(R.string.pref_browser_default_value))) {
			// default should be external browser
			Intent i = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(i);
		} else {
			// right now only alternative is chrome tabs
			// create pending intent for share action button
			Intent actionIntent = new Intent(Intent.ACTION_SEND);
			actionIntent.setType("text/plain");
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				actionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
			} else {
				actionIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			}
			actionIntent.putExtra(Intent.EXTRA_TEXT, url);
			PendingIntent pendingIntent =
				PendingIntent.getActivity(mContext,
										  0,
										  actionIntent,
										  PendingIntent.FLAG_UPDATE_CURRENT);
			String shareLabel = mContext.getString(R.string.action_share);

			// launch custom tab
			CustomTabsIntent.Builder customTabsIntentBuilder =
				new CustomTabsIntent.Builder(mCallback.getCustomTabsSession())
					.setToolbarColor(
						ContextCompat.getColor(getActivity(), R.color.primary))
					.setShowTitle(true);

			Drawable drawable = ContextCompat.getDrawable(getActivity(), R.drawable.ic_share_24dp);
			if (drawable != null) {
				int width = drawable.getIntrinsicWidth();
				int height = drawable.getIntrinsicHeight();
				Bitmap shareIcon = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
				Canvas canvas = new Canvas(shareIcon);
				drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
				drawable.draw(canvas);

				customTabsIntentBuilder = customTabsIntentBuilder.
					setActionButton(shareIcon, shareLabel, pendingIntent);
			}

			CustomTabsIntent customTabsIntent = customTabsIntentBuilder.build();
			customTabsIntent.launchUrl((Activity) mContext, uri);
		}
	}

	private class StarClickListener implements View.OnClickListener {

		private int position;

		public StarClickListener(int position) {
			this.position = position;
		}

		@Override
		public void onClick(View view) {
			ArticleItem item = mItems.get(position);
			item.setStarred(!(item.getStarred()));

			setStarDrawable((ImageView) view, item.getStarred());
			mCallback.onStarClicked(this.position, item.getStarred());
		}
	}

	private class MyWebViewClient extends WebViewClient {

		private int scrollX;
		private int scrollY;

		public MyWebViewClient() {
			scrollX = -1;
			scrollY = -1;
		}

		public MyWebViewClient(int scrollX, int scrollY) {
			this.scrollX = scrollX;
			this.scrollY = scrollY;
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);

			view.postDelayed(() -> {
				if ((scrollX != -1) && (scrollY != -1)) {
					ViewGroup currentPage = mPagerAdapter.getCurrentPage();
					ScrollView articleScroll = (ScrollView) currentPage.findViewById(R.id.articleScroll);
					articleScroll.scrollTo(scrollX, scrollY);
				}
			}, 300);
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			launchURL(url);
			return true;
		}
	}

	private class WebComicOnLongClickListener implements View.OnLongClickListener {
		private String mMessage;

		public WebComicOnLongClickListener(String message) {
			mMessage = message;
		}

		@Override
		public boolean onLongClick(View v) {
			FragmentManager fm = getActivity().getSupportFragmentManager();
			WebComicDialogFragment fragment = new WebComicDialogFragment();
			Bundle bundle = new Bundle();
			bundle.putString("message", Html.fromHtml(mMessage).toString());
			fragment.setArguments(bundle);
			fragment.show(fm, null);

			return true;
		}
	}

	private class TitleOnClickListener implements View.OnClickListener {

		private String url;

		public TitleOnClickListener(String url) {
			this.url = url;
		}

		@Override
		public void onClick(View view) {
			launchURL(url);
		}
	}

	private class InitTask implements Observable.OnSubscribe<Integer> {
		@Override
		public void call(Subscriber<? super Integer> subscriber) {
			mPager.setPageTransformer(true, new ZoomOutPageTransformer());
			mPager.addOnPageChangeListener(new ArticleOnPageChangeListener());

			mItems = mCallback.getItems();
			mPagerAdapter = new ArticlePagerAdapter();
			subscriber.onNext(mPosition);
			subscriber.onCompleted();
		}
	}

	private class InitFinish extends Subscriber<Integer> {
		@Override
		public void onCompleted() {
			unsubscribe();
		}

		@Override
		public void onError(Throwable e) {
			unsubscribe();
		}

		@Override
		public void onNext(Integer position) {
			mPager.setAdapter(mPagerAdapter);
			mPager.setCurrentItem(position);
		}
	}
}
