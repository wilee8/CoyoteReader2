package com.wilee8.coyotereader2.gson;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class TokenResponse implements Parcelable {

	@SerializedName("access_token")
	String accessToken;

	@SerializedName("token_type")
	String tokenType;

	@SerializedName("expires_in")
	int expiresIn;

	@SerializedName("refresh_token")
	String refreshToken;

	@SerializedName("scope")
	String scope;

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.accessToken);
		dest.writeString(this.tokenType);
		dest.writeInt(this.expiresIn);
		dest.writeString(this.refreshToken);
		dest.writeString(this.scope);
	}

	public TokenResponse() {
	}

	protected TokenResponse(Parcel in) {
		this.accessToken = in.readString();
		this.tokenType = in.readString();
		this.expiresIn = in.readInt();
		this.refreshToken = in.readString();
		this.scope = in.readString();
	}

	public static final Parcelable.Creator<TokenResponse> CREATOR = new Parcelable.Creator<TokenResponse>() {
		@Override
		public TokenResponse createFromParcel(Parcel source) {
			return new TokenResponse(source);
		}

		@Override
		public TokenResponse[] newArray(int size) {
			return new TokenResponse[size];
		}
	};
}
