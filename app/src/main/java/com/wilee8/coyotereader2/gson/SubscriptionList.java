package com.wilee8.coyotereader2.gson;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

import java.util.ArrayList;

@Parcel
public class SubscriptionList {
	@SerializedName("subscriptions")
	ArrayList<Subscription>	subscriptions;

	public ArrayList<Subscription> getSubscriptions() {
		return subscriptions;
	}

	public void setSubscriptions(ArrayList<Subscription> subscriptions) {
		this.subscriptions = subscriptions;
	}
}
