package com.bignerdranch.android.photogallery;
import android.app.ActionBar;
import android.content.*;
import android.net.*;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;

public class PhotoPageActivity extends SingleFragmentActivity {
	public interface OnBackPressedListener{
		void onBackPressed();
	}

	public static Intent newIntent( Context context, Uri photoPageUri ) {
		Intent i = new Intent( context, PhotoPageActivity.class );
		i.setData(photoPageUri);
		return i;
	}

	@Override
	protected Fragment createFragment() {
		return PhotoPageFragment.newInstance( getIntent().getData() );
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onSupportNavigateUp() {
		finish();
		return true;
	}

	@Override
	public void onBackPressed() {
		FragmentManager fm = getSupportFragmentManager();
		PhotoPageActivity.OnBackPressedListener backPressedListener = null;
		for( Fragment fragment : fm.getFragments() )
			if( fragment instanceof PhotoPageActivity.OnBackPressedListener) {
				backPressedListener = (PhotoPageActivity.OnBackPressedListener) fragment;
				break;
			}

		if( backPressedListener == null )
			super.onBackPressed();
		else
			backPressedListener.onBackPressed();
	}
}
