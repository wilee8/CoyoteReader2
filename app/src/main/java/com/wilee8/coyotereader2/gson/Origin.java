package com.wilee8.coyotereader2.gson;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class Origin implements Parcelable {
	@SerializedName("htmlUrl")
	String htmlUrl;

	@SerializedName("title")
	String title;

	@SerializedName("streamId")
	String streamId;

	public String getHtmlUrl() {
		return htmlUrl;
	}

	public void setHtmlUrl(String htmlUrl) {
		this.htmlUrl = htmlUrl;
	}

	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.htmlUrl);
		dest.writeString(this.title);
		dest.writeString(this.streamId);
	}

	public Origin() {
	}

	protected Origin(Parcel in) {
		this.htmlUrl = in.readString();
		this.title = in.readString();
		this.streamId = in.readString();
	}

	public static final Parcelable.Creator<Origin> CREATOR = new Parcelable.Creator<Origin>() {
		public Origin createFromParcel(Parcel source) {
			return new Origin(source);
		}

		public Origin[] newArray(int size) {
			return new Origin[size];
		}
	};
}
