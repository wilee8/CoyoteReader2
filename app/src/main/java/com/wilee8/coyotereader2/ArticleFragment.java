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
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsSession;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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

import org.parceler.Parcels;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArticleFragment extends Fragment {

	private ArticleFragmentListener mCallback;
	private Context                 mContext;

	private ArticleItem mItem;

	private ImageView  mStarFrame;
	private ScrollView mArticleScroll;

	private int mScrollX;
	private int mScrollY;

	private CustomTabsSession mCustomTabsSession;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		try {
			mCallback = (ArticleFragmentListener) context;
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString() + " must implement ArticleFragmentListener");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			mScrollX = savedInstanceState.getInt("scrollX", -1);
			mScrollY = savedInstanceState.getInt("scrollY", -1);
		} else {
			mScrollX = -1;
			mScrollY = -1;
		}

		mContext = getActivity();
		mCustomTabsSession = mCallback.getCustomTabsSession();
	}

	@Nullable
	@SuppressLint("SetJavaScriptEnabled")
	@SuppressWarnings("deprecation")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_article, container, false);

		if (savedInstanceState != null) {
			mItem = Parcels.unwrap(savedInstanceState.getParcelable("articleItem"));
		} else {
			mItem = Parcels.unwrap(getArguments().getParcelable("articleItem"));
		}

		TextView titleFrame = (TextView) rootView.findViewById(R.id.title_frame);
		TextView authorFrame = (TextView) rootView.findViewById(R.id.author_frame);
		mArticleScroll = (ScrollView) rootView.findViewById(R.id.articleScroll);
		WebView summaryFrame = (WebView) rootView.findViewById(R.id.summary_frame);
		mStarFrame = (ImageView) rootView.findViewById(R.id.articleStar);

		setStarDrawable(mItem.getStarred());

		int position = getArguments().getInt("position");
		StarClickListener starClickListener = new StarClickListener(position);
		mStarFrame.setOnClickListener(starClickListener);

		// Set title
		titleFrame.setClickable(false);
		titleFrame.setText(Html.fromHtml("<b><a href=\"" + mItem.getCanonical() + "\">"
											 + mItem.getTitle() + "</a></b>"));
		titleFrame.setOnClickListener(new TitleOnClickListener(mItem.getCanonical()));
		titleFrame.setBackground(getResources().getDrawable(R.drawable.ripple_selector));

		// Set author
		String author = mItem.getAuthor();
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
		summaryFrame.setWebViewClient(new MyWebViewClient());

		// get mouseover text for webcomics
		if (mItem.getOrigin().matches("xkcd.com")) {
			Pattern findTitle = Pattern.compile("title=\"(.*?)\"");
			Matcher matcher = findTitle.matcher(mItem.getSummary());
			while (matcher.find()) {
				String s = matcher.group(1);
				summaryFrame.setOnLongClickListener(new WebComicOnLongClickListener(s));
			}
		}

		// Set summary
		String imageCss = "<style>img{display: inline;max-width: 95%;display: block;margin-left: auto;margin-right: auto;}</style>" +
			"<style>iframe{display: inline;max-width: 95%;display: block;margin-left: auto; margin-right: auto;}</style>";
		summaryFrame.loadUrl("about:blank");
		summaryFrame.loadData(imageCss + mItem.getSummary(), "text/html; charset=UTF-8", null);

		return rootView;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		int scrollX = mArticleScroll.getScrollX();
		int scrollY = mArticleScroll.getScrollY();

		outState.putInt("scrollX", scrollX);
		outState.putInt("scrollY", scrollY);

		outState.putParcelable("articleItem", Parcels.wrap(mItem));
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


	private class MyWebViewClient extends WebViewClient {

		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);

			view.postDelayed(new Runnable() {
				@Override
				public void run() {
					if ((mScrollX != -1) && (mScrollY != -1)) {
						mArticleScroll.scrollTo(mScrollX, mScrollY);
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

	public interface ArticleFragmentListener {
		void onStarClicked(int position, Boolean starred);

		CustomTabsSession getCustomTabsSession();

		String getBrowser();
	}

	private class StarClickListener implements View.OnClickListener {

		private int position;

		public StarClickListener(int position) {
			this.position = position;
		}

		@Override
		public void onClick(View view) {
			mItem.setStarred(!(mItem.getStarred()));

			setStarDrawable(mItem.getStarred());
			mCallback.onStarClicked(this.position, mItem.getStarred());
		}
	}

	@SuppressWarnings("deprecation")
	private void setStarDrawable(Boolean starred) {
		if (starred) {
			mStarFrame.setImageDrawable(mContext.getResources().getDrawable(
				R.drawable.ic_star_grey600_48dp));
		} else {
			mStarFrame.setImageDrawable(mContext.getResources().getDrawable(
				R.drawable.ic_star_outline_grey600_48dp));
		}
	}

	public void updateStarredStatus(Boolean starred) {
		mItem.setStarred(starred);
		setStarDrawable(mItem.getStarred());
	}
}
