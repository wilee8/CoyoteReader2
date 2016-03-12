package com.wilee8.coyotereader2.gson;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class AddedFeed implements Parcelable {

	@SerializedName("query")
	String query;

	@SerializedName("numResults")
	int numResults;

	@SerializedName("streamId")
	String streamId;

	@SerializedName("streamName")
	String streamName;

	public int getNumResults() {
		return numResults;
	}

	public void setNumResults(int numResults) {
		this.numResults = numResults;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}

	public String getStreamName() {
		return streamName;
	}

	public void setStreamName(String streamName) {
		this.streamName = streamName;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.query);
		dest.writeInt(this.numResults);
		dest.writeString(this.streamId);
		dest.writeString(this.streamName);
	}

	public AddedFeed() {
	}

	protected AddedFeed(Parcel in) {
		this.query = in.readString();
		this.numResults = in.readInt();
		this.streamId = in.readString();
		this.streamName = in.readString();
	}

	public static final Parcelable.Creator<AddedFeed> CREATOR = new Parcelable.Creator<AddedFeed>() {
		public AddedFeed createFromParcel(Parcel source) {
			return new AddedFeed(source);
		}

		public AddedFeed[] newArray(int size) {
			return new AddedFeed[size];
		}
	};
}
