package com.wilee8.coyotereader2.gson;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

import java.util.ArrayList;

@Parcel
public class Subscription {
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
}
