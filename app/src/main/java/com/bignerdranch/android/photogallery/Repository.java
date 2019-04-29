package com.bignerdranch.android.photogallery;

import java.util.*;
import android.content.*;

public class Repository implements IRepository {
	private static Repository mInstance;
	private Context mContext;
	private int mPage;
	private boolean isNextPagePreparing;
	private List<GalleryItem> mItems;
	
	public static Repository get(Context context) {
		if( mInstance == null )
			mInstance = new Repository(context);
		return mInstance;
	}

	private Repository(Context context) {
		mContext = context;
	} 
	
	@Override
	public void savePage(int page) {
		mPage = page;
	}

	@Override
	public int getPage() {
		return mPage;
	}

	@Override
	public void setNextPagePreparing(boolean isNextPagePreparing) {
		this.isNextPagePreparing = isNextPagePreparing;
	}

	@Override
	public boolean isNextPagePreparing() {
		return isNextPagePreparing;
	}

	@Override
	public void saveItems(List<GalleryItem> items) {
		mItems = items;
	}

	@Override
	public List<GalleryItem> getItems() {
		return mItems;
	}

	@Override
	public String getQuery() {
		return QueryPreferences.getStoredQuery(mContext);
	}

	@Override
	public void saveQuery(String query) {
		QueryPreferences.setStoredQuery(mContext, query);
	}
}

