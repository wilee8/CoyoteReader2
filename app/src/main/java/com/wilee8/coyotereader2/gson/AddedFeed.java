package com.wilee8.coyotereader2.gson;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

@Parcel
public class AddedFeed {

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
}
