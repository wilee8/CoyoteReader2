package com.wilee8.coyotereader2.gson;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class UnreadCounts implements Parcelable {
	@SerializedName("unreadcounts")
	ArrayList<UnreadCount> unreadCounts;

	@SerializedName("max")
	int max;

	public ArrayList<UnreadCount> getUnreadCounts() {
		return unreadCounts;
	}

	public void setUnreadCounts(ArrayList<UnreadCount> unreadCounts) {
		this.unreadCounts = unreadCounts;
	}

	public int getMax() {
		return max;
	}

	public void setMax(int max) {
		this.max = max;
	}

	private UnreadCount findFeed(String id) {
		for (int i = 0; i < unreadCounts.size(); i++) {
			UnreadCount unreadCount = unreadCounts.get(i);

			// for some reason we get exceptions in this section. Can the returned UnreadCount be null?
			try {
				if (id.equals(unreadCount.getId())) {
					return unreadCount;
				}
			} catch (Exception e) {} // it's not the right feed if we get an error
		}

		return null;
	}

	public int getUnreadCount(String id) {
		UnreadCount unreadCount = findFeed(id);

		if (unreadCount == null) {
			return 0;
		} else {
			return unreadCount.getCount();
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeTypedList(unreadCounts);
		dest.writeInt(this.max);
	}

	public UnreadCounts() {
	}

	protected UnreadCounts(Parcel in) {
		this.unreadCounts = in.createTypedArrayList(UnreadCount.CREATOR);
		this.max = in.readInt();
	}

	public static final Parcelable.Creator<UnreadCounts> CREATOR = new Parcelable.Creator<UnreadCounts>() {
		public UnreadCounts createFromParcel(Parcel source) {
			return new UnreadCounts(source);
		}

		public UnreadCounts[] newArray(int size) {
			return new UnreadCounts[size];
		}
	};
}
