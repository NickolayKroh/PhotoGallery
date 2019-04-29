package com.bignerdranch.android.photogallery.PhotoGalleryFragmentComponents;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bignerdranch.android.photogallery.*;

public class ScrollListener extends RecyclerView.OnScrollListener {
	//private static final String TAG = "PhotoGallery";
	
	private GalleryViewModel mViewModel;
	private PhotoGalleryFragment mFragment;
	private float mItemHeight;

	public ScrollListener(GalleryViewModel viewModel, @NonNull PhotoGalleryFragment fragment){
		mViewModel = viewModel;
		if(fragment.getActivity() == null)
			mItemHeight = 40;
		else
			mItemHeight = fragment.getActivity().getResources().getDimension(R.dimen.item_height_dp);
		mFragment = fragment;
	}

	@Override
	public void onScrolled( @NonNull RecyclerView rv, int dx, int dy ) {
		if(dy > 0)
			mFragment.preCachingDown( findTopSpanOnScreen(rv) );
			if( isEndOfPageReached(rv) )
				mViewModel.onReachedEndOfPage();
		else if(dy < 0)
			mFragment.preCachingUp( findTopSpanOnScreen(rv) );
	}

	@Override
	public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
		super.onScrollStateChanged(recyclerView, newState);
	}
	
	private int findTopSpanOnScreen(@NonNull RecyclerView rv) {
		return mFragment.getSpanCount()*Math.round(rv.computeVerticalScrollOffset()/mItemHeight);
	}

	private boolean isEndOfPageReached(@NonNull RecyclerView rv) {
		return rv.computeVerticalScrollRange() <= 2*rv.computeVerticalScrollExtent() + rv.computeVerticalScrollOffset();
	}
}
