package com.wilee8.coyotereader2.gson;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

@Parcel
public class Tag {
	@SerializedName("sortid")
	String sortId;

	@SerializedName("id")
	String id;

	public String getSortId() {
		return sortId;
	}

	public void setSortId(String sortId) {
		this.sortId = sortId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
