package com.wilee8.coyotereader2.gson;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class Subscription implements Parcelable {
	@SerializedName("sortid")
	String sortId;

	@SerializedName("iconUrl")
	String iconUrl;

	@SerializedName("categories")
	ArrayList<Category> categories;

	@SerializedName("htmlUrl")
	String htmlUrl;

	@SerializedName("url")
	String url;

	@SerializedName("id")
	String id;

	@SerializedName("title")
	String title;

	@SerializedName("firstitemmsec")
	long firstItemMsec;

	public String getSortId() {
		return sortId;
	}

	public void setSortId(String sortId) {
		this.sortId = sortId;
	}

	public String getIconUrl() {
		return iconUrl;
	}

	public void setIconUrl(String iconUrl) {
		this.iconUrl = iconUrl;
	}

	public ArrayList<Category> getCategories() {
		return categories;
	}

	public void setCategories(ArrayList<Category> categories) {
		this.categories = categories;
	}

	public String getHtmlUrl() {
		return htmlUrl;
	}

	public void setHtmlUrl(String htmlUrl) {
		this.htmlUrl = htmlUrl;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public long getFirstItemMsec() {
		return firstItemMsec;
	}

	public void setFirstItemMsec(long firstItemMsec) {
		this.firstItemMsec = firstItemMsec;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.sortId);
		dest.writeString(this.iconUrl);
		dest.writeTypedList(categories);
		dest.writeString(this.htmlUrl);
		dest.writeString(this.url);
		dest.writeString(this.id);
		dest.writeString(this.title);
		dest.writeLong(this.firstItemMsec);
	}

	public Subscription() {
	}

	protected Subscription(Parcel in) {
		this.sortId = in.readString();
		this.iconUrl = in.readString();
		this.categories = in.createTypedArrayList(Category.CREATOR);
		this.htmlUrl = in.readString();
		this.url = in.readString();
		this.id = in.readString();
		this.title = in.readString();
		this.firstItemMsec = in.readLong();
	}

	public static final Parcelable.Creator<Subscription> CREATOR = new Parcelable.Creator<Subscription>() {
		public Subscription createFromParcel(Parcel source) {
			return new Subscription(source);
		}

		public Subscription[] newArray(int size) {
			return new Subscription[size];
		}
	};
}
