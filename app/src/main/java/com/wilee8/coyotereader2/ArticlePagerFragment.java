package com.wilee8.coyotereader2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsSession;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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

import com.wilee8.coyotereader2.containers.ArticleItem;
import com.wilee8.coyotereader2.containers.ArticleScrollState;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArticlePagerFragment extends Fragment {

	private ArticlePagerFragmentListener mCallback;
	private Context                      mContext;
	private ViewPager                    mPager;
	private ArticlePagerAdapter          mPagerAdapter;
	private ArrayList<ArticleItem>       mItems;
	private CustomTabsSession            mCustomTabsSession;
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
		mCustomTabsSession = mCallback.getCustomTabsSession();

		// always assume no scroll, restoreState will change these if there is a scroll to restore
		mScrollX = -1;
		mScrollY = -1;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_article_pager, container, false);

		mPager = (ViewPager) rootView.findViewById(R.id.pager);
		// For some reason using page transformers causes other fragments to render incorrectly until refreshed
		mPager.setPageTransformer(true, new ZoomOutPageTransformer());
		mPager.addOnPageChangeListener(new ArticleOnPageChangeListener());

		mItems = mCallback.getItems();
		int position = getArguments().getInt("position", 0);
		mPagerAdapter = new ArticlePagerAdapter();
		mPager.setAdapter(mPagerAdapter);
		mPager.setCurrentItem(position);

		return rootView;
	}

	public void updateItems() {
		mPagerAdapter.notifyDataSetChanged();
	}

	public void changeSelected(int position) {
		mPager.setCurrentItem(position);
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
		@SuppressWarnings("deprecation")
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
			titleFrame.setBackground(getResources().getDrawable(R.drawable.ripple_selector));

			// Set author
			String author = item.getAuthor();
			if ((author != null) && (author.length() != 0)) {
				authorFrame.setText("by " + Html.fromHtml(author).toString());
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

			summaryFrame.setBackgroundColor(getResources().getColor(R.color.background_material_light));
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
			String imageCss = "<style>img{display: inline;max-width: 95%;display: block;margin-left: auto;margin-right: auto;}</style>" +
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
			ArticleScrollState scrollState = Parcels.unwrap(state);
			mScrollX = scrollState.getScrollX();
			mScrollY = scrollState.getScrollY();
		}

		@Override
		public Parcelable saveState() {
			ViewGroup currentPage = mPagerAdapter.getCurrentPage();
			ScrollView articleScroll = (ScrollView) currentPage.findViewById(R.id.articleScroll);
			ArticleScrollState state = new ArticleScrollState(articleScroll.getScrollX(),
															  articleScroll.getScrollY());
			return Parcels.wrap(state);
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

	@SuppressWarnings("deprecation")
	private void setStarDrawable(ImageView starFrame, Boolean starred) {
		if (starred) {
			starFrame.setImageDrawable(mContext.getResources().getDrawable(
				R.drawable.ic_star_24dp));
		} else {
			starFrame.setImageDrawable(mContext.getResources().getDrawable(
				R.drawable.ic_star_outline_24dp));
		}
	}

	@SuppressWarnings("deprecation")
	private void launchURL(String url) {
		Uri uri = Uri.parse(url);
		if (mCallback.getBrowser().matches(
			mContext.getResources().getString(R.string.pref_browser_default_value))) {
			// default should be chrome tabs
			// create pending intent for share action button
			Intent actionIntent = new Intent(Intent.ACTION_SEND);
			actionIntent.setType("text/plain");
			actionIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			actionIntent.putExtra(Intent.EXTRA_TEXT, url);
			PendingIntent pendingIntent =
				PendingIntent.getActivity(mContext,
										  0,
										  actionIntent,
										  PendingIntent.FLAG_UPDATE_CURRENT);
			String shareLabel = mContext.getString(R.string.action_share);
			Bitmap shareIcon = BitmapFactory.decodeResource(mContext.getResources(),
															R.drawable.ic_share_white_48dp);

			// launch custom tab
			CustomTabsIntent customTabsIntent =
				new CustomTabsIntent.Builder(mCustomTabsSession)
					.setToolbarColor(mContext.getResources().getColor(R.color.primary))
					.setShowTitle(true)
					.setActionButton(shareIcon, shareLabel, pendingIntent)
					.build();
			customTabsIntent.launchUrl((Activity) mContext, uri);
		} else {
			// right now only alternative is external browser
			Intent i = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(i);
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

			view.postDelayed(new Runnable() {
				@Override
				public void run() {
					if ((scrollX != -1) && (scrollY != -1)) {
						ViewGroup currentPage = mPagerAdapter.getCurrentPage();
						ScrollView articleScroll = (ScrollView) currentPage.findViewById(R.id.articleScroll);
						articleScroll.scrollTo(scrollX, scrollY);
					}
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
}
