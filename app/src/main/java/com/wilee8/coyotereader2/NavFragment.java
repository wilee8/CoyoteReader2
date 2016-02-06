package com.wilee8.coyotereader2;


import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.wilee8.coyotereader2.containers.TagItem;

import java.util.ArrayList;

public class NavFragment extends Fragment {

	private NavFragmentListener mCallback;

	private Activity mContext;

	private NavAdapter          mAdapter;
	private LinearLayoutManager mLayoutManager;

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
	public void onAttach(Context context) {
		super.onAttach(context);

		try {
			mCallback = (NavFragmentListener) context;
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString() + " must implement NavFragmentListener");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_nav, container, false);

		mNavList = mCallback.getNavList();

		RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.nav_recycler_view);
		mLayoutManager = new LinearLayoutManager(mContext);
		recyclerView.setLayoutManager(mLayoutManager);
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
			// use networkimageview to display favicon url
			return new NavViewHolder(LayoutInflater.from(mContext).
				inflate(R.layout.row_nav_card, parent, false));
		}

		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {

			TagItem tagItem = mNavList.get(position);

			NavViewHolder navViewHolder = (NavViewHolder) viewHolder;

			if (tagItem.getIsTopLevel()) {
				if (!tagItem.getIsFeed()) {
					if (tagItem.getIsExpanded()) {
						navViewHolder.tagExpand.setImageDrawable(
							ContextCompat.getDrawable(mContext, R.drawable.ic_arrow_drop_up_24dp));
					} else {
						navViewHolder.tagExpand.setImageDrawable(
							ContextCompat.getDrawable(mContext, R.drawable.ic_arrow_drop_down_24dp));
					}
				} else {
					navViewHolder.tagExpand.setImageDrawable(null);
				}
			}

			switch (getItemViewType(position)) {
				case VIEW_TYPE_SUB_FEED:
				case VIEW_TYPE_REMOTE_URL:
					String iconUrl = tagItem.getIconUrl();
					if ((iconUrl.length() == 0) || (iconUrl.matches("ICON_PATH/feed.png"))) {
						navViewHolder.tagIcon.
							setImageDrawable(
								ContextCompat.getDrawable(mContext, R.drawable.clear_favicon));
					} else {
						Picasso.with(mContext)
							.load(iconUrl)
							.placeholder(R.drawable.clear_favicon)
							.error(R.drawable.clear_favicon)
							.into(navViewHolder.tagIcon);
					}

					break;

				case VIEW_TYPE_LOCAL_RESOURCE:
					if (tagItem.getResId() == 0) {
						navViewHolder.tagIcon.setImageDrawable(
							ContextCompat.getDrawable(mContext, R.drawable.clear_favicon));
					} else {
						navViewHolder.tagIcon.setImageDrawable(
							ContextCompat.getDrawable(mContext, tagItem.getResId()));
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

			navViewHolder.tagFrame.setBackground(
				ContextCompat.getDrawable(mContext, R.drawable.ripple_selector));
			if (position == mSelected) {
				navViewHolder.tagFrame.setSelected(true);
			} else {
				navViewHolder.tagFrame.setSelected(false);
			}

			navViewHolder.tagFrame.setOnClickListener(new NavSelectClickListener(tagItem));
			navViewHolder.tagFrame.setOnTouchListener(new NavSelectTouchListener(tagItem));
			if (!tagItem.getIsFeed()) {
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
		public ImageView      tagIcon;
		public TextView       tagName;
		public TextView       tagUnreadCount;
		public RelativeLayout tagFrame;
		public LinearLayout   tagRow;

		public NavViewHolder(View itemView) {
			super(itemView);
			tagExpand = (ImageView) itemView.findViewById(R.id.tagExpand);
			tagIcon = (ImageView) itemView.findViewById(R.id.tagIcon);
			tagName = (TextView) itemView.findViewById(R.id.tagName);
			tagUnreadCount = (TextView) itemView.findViewById(R.id.tagUnreadCount);
			tagFrame = (RelativeLayout) itemView.findViewById(R.id.tagFrame);
			tagRow = (LinearLayout) itemView.findViewById(R.id.tagRow);
		}
	}

	private class NavSelectClickListener implements OnClickListener {

		private TagItem thisItem;

		public NavSelectClickListener(TagItem item) {
			thisItem = item;
		}

		@Override
		public void onClick(View view) {
			mCallback.selectNav(thisItem.getId(), thisItem.getName());
		}
	}

	private class NavSelectTouchListener implements OnTouchListener {

		private TagItem thisItem;

		public NavSelectTouchListener(TagItem item) {
			thisItem = item;
		}


		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
				int oldSelected = mSelected;
				mSelected = mNavList.indexOf(thisItem);
				changeSelected(mSelected,
							   oldSelected,
							   (int) motionEvent.getX(),
							   (int) motionEvent.getY());
			}
			return false;
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

			if (thisItem.getIsExpanded()) {
				// expanded, remove all sub feeds so we can close
				int count = 0;
				while (!mNavList.get(index + 1).getIsTopLevel()) {
					mNavList.remove(index + 1);
					mAdapter.notifyItemRemoved(index + 1);
					count++;
				}

				// adjust selected item if shifted
				if (mSelected > index) {
					if (mSelected > (index + count)) {
						// selected item not removed, adjust value
						mSelected = mSelected - count;
					} else {
						// selected item was removed, nothing selected any more
						mSelected = -1;
					}
				}
			} else {
				// closed, add all sub feeds so we can expand
				ArrayList<TagItem> thisFeedList = thisItem.getFeeds();
				mNavList.addAll(index + 1, thisFeedList);
				mAdapter.notifyItemRangeInserted(index + 1, thisFeedList.size());

				// adjust selected item if shifted
				if (mSelected > index) {
					// selected item shifted, adjust value
					mSelected = mSelected + thisFeedList.size();
				}
			}

			// toggle expand button
			thisItem.setIsExpanded(!thisItem.getIsExpanded());
			mAdapter.notifyItemChanged(index);
		}
	}

	private void changeSelected(int newSelected, int oldSelected, int x, int y) {
		if (newSelected == oldSelected) return;

		if (newSelected != -1) {
			View view = mLayoutManager.findViewByPosition(newSelected);
			if (view != null) {
				RelativeLayout tagFrame = (RelativeLayout) view.findViewById(R.id.tagFrame);
				tagFrame.setSelected(true);

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					if (x == -1) {
						x = tagFrame.getWidth() / 2;
					}
					if (y == -1) {
						y = tagFrame.getHeight() / 2;
					}
					ViewAnimationUtils.createCircularReveal(tagFrame,
															x,
															y,
															0,
															tagFrame.getWidth()).start();
				}
			}
		}
		if (oldSelected != -1) {
			View view = mLayoutManager.findViewByPosition(oldSelected);
			if (view != null) {
				RelativeLayout tagFrame = (RelativeLayout) view.findViewById(R.id.tagFrame);
				tagFrame.setSelected(false);
			}
		}
	}

	public void updateUnreadCount(String id) {
		// the unread counts inside the nav list was updated in MainActivity
		// just need to refresh updated views here
		for (int i = 0; i < mNavList.size(); i++) {
			TagItem tagItem = mNavList.get(i);
			if (tagItem.getId().equalsIgnoreCase(id)) {
				mAdapter.notifyItemChanged(i);
				break;
			}
		}
	}

	public void advanceToNextUnreadFeed() {
		// if All Items unread count is zero, there is nowhere to advance
		// All Items is at index zero
		TagItem tagItem = mNavList.get(0);
		if (tagItem.getUnreadCount() != 0) {
			if (mSelected == 0) {
				// All Items, reload all items
				mCallback.selectNav(tagItem.getId(), tagItem.getName());
			} else {
				// search for non-"All Items" feed with unread items
				// offset starts loop at last selected feed
				int offset = (mSelected == -1) ? 0 : mSelected;
				int lastNavIndex = mNavList.size();

				for (int i = 0; i < mNavList.size(); i++) {
					int thisIndex = (offset + i) % lastNavIndex;

					// don't go to All Items if we weren't already there
					if (thisIndex != 0) {
						// if this feed has unread items, select it
						TagItem thisItem = mNavList.get(thisIndex);

						// don't select this item if it's an expanded top level item
						// if it has unread items, at least one child will have unread items
						if (!((thisItem.getIsTopLevel()) && (thisItem.getIsExpanded()))) {
							if (thisItem.getUnreadCount() != 0) {
								int oldSelected = mSelected;
								mSelected = thisIndex;

								changeSelected(mSelected, oldSelected, -1, -1);

								mLayoutManager.scrollToPosition(thisIndex);
								mCallback.selectNav(thisItem.getId(), thisItem.getName());
								break;
							}
						}
					}
				}
			}
		}
	}
}
