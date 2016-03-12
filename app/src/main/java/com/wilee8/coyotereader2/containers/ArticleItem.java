package com.wilee8.coyotereader2.containers;

import android.os.Parcel;
import android.os.Parcelable;

public class ArticleItem implements Parcelable {
	String  id;
	String  title;
	String  summary;
	String  author;
	String  canonical;
	String  origin;
	Boolean starred;
	Boolean unread;
	Boolean isFooter;
	long    crawlTimeMsec;

	public ArticleItem() {
		isFooter = false;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getCanonical() {
		return canonical;
	}

	public void setCanonical(String canonical) {
		this.canonical = canonical;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Boolean getIsFooter() {
		return isFooter;
	}

	public void setIsFooter(Boolean isFooter) {
		this.isFooter = isFooter;
	}

	public Boolean getStarred() {
		return starred;
	}

	public void setStarred(Boolean starred) {
		this.starred = starred;
	}

	public Boolean getUnread() {
		return unread;
	}

	public void setUnread(Boolean unread) {
		this.unread = unread;
	}

	public long getCrawlTimeMsec() {
		return crawlTimeMsec;
	}

	public void setCrawlTimeMsec(long crawlTimeMsec) {
		this.crawlTimeMsec = crawlTimeMsec;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.id);
		dest.writeString(this.title);
		dest.writeString(this.summary);
		dest.writeString(this.author);
		dest.writeString(this.canonical);
		dest.writeString(this.origin);
		dest.writeValue(this.starred);
		dest.writeValue(this.unread);
		dest.writeValue(this.isFooter);
		dest.writeLong(this.crawlTimeMsec);
	}

	protected ArticleItem(Parcel in) {
		this.id = in.readString();
		this.title = in.readString();
		this.summary = in.readString();
		this.author = in.readString();
		this.canonical = in.readString();
		this.origin = in.readString();
		this.starred = (Boolean) in.readValue(Boolean.class.getClassLoader());
		this.unread = (Boolean) in.readValue(Boolean.class.getClassLoader());
		this.isFooter = (Boolean) in.readValue(Boolean.class.getClassLoader());
		this.crawlTimeMsec = in.readLong();
	}

	public static final Parcelable.Creator<ArticleItem> CREATOR = new Parcelable.Creator<ArticleItem>() {
		public ArticleItem createFromParcel(Parcel source) {
			return new ArticleItem(source);
		}

		public ArticleItem[] newArray(int size) {
			return new ArticleItem[size];
		}
	};
}
