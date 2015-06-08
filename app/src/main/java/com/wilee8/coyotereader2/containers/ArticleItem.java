package com.wilee8.coyotereader2.containers;

import org.parceler.Parcel;

@Parcel
public class ArticleItem {
	private Boolean isFooter;

	public ArticleItem() {
		isFooter = false;
	}

	public Boolean getIsFooter() {
		return isFooter;
	}

	public void setIsFooter(Boolean isFooter) {
		this.isFooter = isFooter;
	}
}
