package com.wilee8.coyotereader2.gson;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class Alternate implements Parcelable {
	@SerializedName("href")
	String href;

	@SerializedName("type")
	String type;

	public String getHref() {
		return href;
	}

	public void setHref(String href) {
		this.href = href;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.href);
		dest.writeString(this.type);
	}

	public Alternate() {
	}

	protected Alternate(Parcel in) {
		this.href = in.readString();
		this.type = in.readString();
	}

	public static final Parcelable.Creator<Alternate> CREATOR = new Parcelable.Creator<Alternate>() {
		public Alternate createFromParcel(Parcel source) {
			return new Alternate(source);
		}

		public Alternate[] newArray(int size) {
			return new Alternate[size];
		}
	};
}
