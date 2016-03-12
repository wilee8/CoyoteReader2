package com.wilee8.coyotereader2.gson;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class Item implements Parcelable {
	@SerializedName("canonical")
	ArrayList<Canonical> canonical;

	@SerializedName("origin")
	Origin origin;

	@SerializedName("author")
	String author;

	@SerializedName("categories")
	ArrayList<String> categories;

	@SerializedName("published")
	long published;

	@SerializedName("timestampUsec")
	long timestampUsec;

	@SerializedName("summary")
	Summary summary;

	@SerializedName("crawlTimeMsec")
	long crawlTimeMsec;

	@SerializedName("id")
	String id;

	@SerializedName("title")
	String title;

	@SerializedName("updated")
	long updated;

	@SerializedName("alternate")
	ArrayList<Alternate> alternate;

	public ArrayList<Alternate> getAlternate() {
		return alternate;
	}

	public void setAlternate(ArrayList<Alternate> alternate) {
		this.alternate = alternate;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public ArrayList<Canonical> getCanonical() {
		return canonical;
	}

	public void setCanonical(ArrayList<Canonical> canonical) {
		this.canonical = canonical;
	}

	public ArrayList<String> getCategories() {
		return categories;
	}

	public void setCategories(ArrayList<String> categories) {
		this.categories = categories;
	}

	public long getCrawlTimeMsec() {
		return crawlTimeMsec;
	}

	public void setCrawlTimeMsec(long crawlTimeMsec) {
		this.crawlTimeMsec = crawlTimeMsec;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Origin getOrigin() {
		return origin;
	}

	public void setOrigin(Origin origin) {
		this.origin = origin;
	}

	public long getPublished() {
		return published;
	}

	public void setPublished(long published) {
		this.published = published;
	}

	public Summary getSummary() {
		return summary;
	}

	public void setSummary(Summary summary) {
		this.summary = summary;
	}

	public long getTimestampUsec() {
		return timestampUsec;
	}

	public void setTimestampUsec(long timestampUsec) {
		this.timestampUsec = timestampUsec;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public long getUpdated() {
		return updated;
	}

	public void setUpdated(long updated) {
		this.updated = updated;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeTypedList(canonical);
		dest.writeParcelable(this.origin, 0);
		dest.writeString(this.author);
		dest.writeStringList(this.categories);
		dest.writeLong(this.published);
		dest.writeLong(this.timestampUsec);
		dest.writeParcelable(this.summary, 0);
		dest.writeLong(this.crawlTimeMsec);
		dest.writeString(this.id);
		dest.writeString(this.title);
		dest.writeLong(this.updated);
		dest.writeTypedList(alternate);
	}

	public Item() {
	}

	protected Item(Parcel in) {
		this.canonical = in.createTypedArrayList(Canonical.CREATOR);
		this.origin = in.readParcelable(Origin.class.getClassLoader());
		this.author = in.readString();
		this.categories = in.createStringArrayList();
		this.published = in.readLong();
		this.timestampUsec = in.readLong();
		this.summary = in.readParcelable(Summary.class.getClassLoader());
		this.crawlTimeMsec = in.readLong();
		this.id = in.readString();
		this.title = in.readString();
		this.updated = in.readLong();
		this.alternate = in.createTypedArrayList(Alternate.CREATOR);
	}

	public static final Parcelable.Creator<Item> CREATOR = new Parcelable.Creator<Item>() {
		public Item createFromParcel(Parcel source) {
			return new Item(source);
		}

		public Item[] newArray(int size) {
			return new Item[size];
		}
	};
}
