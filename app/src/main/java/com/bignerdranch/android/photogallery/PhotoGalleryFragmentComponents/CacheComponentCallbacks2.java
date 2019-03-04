package com.bignerdranch.android.photogallery.PhotoGalleryFragmentComponents;

import android.content.ComponentCallbacks2;
import android.content.res.Configuration;
import android.util.Log;
import com.bignerdranch.android.photogallery.ImageCache;

public class CacheComponentCallbacks2 implements ComponentCallbacks2 {
	private static final String TAG = "PhotoGallery";

	@Override
	public void onTrimMemory(int level) {
		Log.i(TAG,"onTrim - " + level);

		if( level >= TRIM_MEMORY_MODERATE )
			ImageCache.getInstance().evictAll();
		else if( level >= TRIM_MEMORY_RUNNING_MODERATE )
			ImageCache.getInstance().trimToSize(ImageCache.getInstance().size() / 2);
	}

	@Override
	public void onConfigurationChanged(Configuration configuration) {}

	@Override
	public void onLowMemory() {
		Log.i(TAG,"onLowMemory");
		ImageCache.getInstance().evictAll();
	}
}
