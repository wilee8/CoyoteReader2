package com.wilee8.coyotereader2.gson;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

@Parcel
public class UnreadCount {
	@SerializedName("newestItemTimestampUsec")
	private long newestItemTimestampUsec;

	@SerializedName("count")
	private int count;

	@SerializedName("id")
	private String id;

	public long getNewestItemTimestampUsec() {
		return newestItemTimestampUsec;
	}

	public void setNewestItemTimestampUsec(long newestItemTimestampUsec) {
		this.newestItemTimestampUsec = newestItemTimestampUsec;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
