package com.bignerdranch.android.photogallery.PhotoGalleryFragmentComponents;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import com.bignerdranch.android.photogallery.PhotoGalleryFragment;

public class ScrollListener extends RecyclerView.OnScrollListener {
	PhotoGalleryFragment mPhotoGalleryFragment;

	public ScrollListener(PhotoGalleryFragment photoGalleryFragment){
		mPhotoGalleryFragment = photoGalleryFragment;
	}

	@Override
	public void onScrolled( @NonNull RecyclerView recyclerView, int dx, int dy ) {
		if( isEndOfPageReached( recyclerView, dy ) )
			mPhotoGalleryFragment.onReachedEndOfPage();
	}

	@Override
	public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
		super.onScrollStateChanged(recyclerView, newState);
	}

	private boolean isEndOfPageReached(@NonNull RecyclerView recyclerView, int dy) {
		return dy > 0 && recyclerView.computeVerticalScrollRange() <= 2*recyclerView.computeVerticalScrollExtent() + recyclerView.computeVerticalScrollOffset();
	}
}
