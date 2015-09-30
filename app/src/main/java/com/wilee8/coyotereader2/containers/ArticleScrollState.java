package com.wilee8.coyotereader2.containers;

import org.parceler.Parcel;

@Parcel
public class ArticleScrollState {
	private int scrollX;
	private int scrollY;

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
}
