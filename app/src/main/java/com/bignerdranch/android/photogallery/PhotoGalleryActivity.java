package com.bignerdranch.android.photogallery;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

public class PhotoGalleryActivity extends SingleFragmentActivity {
	public interface OnBackPressedListener{
		void onBackPressed();
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		setTheme(R.style.AppTheme);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		setTheme(R.style.AppTheme);
		super.onResume();
	}


	@Override
    protected Fragment createFragment() {
		return new PhotoGalleryFragment();
    }

    public static Intent newIntent( Context context ) {
		return new Intent( context, PhotoGalleryActivity.class );
	}

	@Override
	public void onBackPressed() {
		FragmentManager fm = getSupportFragmentManager();
		OnBackPressedListener backPressedListener = null;
		for( Fragment fragment : fm.getFragments() )
			if( fragment instanceof OnBackPressedListener ) {
				backPressedListener = (OnBackPressedListener) fragment;
				break;
			}

		if( backPressedListener == null )
			super.onBackPressed();
		else
			backPressedListener.onBackPressed();
	}
}
