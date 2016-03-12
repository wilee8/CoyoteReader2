package com.wilee8.coyotereader2.containers;

import android.os.Parcel;
import android.os.Parcelable;

public class ArticleScrollState implements Parcelable {
	int scrollX;
	int scrollY;

	public ArticleScrollState () {
		this.scrollX = -1;
		this.scrollY = -1;
	}

	public ArticleScrollState (int scrollX, int scrollY) {
		this.scrollX = scrollX;
		this.scrollY = scrollY;
	}

	public int getScrollX() {
		return scrollX;
	}

	public void setScrollX(int scrollX) {
		this.scrollX = scrollX;
	}

	public int getScrollY() {
		return scrollY;
	}

	public void setScrollY(int scrollY) {
		this.scrollY = scrollY;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(this.scrollX);
		dest.writeInt(this.scrollY);
	}

	protected ArticleScrollState(Parcel in) {
		this.scrollX = in.readInt();
		this.scrollY = in.readInt();
	}

	public static final Parcelable.Creator<ArticleScrollState> CREATOR = new Parcelable.Creator<ArticleScrollState>() {
		public ArticleScrollState createFromParcel(Parcel source) {
			return new ArticleScrollState(source);
		}

		public ArticleScrollState[] newArray(int size) {
			return new ArticleScrollState[size];
		}
	};
}
