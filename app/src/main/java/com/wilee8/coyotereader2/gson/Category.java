package com.wilee8.coyotereader2.gson;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class Category implements Parcelable {
	@SerializedName("label")
	String label;

	@SerializedName("id")
	String id;

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
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
		dest.writeString(this.label);
		dest.writeString(this.id);
	}

	public Category() {
	}

	protected Category(Parcel in) {
		this.label = in.readString();
		this.id = in.readString();
	}

	public static final Parcelable.Creator<Category> CREATOR = new Parcelable.Creator<Category>() {
		public Category createFromParcel(Parcel source) {
			return new Category(source);
		}

		public Category[] newArray(int size) {
			return new Category[size];
		}
	};
}