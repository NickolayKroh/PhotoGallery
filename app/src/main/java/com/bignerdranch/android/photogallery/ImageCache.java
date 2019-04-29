package com.bignerdranch.android.photogallery;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.collection.LruCache;

public class ImageCache extends LruCache<String, Bitmap> {
	private static ImageCache sImageCache;

	private ImageCache(int maxSize) {
		super(maxSize);
	}

	public static ImageCache getInstance(){
		if(sImageCache == null)
			sImageCache = new ImageCache(getHalfAvailableMemoryKBytes());
		return sImageCache;
	}

	private static int getHalfAvailableMemoryKBytes() {
		//Context context = PhotoGalleryApplication.getContext();
		//ActivityManager mgr = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		return (int) ( Runtime.getRuntime().maxMemory() / 1024 / 2 );
	}

	@Override
	protected int sizeOf(@NonNull String key, @NonNull Bitmap bitmap) {
		return bitmap.getByteCount() / 1024; //KBytes as in constructor
	}
}
