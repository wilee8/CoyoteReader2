package com.wilee8.coyotereader2.gson;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class Self implements Parcelable {
	@SerializedName("href")
	String href;

	public String getHref() {
		return href;
	}

	public void setHref(String href) {
		this.href = href;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.href);
	}

	public Self() {
	}

	protected Self(Parcel in) {
		this.href = in.readString();
	}

	public static final Parcelable.Creator<Self> CREATOR = new Parcelable.Creator<Self>() {
		public Self createFromParcel(Parcel source) {
			return new Self(source);
		}

		public Self[] newArray(int size) {
			return new Self[size];
		}
	};
}
