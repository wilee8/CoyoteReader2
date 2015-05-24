package com.wilee8.coyotereader2;


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.wilee8.coyotereader2.containers.TagItem;

import java.util.ArrayList;

import static android.view.View.OnClickListener;

public class NavFragment extends Fragment {

	private NavFragmentListener mCallback;

	private Activity    mContext;
	private ImageLoader mImageLoader;

	private NavAdapter mAdapter;

	private ArrayList<TagItem> mNavList;

	private int mSelected;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = getActivity();

		if (savedInstanceState != null) {
			mSelected = savedInstanceState.getInt("mSelected", -1);
		} else {
			mSelected = -1;
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			mCallback = (NavFragmentListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement NavFragmentListener");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_nav, container, false);

		mNavList = mCallback.getNavList();

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

		RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.nav_recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
		recyclerView.setHasFixedSize(true);
		recyclerView.setItemAnimator(new DefaultItemAnimator());

		mAdapter = new NavAdapter();
		recyclerView.setAdapter(mAdapter);

		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		if (mSelected != -1) {
			outState.putInt("mSelected", mSelected);
		}
	}

	public interface NavFragmentListener {
		ArrayList<TagItem> getNavList();

		RequestQueue getRequestQueue();

		void selectNav(String id, String title);
	}

	private class NavAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

		public static final int VIEW_TYPE_LOCAL_RESOURCE = 0;
		public static final int VIEW_TYPE_REMOTE_URL     = 1;
		public static final int VIEW_TYPE_SUB_FEED       = 2;

		@Override
		public int getItemViewType(int position) {
			TagItem tagItem = mNavList.get(position);
			String iconUrl = tagItem.getIconUrl();

			if (!tagItem.getIsTopLevel()) {
				return VIEW_TYPE_SUB_FEED;
			} else if ((tagItem.getResId() == 0) &&
				(iconUrl != null) &&
				(!iconUrl.matches("ICON_PATH/feed.png"))) {
				// use networkimageview to display favicon from url
				return VIEW_TYPE_REMOTE_URL;
			} else {
				// use imageview to display drawable resource
				return VIEW_TYPE_LOCAL_RESOURCE;
			}
		}

		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			if (viewType == VIEW_TYPE_SUB_FEED) {
				return new SubfeedViewHolder(LayoutInflater.from(mContext).
					inflate(R.layout.row_nav_card_subfeed, parent, false));
			} else if (viewType == VIEW_TYPE_LOCAL_RESOURCE) {
				// use networkimageview to display favicon url
				return new LocalViewHolder(LayoutInflater.from(mContext).
					inflate(R.layout.row_nav_card, parent, false));
			} else {
				return new NetworkViewHolder(LayoutInflater.from(mContext).
					inflate(R.layout.row_nav_card_network, parent, false));
			}
		}

		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {

			TagItem tagItem = mNavList.get(position);

			NavViewHolder navViewHolder = (NavViewHolder) viewHolder;

			if (tagItem.getIsTopLevel()) {
				if (!tagItem.getIsFeed()) {
					if (tagItem.getIsExpanded()) {
						navViewHolder.tagExpand.setImageDrawable(
							mContext.getResources().getDrawable(R.drawable.ic_arrow_drop_up_grey600_48dp));
					} else {
						navViewHolder.tagExpand.setImageDrawable(
							mContext.getResources().getDrawable(R.drawable.ic_arrow_drop_down_grey600_48dp));
					}
				} else {
					navViewHolder.tagExpand.setImageDrawable(null);
				}
			}

			switch (getItemViewType(position)) {
				case VIEW_TYPE_SUB_FEED:
					String iconUrl = tagItem.getIconUrl();
					SubfeedViewHolder subfeedViewHolder = (SubfeedViewHolder) viewHolder;
					subfeedViewHolder.tagIcon.setImageUrl(iconUrl, mImageLoader);

					break;

				case VIEW_TYPE_REMOTE_URL:
					iconUrl = tagItem.getIconUrl();
					NetworkViewHolder networkViewHolder = (NetworkViewHolder) viewHolder;
					networkViewHolder.tagIcon.setImageUrl(iconUrl, mImageLoader);

					break;

				case VIEW_TYPE_LOCAL_RESOURCE:
					LocalViewHolder localViewHolder = (LocalViewHolder) viewHolder;
					if (tagItem.getResId() == 0) {
						localViewHolder.tagIcon.setImageDrawable(
							mContext.getResources().getDrawable(R.drawable.clear_favicon));
					} else {
						localViewHolder.tagIcon.setImageDrawable(
							mContext.getResources().getDrawable(tagItem.getResId()));
					}

					break;
			}

			navViewHolder.tagName.setText(tagItem.getName());

			// unread count should only be visible if not zero
			// bold both name and unread count if not zero
			if (tagItem.getUnreadCount() != 0) {
				navViewHolder.tagUnreadCount.setVisibility(View.VISIBLE);
				navViewHolder.tagUnreadCount.setText(String.valueOf(tagItem.getUnreadCount()));
				navViewHolder.tagUnreadCount.setTypeface(null, Typeface.BOLD);
				navViewHolder.tagName.setTypeface(null, Typeface.BOLD);
			} else {
				navViewHolder.tagUnreadCount.setVisibility(View.GONE);
				navViewHolder.tagUnreadCount.setTypeface(null, Typeface.NORMAL);
				navViewHolder.tagName.setTypeface(null, Typeface.NORMAL);
			}

			if (position == mSelected) {
				navViewHolder.tagCardView.setCardBackgroundColor(getResources().getColor(R.color.accent));
			} else {
				navViewHolder.tagCardView.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background));
			}

			navViewHolder.tagSelectAll.setOnClickListener(new NavSelectClickListener(tagItem));
			if(!tagItem.getIsFeed()) {
				navViewHolder.tagExpand.setOnClickListener(new NavExpandClickListener(tagItem));
			}
		}

		@Override
		public int getItemCount() {
			return mNavList.size();
		}
	}

	private class NavViewHolder extends RecyclerView.ViewHolder {
		public ImageView      tagExpand;
		public RelativeLayout tagSelectAll;
		public TextView       tagName;
		public TextView       tagUnreadCount;
		public CardView       tagCardView;

		public NavViewHolder(View itemView) {
			super(itemView);
			tagExpand = (ImageView) itemView.findViewById(R.id.tagExpand);
			tagSelectAll = (RelativeLayout) itemView.findViewById(R.id.tagSelectAll);
			tagName = (TextView) itemView.findViewById(R.id.tagName);
			tagUnreadCount = (TextView) itemView.findViewById(R.id.tagUnreadCount);
			tagCardView = (CardView) itemView.findViewById(R.id.tagCardView);
		}
	}

	private class LocalViewHolder extends NavViewHolder {
		public ImageView tagIcon;

		public LocalViewHolder(View itemView) {
			super(itemView);
			tagIcon = (ImageView) itemView.findViewById(R.id.tagIcon);
		}
	}

	private class NetworkViewHolder extends NavViewHolder {
		public NetworkImageView tagIcon;

		public NetworkViewHolder(View itemView) {
			super(itemView);
			tagIcon = (NetworkImageView) itemView.findViewById(R.id.tagIcon);
		}
	}

	private class SubfeedViewHolder extends NavViewHolder {
		public NetworkImageView tagIcon;

		public SubfeedViewHolder(View itemView) {
			super(itemView);
			tagIcon = (NetworkImageView) itemView.findViewById(R.id.tagIcon);
		}
	}
	private class NavSelectClickListener implements OnClickListener {

		private TagItem thisItem;

		public NavSelectClickListener(TagItem item) {
			thisItem = item;
		}

		@Override
		public void onClick(View view) {
			RelativeLayout thisView = (RelativeLayout) view;
			CardView newView = (CardView) thisView.getParent();
			newView.setCardBackgroundColor(getResources().getColor(R.color.accent));
			int oldSelected = mSelected;
			mSelected = mNavList.indexOf(thisItem);

			if (oldSelected != -1) {
				mAdapter.notifyItemChanged(oldSelected);
			}

			mCallback.selectNav(thisItem.getId(), thisItem.getName());
		}
	}

	private class NavExpandClickListener implements OnClickListener {

		private TagItem thisItem;

		public NavExpandClickListener(TagItem item) {
			thisItem = item;
		}

		@Override
		public void onClick(View view) {
			int index = mNavList.indexOf(thisItem);

			if(thisItem.getIsExpanded()) {
				// expanded, remove all sub feeds so we can close
				int count = 0;
				while(!mNavList.get(index + 1).getIsTopLevel()) {
					mNavList.remove(index + 1);
					mAdapter.notifyItemRemoved(index + 1);
					count++;
				}

				// adjust selected item if shifted
				if(mSelected > index) {
					if(mSelected > (index + count)) {
						// selected item not removed, adjust value
						int oldSelected = mSelected;
						mSelected = mSelected - count;

						mAdapter.notifyItemChanged(oldSelected);
						mAdapter.notifyItemChanged(mSelected);
					} else {
						// selected item was removed, nothing selected any more
						int oldSelected = mSelected;
						mSelected = -1;

						mAdapter.notifyItemChanged(oldSelected);
					}
				}
			} else {
				// closed, add all sub feeds so we can expand
				ArrayList<TagItem> thisFeedList = thisItem.getFeeds();
				mNavList.addAll(index + 1, thisFeedList);
				mAdapter.notifyItemRangeInserted(index + 1, thisFeedList.size());

				// adjust selected item if shifted
				if(mSelected > index) {
					// selected item shifted, adjust value
					int oldSelected = mSelected;
					mSelected = mSelected + thisFeedList.size();

					mAdapter.notifyItemChanged(oldSelected);
					mAdapter.notifyItemChanged(mSelected);
				}
			}

			// toggle expand button
			thisItem.setIsExpanded(!thisItem.getIsExpanded());
			mAdapter.notifyItemChanged(index);
		}
	}
}