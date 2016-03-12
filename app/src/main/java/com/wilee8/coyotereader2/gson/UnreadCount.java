package com.wilee8.coyotereader2.gson;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class UnreadCount implements Parcelable {
	@SerializedName("newestItemTimestampUsec")
	long newestItemTimestampUsec;

	@SerializedName("count")
	int count;

	@SerializedName("id")
	String id;

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

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(this.newestItemTimestampUsec);
		dest.writeInt(this.count);
		dest.writeString(this.id);
	}

	public UnreadCount() {
	}

	protected UnreadCount(Parcel in) {
		this.newestItemTimestampUsec = in.readLong();
		this.count = in.readInt();
		this.id = in.readString();
	}

	public static final Parcelable.Creator<UnreadCount> CREATOR = new Parcelable.Creator<UnreadCount>() {
		public UnreadCount createFromParcel(Parcel source) {
			return new UnreadCount(source);
		}

		public UnreadCount[] newArray(int size) {
			return new UnreadCount[size];
		}
	};
}
