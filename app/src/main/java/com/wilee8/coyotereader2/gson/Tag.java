package com.wilee8.coyotereader2.gson;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class Tag implements Parcelable {
	@SerializedName("sortid")
	String sortId;

	@SerializedName("id")
	String id;

	public String getSortId() {
		return sortId;
	}

	public void setSortId(String sortId) {
		this.sortId = sortId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.sortId);
		dest.writeString(this.id);
	}

	public Tag() {
	}

	protected Tag(Parcel in) {
		this.sortId = in.readString();
		this.id = in.readString();
	}

	public static final Parcelable.Creator<Tag> CREATOR = new Parcelable.Creator<Tag>() {
		public Tag createFromParcel(Parcel source) {
			return new Tag(source);
		}

		public Tag[] newArray(int size) {
			return new Tag[size];
		}
	};
}
