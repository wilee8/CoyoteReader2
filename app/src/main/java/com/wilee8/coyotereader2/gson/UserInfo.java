package com.wilee8.coyotereader2.gson;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class UserInfo implements Parcelable {
	@SerializedName("userId")
	String userId;

	@SerializedName("userName")
	String userName;

	@SerializedName("userProfileId")
	String userProfileId;

	@SerializedName("userEmail")
	String userEmail;

	@SerializedName("isBloggerUser")
	Boolean isBloggerUser;

	@SerializedName("signupTimeSec")
	long signupTimeSec;

	@SerializedName("isMultiLoginEnabled")
	Boolean isMultiLoginEnabled;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getUserProfileId() {
		return userProfileId;
	}

	public void setUserProfileId(String userProfileId) {
		this.userProfileId = userProfileId;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	public Boolean getIsBloggerUser() {
		return isBloggerUser;
	}

	public void setIsBloggerUser(Boolean isBloggerUser) {
		this.isBloggerUser = isBloggerUser;
	}

	public long getSignupTimeSec() {
		return signupTimeSec;
	}

	public void setSignupTimeSec(long signupTimeSec) {
		this.signupTimeSec = signupTimeSec;
	}

	public Boolean getIsMultiLoginEnabled() {
		return isMultiLoginEnabled;
	}

	public void setIsMultiLoginEnabled(Boolean isMultiLoginEnabled) {
		this.isMultiLoginEnabled = isMultiLoginEnabled;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.userId);
		dest.writeString(this.userName);
		dest.writeString(this.userProfileId);
		dest.writeString(this.userEmail);
		dest.writeValue(this.isBloggerUser);
		dest.writeLong(this.signupTimeSec);
		dest.writeValue(this.isMultiLoginEnabled);
	}

	public UserInfo() {
	}

	protected UserInfo(Parcel in) {
		this.userId = in.readString();
		this.userName = in.readString();
		this.userProfileId = in.readString();
		this.userEmail = in.readString();
		this.isBloggerUser = (Boolean) in.readValue(Boolean.class.getClassLoader());
		this.signupTimeSec = in.readLong();
		this.isMultiLoginEnabled = (Boolean) in.readValue(Boolean.class.getClassLoader());
	}

	public static final Parcelable.Creator<UserInfo> CREATOR = new Parcelable.Creator<UserInfo>() {
		public UserInfo createFromParcel(Parcel source) {
			return new UserInfo(source);
		}

		public UserInfo[] newArray(int size) {
			return new UserInfo[size];
		}
	};
}
