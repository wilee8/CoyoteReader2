package com.wilee8.coyotereader2.gson;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class StreamContents implements Parcelable {
	@SerializedName("self")
	Self self;

	@SerializedName("description")
	String description;

	@SerializedName("direction")
	String direction;

	@SerializedName("continuation")
	String continuation;

	@SerializedName("id")
	String id;

	@SerializedName("title")
	String title;

	@SerializedName("updated")
	long updated;

	@SerializedName("items")
	ArrayList<Item> items;

	public String getContinuation() {
		return continuation;
	}

	public void setContinuation(String continuation) {
		this.continuation = continuation;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public ArrayList<Item> getItems() {
		return items;
	}

	public void setItems(ArrayList<Item> items) {
		this.items = items;
	}

	public Self getSelf() {
		return self;
	}

	public void setSelf(Self self) {
		this.self = self;
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
		dest.writeParcelable(this.self, 0);
		dest.writeString(this.description);
		dest.writeString(this.direction);
		dest.writeString(this.continuation);
		dest.writeString(this.id);
		dest.writeString(this.title);
		dest.writeLong(this.updated);
		dest.writeTypedList(items);
	}

	public StreamContents() {
	}

	protected StreamContents(Parcel in) {
		this.self = in.readParcelable(Self.class.getClassLoader());
		this.description = in.readString();
		this.direction = in.readString();
		this.continuation = in.readString();
		this.id = in.readString();
		this.title = in.readString();
		this.updated = in.readLong();
		this.items = in.createTypedArrayList(Item.CREATOR);
	}

	public static final Parcelable.Creator<StreamContents> CREATOR = new Parcelable.Creator<StreamContents>() {
		public StreamContents createFromParcel(Parcel source) {
			return new StreamContents(source);
		}

		public StreamContents[] newArray(int size) {
			return new StreamContents[size];
		}
	};
}
