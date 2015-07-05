package com.wilee8.coyotereader2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import com.wilee8.coyotereader2.containers.ArticleItem;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArticleFragment extends Fragment{

	private ArticleFragmentListener mCallback;
	private Context                 mContext;

	private ArticleItem mItem;

	private WebView   mSummaryFrame;
	private ImageView mStarFrame;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			mCallback = (ArticleFragmentListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement ArticleFragmentListener");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = getActivity();
	}

	@Nullable
	@SuppressWarnings("deprecation")
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_article, container, false);

		mItem = Parcels.unwrap(getArguments().getParcelable("articleItem"));

		TextView titleFrame = (TextView) rootView.findViewById(R.id.title_frame);
		TextView authorFrame = (TextView) rootView.findViewById(R.id.author_frame);
		mSummaryFrame = (WebView) rootView.findViewById(R.id.summary_frame);
		mStarFrame = (ImageView) rootView.findViewById(R.id.articleStar);

		// set star
		ArrayList<String> categories = mItem.getCategories();
		Boolean starred = false;

		for (int i = 0; i < categories.size(); i++) {
			String category = categories.get(i);

			if (category.equals("user/" + mCallback.getUserId() + "/state/com.google/starred")) {
				starred = true;
			}
		}

		if (starred) {
			mStarFrame.setImageDrawable(mContext.getResources().getDrawable(
				R.drawable.ic_star_grey600_48dp));
		} else {
			mStarFrame.setImageDrawable(mContext.getResources().getDrawable(
				R.drawable.ic_star_outline_grey600_48dp));
		}

		//TODO set onClickListener for mStarFrame

		// Set title
		titleFrame.setLinksClickable(true);
		titleFrame.setMovementMethod(LinkMovementMethod.getInstance());
		titleFrame.setText(Html.fromHtml("<b><a href=\"" + mItem.getCanonical() + "\">"
											  + mItem.getTitle() + "</a></b>"));

		// Set author
		String author = mItem.getAuthor();
		if ((author != null) && (author.length() != 0)) {
			authorFrame.setText("by " + author);
			authorFrame.setVisibility(View.VISIBLE);
		}

		WebSettings ws = mSummaryFrame.getSettings();
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

		mSummaryFrame.setBackgroundColor(getResources().getColor(R.color.cardview_light_background));

		if ((savedInstanceState != null) && savedInstanceState.containsKey("progress")) {
			float progress = savedInstanceState.getFloat("progress");

			mSummaryFrame.setWebViewClient(new MyWebViewClient(progress, mSummaryFrame));
		}

		// get mouseover text for webcomics
		if (mItem.getOrigin().matches("http://xkcd.com/")) {
			Pattern findTitle = Pattern.compile("title=\"(.*?)\"");
			Matcher matcher = findTitle.matcher(mItem.getSummary());
			while (matcher.find()) {
				String s = matcher.group(1);
				mSummaryFrame.setOnLongClickListener(new WebComicOnLongClickListener(s));
			}
		}

		// Set summary
		String imageCss = "<style>img{display: inline;max-width: 95%;display: block;margin-left: auto;margin-right: auto;}</style>" +
			"<style>iframe{display: inline;max-width: 95%;display: block;margin-left: auto; margin-right: auto;}</style>";
		mSummaryFrame.loadUrl("about:blank");
		mSummaryFrame.loadData(imageCss + mItem.getSummary(), "text/html; charset=UTF-8", null);

		return rootView;
	}

	private class MyWebViewClient extends WebViewClient {
		private float   mProgress;
		private WebView mWebView;

		public MyWebViewClient(float progress, WebView webview) {
			super();

			mProgress = progress;
			mWebView = webview;
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);

			view.postDelayed(new Runnable() {
				@Override
				public void run() {
					float positionTopView = mSummaryFrame.getTop();
					float contentHeight = mSummaryFrame.getContentHeight();
					int currentScrollPosition = Math.round((contentHeight * mProgress) + positionTopView);
					mWebView.scrollTo(0, currentScrollPosition);
				}
			}, 300);
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
		String getUserId();
	}
}
