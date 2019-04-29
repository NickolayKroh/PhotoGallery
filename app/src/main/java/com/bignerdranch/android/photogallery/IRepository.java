package com.bignerdranch.android.photogallery;

import java.util.*;

public interface IRepository {
	void savePage(int page);

	int getPage();

	void setNextPagePreparing(boolean isNextPagePreparing);

	boolean isNextPagePreparing();

	@SuppressWarnings("unused")
	void saveItems(List<GalleryItem> items);

	@SuppressWarnings("unused")
	List<GalleryItem> getItems();

	@SuppressWarnings("unused")
	void saveQuery(String query);
	
	String getQuery();
}
