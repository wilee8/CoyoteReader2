package com.wilee8.coyotereader2.containers;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class TagItem implements Parcelable {
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

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.id);
		dest.writeString(this.name);
		dest.writeInt(this.unreadCount);
		dest.writeValue(this.isFeed);
		dest.writeInt(this.resId);
		dest.writeString(this.iconUrl);
		dest.writeList(this.feeds);
		dest.writeValue(this.isExpanded);
		dest.writeValue(this.isTopLevel);
	}

	protected TagItem(Parcel in) {
		this.id = in.readString();
		this.name = in.readString();
		this.unreadCount = in.readInt();
		this.isFeed = (Boolean) in.readValue(Boolean.class.getClassLoader());
		this.resId = in.readInt();
		this.iconUrl = in.readString();
		this.feeds = new ArrayList<TagItem>();
		in.readList(this.feeds, List.class.getClassLoader());
		this.isExpanded = (Boolean) in.readValue(Boolean.class.getClassLoader());
		this.isTopLevel = (Boolean) in.readValue(Boolean.class.getClassLoader());
	}

	public static final Parcelable.Creator<TagItem> CREATOR = new Parcelable.Creator<TagItem>() {
		public TagItem createFromParcel(Parcel source) {
			return new TagItem(source);
		}

		public TagItem[] newArray(int size) {
			return new TagItem[size];
		}
	};
}
