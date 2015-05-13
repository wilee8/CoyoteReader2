package com.wilee8.coyotereader2.gson;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.Map;

@Parcel
public class StreamPrefs {
	@SerializedName("streamprefs")
	private Map<String, ArrayList<StreamPref>> streamPrefs;

	public Map<String, ArrayList<StreamPref>> getStreamPrefs() {
		return streamPrefs;
	}

	public void setStreamPrefs(Map<String, ArrayList<StreamPref>> streamPrefs) {
		this.streamPrefs = streamPrefs;
	}
}
