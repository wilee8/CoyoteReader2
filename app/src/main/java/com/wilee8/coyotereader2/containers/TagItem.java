package com.wilee8.coyotereader2.containers;

import org.parceler.Parcel;

import java.util.ArrayList;

@Parcel
public class TagItem {
	String             id;
	String             name;
	int                unreadCount;
	Boolean            isFeed;
	int                resId;
	String             iconUrl;
	ArrayList<TagItem> feeds;
	Boolean            isExpanded;
	Boolean            isTopLevel;

	public TagItem() {
		unreadCount = 0;
		resId = 0;
		isExpanded = false;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getUnreadCount() {
		return unreadCount;
	}

	public void setUnreadCount(int unreadCount) {
		this.unreadCount = unreadCount;
	}

	public Boolean getIsFeed() {
		return isFeed;
	}

	public void setIsFeed(Boolean isFeed) {
		this.isFeed = isFeed;
	}

	public int getResId() {
		return resId;
	}

	public void setResId(int resId) {
		this.resId = resId;
	}

	public String getIconUrl() {
		return iconUrl;
	}

	public void setIconUrl(String iconUrl) {
		this.iconUrl = iconUrl;
	}

	public ArrayList<TagItem> getFeeds() {
		return feeds;
	}

	public void setFeeds(ArrayList<TagItem> mFeeds) {
		this.feeds = mFeeds;
	}

	public Boolean getIsExpanded() {
		return isExpanded;
	}

	public void setIsExpanded(Boolean isExpanded) {
		this.isExpanded = isExpanded;
	}

	public Boolean getIsTopLevel() {
		return isTopLevel;
	}

	public void setIsTopLevel(Boolean isTopLevel) {
		this.isTopLevel = isTopLevel;
	}
}
