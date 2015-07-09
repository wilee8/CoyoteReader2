package com.wilee8.coyotereader2;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wilee8.coyotereader2.containers.ArticleItem;

import org.parceler.Parcels;

import java.util.ArrayList;

public class ArticlePagerFragment extends Fragment {

	private ArticlePagerFragmentListener mCallback;
	private ViewPager                    mPager;
	private ArticlePagerAdapter          mPagerAdapter;
	private ArrayList<ArticleItem>       mItems;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			mCallback = (ArticlePagerFragmentListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement ArticlePagerFragmentListener");
		}
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
		mPagerAdapter = new ArticlePagerAdapter(getChildFragmentManager());
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
	}

	private class ArticlePagerAdapter extends FragmentStatePagerAdapter {
		private ArticleFragment[] mFragmentList;

		public ArticlePagerAdapter(FragmentManager fm) {
			super(fm);

			int size = mItems.size();
			mFragmentList = new ArticleFragment[size];
			for (int i = 0; i < size; i++) {
				mFragmentList[i] = null;
			}
		}

		@Override
		public Fragment getItem(int position) {
			ArticleItem item = mItems.get(position);
			Bundle args = new Bundle();
			args.putParcelable("articleItem", Parcels.wrap(item));

			ArticleFragment fragment = new ArticleFragment();
			fragment.setArguments(args);

			mFragmentList[position] = fragment;
			return fragment;
		}

		@Override
		public int getCount() {
			return mFragmentList.length;
		}

		@Override
		public void notifyDataSetChanged() {
			int oldSize = mFragmentList.length;
			int newSize = mItems.size();

			if (oldSize > newSize) {
				// we can just ignore the extra element that we are losing
				// this should never happen anyway
				oldSize = newSize;
			}

			ArticleFragment newFragmentList[] = new ArticleFragment[newSize];

			// copy old elements to new array
			System.arraycopy(mFragmentList, 0, newFragmentList, 0, oldSize);

			for (int i = oldSize; i < newSize; i++) {
				newFragmentList[i] = null;
			}

			mFragmentList = newFragmentList;

			super.notifyDataSetChanged();
		}

		public boolean articleExists(int position) {
			return mFragmentList[position] != null;
		}

		public Fragment returnExistingElement(int position) {
			return mFragmentList[position];
		}
	}

	private class ArticleOnPageChangeListener extends ViewPager.SimpleOnPageChangeListener {

		@Override
		public void onPageSelected(int position) {
			super.onPageSelected(position);

			mCallback.onArticleSelected(position);
		}
	}
}
