package com.wilee8.coyotereader2.gson;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class SubscriptionList implements Parcelable {
	@SerializedName("subscriptions")
	ArrayList<Subscription>	subscriptions;

	public ArrayList<Subscription> getSubscriptions() {
		return subscriptions;
	}

	public void setSubscriptions(ArrayList<Subscription> subscriptions) {
		this.subscriptions = subscriptions;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeTypedList(subscriptions);
	}

	public SubscriptionList() {
	}

	protected SubscriptionList(Parcel in) {
		this.subscriptions = in.createTypedArrayList(Subscription.CREATOR);
	}

	public static final Parcelable.Creator<SubscriptionList> CREATOR = new Parcelable.Creator<SubscriptionList>() {
		public SubscriptionList createFromParcel(Parcel source) {
			return new SubscriptionList(source);
		}

		public SubscriptionList[] newArray(int size) {
			return new SubscriptionList[size];
		}
	};
}
