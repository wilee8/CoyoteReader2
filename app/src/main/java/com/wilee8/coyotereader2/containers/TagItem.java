package com.wilee8.coyotereader2.containers;

import org.parceler.Parcel;

@Parcel
public class TagItem {
	private String  id;
	private String  name;
	private int     unreadCount;
	private Boolean isFeed;
	private int     resId;
	private String  iconUrl;

	public TagItem() {
		unreadCount = 0;
		resId = 0;
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
}
