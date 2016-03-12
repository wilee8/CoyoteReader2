package com.wilee8.coyotereader2.gson;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class TagList implements Parcelable {
	@SerializedName("tags")
	ArrayList<Tag> tags;

	public ArrayList<Tag> getTags() {
		return tags;
	}

	public void setTags(ArrayList<Tag> tags) {
		this.tags = tags;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeTypedList(tags);
	}

	public TagList() {
	}

	protected TagList(Parcel in) {
		this.tags = in.createTypedArrayList(Tag.CREATOR);
	}

	public static final Parcelable.Creator<TagList> CREATOR = new Parcelable.Creator<TagList>() {
		public TagList createFromParcel(Parcel source) {
			return new TagList(source);
		}

		public TagList[] newArray(int size) {
			return new TagList[size];
		}
	};
}
