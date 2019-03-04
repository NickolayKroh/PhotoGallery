package com.bignerdranch.android.photogallery.PhotoGalleryFragmentComponents;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.bignerdranch.android.photogallery.FlickrFetcher;
import com.bignerdranch.android.photogallery.GalleryItem;

import java.util.List;

public class ItemsLoader extends AsyncTaskLoader<List<GalleryItem>> {
	public final static String ARGS_PAGE = "page";
	public final static String ARGS_QUERY = "query";

	private String mQuery;
	private int mPage;

	public ItemsLoader(Context context, Bundle args) {
		super(context);
		mPage = args.getInt(ARGS_PAGE, 1);
		mQuery = args.getString(ARGS_QUERY, null);
	}

	@Nullable
	@Override
	public List<GalleryItem> loadInBackground() {
		Log.i("AsyncTaskLoader","background load");
		if( mQuery == null )
			return new FlickrFetcher().fetchRecentPhotos(mPage);
		else
			return new FlickrFetcher().searchPhotos(mQuery, mPage);
	}
}
