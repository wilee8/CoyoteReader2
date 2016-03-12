package com.wilee8.coyotereader2.gson;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class Canonical implements Parcelable {
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

	public Canonical() {
	}

	protected Canonical(Parcel in) {
		this.href = in.readString();
	}

	public static final Parcelable.Creator<Canonical> CREATOR = new Parcelable.Creator<Canonical>() {
		public Canonical createFromParcel(Parcel source) {
			return new Canonical(source);
		}

		public Canonical[] newArray(int size) {
			return new Canonical[size];
		}
	};
}
