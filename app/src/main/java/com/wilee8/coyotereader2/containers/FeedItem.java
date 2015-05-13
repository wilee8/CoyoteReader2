package com.wilee8.coyotereader2.containers;

import org.parceler.Parcel;

@Parcel
public class FeedItem {
	private String feedId;
	private String feedTitle;
	private int    unreadCount;
	private String feedIconUrl;

	public String getFeedId() {
		return feedId;
	}

	public void setFeedId(String feedId) {
		this.feedId = feedId;
	}

	public String getFeedTitle() {
		return feedTitle;
	}

	public void setFeedTitle(String feedTitle) {
		this.feedTitle = feedTitle;
	}

	public int getUnreadCount() {
		return unreadCount;
	}

	public void setUnreadCount(int unreadCount) {
		this.unreadCount = unreadCount;
	}

	public String getFeedIconUrl() {
		return feedIconUrl;
	}

	public void setFeedIconUrl(String feedIconUrl) {
		this.feedIconUrl = feedIconUrl;
	}
}
