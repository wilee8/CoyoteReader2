package com.wilee8.coyotereader2.gson;

import java.util.ArrayList;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

@Parcel
public class SubscriptionList {
	@SerializedName("subscriptions")
	private ArrayList<Subscription>	subscriptions;

	public ArrayList<Subscription> getSubscriptions() {
		return subscriptions;
	}

	public void setSubscriptions(ArrayList<Subscription> subscriptions) {
		this.subscriptions = subscriptions;
	}
}
