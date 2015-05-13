package com.wilee8.coyotereader2.gson;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

import java.util.ArrayList;

@Parcel
public class Item {
	@SerializedName("canonical")
	private ArrayList<Canonical> canonical;

	@SerializedName("origin")
	private Origin origin;

	@SerializedName("author")
	private String author;

	@SerializedName("categories")
	private ArrayList<String> categories;

	@SerializedName("published")
	private long published;

	@SerializedName("timestampUsec")
	private long timestampUsec;

	@SerializedName("summary")
	private Summary summary;

	@SerializedName("crawlTimeMsec")
	private long crawlTimeMsec;

	@SerializedName("id")
	private String id;

	@SerializedName("title")
	private String title;

	@SerializedName("updated")
	private long updated;

	@SerializedName("alternate")
	private ArrayList<Alternate> alternate;
}
