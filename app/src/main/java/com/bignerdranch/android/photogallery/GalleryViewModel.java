package com.bignerdranch.android.photogallery;

import androidx.lifecycle.AndroidViewModel;
import android.app.*;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.util.Log;
import java.util.*;

public class GalleryViewModel extends AndroidViewModel {
	private static final String TAG = "PhotoGallery";
	private MutableLiveData<List<GalleryItem>> mItemsLiveData;
	private ItemsLoader mPageLoader;
	private boolean isNextPagePreparing;
	private int mPage;
	private Application mApp;

	GalleryViewModel(Application app) {
		super(app);
		mApp = app;
		mItemsLiveData = new MutableLiveData<>();
		isNextPagePreparing = false;
		mPage = 1;
	}
	
	@Override
	protected void onCleared() {
		Log.i(TAG, "MV onCleared");
		if(mPageLoader != null)
			mPageLoader.cancel(true);
		super.onCleared();
	}

	public void loadNewPage() {
		if(mPageLoader != null)
			mPageLoader.cancel(true);
		mPageLoader = new ItemsLoader(this);
		mPageLoader.execute();
	}

	public void onReachedEndOfPage() {
		if(!isNextPagePreparing) {
			isNextPagePreparing = true;
			mPage += 1;
			loadNewPage();
		}
	}

	public void setItems(List<GalleryItem> items) {
		Log.i(TAG, "onChanged MV");

		if(isNextPagePreparing) { //first page?
			List<GalleryItem> currentList = mItemsLiveData.getValue();
			if(currentList == null)
				currentList = items;
			else
				currentList.addAll(items);

			mItemsLiveData.setValue(currentList);
		} else
			mItemsLiveData.setValue(items);

		if(items.size() == 0)
			mPage -= 1;
		
		isNextPagePreparing = false;
	}

	public LiveData<List<GalleryItem>> getItems() {
		return mItemsLiveData;
	}
	
	public void setPage(int page) {
		mPage = page;
	}
	
	public int getPage() {
		return mPage;
	}
	
	public String getQuery() {
		return QueryPreferences.getStoredQuery( mApp.getApplicationContext() );
	}
}

