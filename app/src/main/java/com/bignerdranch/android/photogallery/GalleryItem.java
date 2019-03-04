package com.bignerdranch.android.photogallery;

import com.google.gson.annotations.SerializedName;
import android.net.*;

public class GalleryItem {
	@SerializedName("title")
	private String mCaption;

	@SerializedName("id")
	private String mId;

	@SerializedName("url_s")
	private String mUrl;

	@SerializedName("owner")
	private String mOwner;

	public Uri getPhotoPageUri() {
		return Uri.parse("http://www.flickr.com/photos/")
			.buildUpon()
			.appendPath(mOwner)
			.appendPath(mId)
			.build();
	}
	
	public void setOwner(String owner) {
		mOwner = owner;
	}

	public String getOwner() {
		return mOwner;
	}

	public void setUrl( String Url ) {
		mUrl = Url;
	}

	public String getUrl()
	{
		return mUrl;
	}

	public void setId( String Id )
	{
		mId = Id;
	}

	public String getId()
	{
		return mId;
	}

	public void setCaption( String Caption ) {
		mCaption = Caption;
	}

	public String getCaption()
	{
		return mCaption;
	}

	@Override
	public String toString()
	{
		return mCaption;
	}
}
