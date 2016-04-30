package com.wilee8.coyotereader2.gson;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class StreamPrefs implements Parcelable {
    @SerializedName("streamprefs")
    Map<String, ArrayList<StreamPref>> streamPrefs;

    public Map<String, ArrayList<StreamPref>> getStreamPrefs() {
        return streamPrefs;
    }

    public void setStreamPrefs(Map<String, ArrayList<StreamPref>> streamPrefs) {
        this.streamPrefs = streamPrefs;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.streamPrefs.size());
        for (Map.Entry<String, ArrayList<StreamPref>> entry : this.streamPrefs.entrySet()) {
            dest.writeString(entry.getKey());
            dest.writeTypedList(entry.getValue());
        }

    }

    public StreamPrefs() {
    }

    protected StreamPrefs(Parcel in) {
        int size = in.readInt();
        this.streamPrefs = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String key = in.readString();
            ArrayList<StreamPref> value = in.createTypedArrayList(StreamPref.CREATOR);
            this.streamPrefs.put(key, value);
        }
    }

    public static final Parcelable.Creator<StreamPrefs> CREATOR = new Parcelable.Creator<StreamPrefs>() {
        public StreamPrefs createFromParcel(Parcel source) {
            return new StreamPrefs(source);
        }

        public StreamPrefs[] newArray(int size) {
            return new StreamPrefs[size];
        }
    };
}
