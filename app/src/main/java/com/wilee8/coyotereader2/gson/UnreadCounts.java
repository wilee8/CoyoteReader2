package com.wilee8.coyotereader2.gson;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

import java.util.ArrayList;

@Parcel
public class UnreadCounts {
	@SerializedName("unreadcounts")
	private ArrayList<UnreadCount> unreadCounts;

	@SerializedName("max")
	private int max;

	public ArrayList<UnreadCount> getUnreadCounts() {
		return unreadCounts;
	}

	public void setUnreadCounts(ArrayList<UnreadCount> unreadCounts) {
		this.unreadCounts = unreadCounts;
	}

	public int getMax() {
		return max;
	}

	public void setMax(int max) {
		this.max = max;
	}

	private UnreadCount findFeed(String id) {
		for (int i = 0; i < unreadCounts.size(); i++) {
			UnreadCount unreadCount = unreadCounts.get(i);

			// for some reason we get exceptions in this section. Can the returned UnreadCount be null?
			try {
				if (id.equals(unreadCount.getId())) {
					return unreadCount;
				}
			} catch (Exception e) {} // it's not the right feed if we get an error
		}

		return null;
	}

	public int getUnreadCount(String id) {
		UnreadCount unreadCount = findFeed(id);

		if (unreadCount == null) {
			return 0;
		} else {
			return unreadCount.getCount();
		}
	}
}
