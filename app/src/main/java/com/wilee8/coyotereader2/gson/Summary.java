package com.wilee8.coyotereader2.gson;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class Summary implements Parcelable {
	@SerializedName("direction")
	String direction;

	@SerializedName("content")
	String content;

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.direction);
		dest.writeString(this.content);
	}

	public Summary() {
	}

	protected Summary(Parcel in) {
		this.direction = in.readString();
		this.content = in.readString();
	}

	public static final Parcelable.Creator<Summary> CREATOR = new Parcelable.Creator<Summary>() {
		public Summary createFromParcel(Parcel source) {
			return new Summary(source);
		}

		public Summary[] newArray(int size) {
			return new Summary[size];
		}
	};
}
