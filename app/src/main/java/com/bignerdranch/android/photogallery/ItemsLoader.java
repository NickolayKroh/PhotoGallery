package com.bignerdranch.android.photogallery;

import java.util.*;
import android.os.*;
import androidx.annotation.NonNull;
import android.util.*;

public class ItemsLoader extends AsyncTask<Void, Void, List<GalleryItem>> {
	private static final String TAG = "PhotoGallery";

	private GalleryViewModel mViewModel;
	private String mQuery;
	private int mPage;

	ItemsLoader(@NonNull GalleryViewModel vm) {
		mPage = vm.getPage();
		mQuery = vm.getQuery();
		mViewModel = vm;
	}

	@Override
	protected List<GalleryItem> doInBackground(Void... voids) {
		Log.i(TAG, "background load. page - " + mPage);
		if( mQuery == null )
			return new FlickrFetcher().fetchRecentPhotos(mPage);
		else
			return new FlickrFetcher().searchPhotos(mQuery, mPage);
	}

	@Override
	protected void onPostExecute(@NonNull List<GalleryItem> items) {
		mViewModel.setItems(items);
	}
}
