package com.bignerdranch.android.photogallery;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.Explode;
import android.view.Window;

public class SplashActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
			getWindow().setExitTransition(new Explode());
		}
		Intent intent = new Intent( this, PhotoGalleryActivity.class );
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle() );
		overridePendingTransition(R.anim.rotate, R.anim.slide_in_right);
		finish();
	}
}
