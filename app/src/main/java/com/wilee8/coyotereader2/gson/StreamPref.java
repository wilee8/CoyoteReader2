package com.wilee8.coyotereader2.gson;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class StreamPref implements Parcelable {
	@SerializedName("value")
	String value;

	@SerializedName("id")
	String id;

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
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
		dest.writeString(this.value);
		dest.writeString(this.id);
	}

	public StreamPref() {
	}

	protected StreamPref(Parcel in) {
		this.value = in.readString();
		this.id = in.readString();
	}

	public static final Parcelable.Creator<StreamPref> CREATOR = new Parcelable.Creator<StreamPref>() {
		public StreamPref createFromParcel(Parcel source) {
			return new StreamPref(source);
		}

		public StreamPref[] newArray(int size) {
			return new StreamPref[size];
		}
	};
}
