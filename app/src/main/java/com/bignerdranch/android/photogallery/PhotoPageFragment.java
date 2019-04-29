package com.bignerdranch.android.photogallery;
import android.content.Intent;
import android.net.*;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.webkit.*;
import android.os.*;
import android.view.*;
import android.widget.*;

public class PhotoPageFragment extends VisibleFragment implements PhotoPageActivity.OnBackPressedListener {
	private static final String ARG_URI = "photo_page_url";

	private Uri mUri;
	private WebView mWebView;
	private ProgressBar mProgressBar;

	public static PhotoPageFragment newInstance( Uri uri ) {
		Bundle args = new Bundle();
		args.putParcelable( ARG_URI, uri );
		PhotoPageFragment fragment = new PhotoPageFragment();
		fragment.setArguments(args);
		return fragment;
	}
	
	@Override
	public void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		if( getArguments() != null )
			mUri = getArguments().getParcelable(ARG_URI);
		setHasOptionsMenu(true);
	}
	
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
		final View v = inflater.inflate( R.layout.fragment_photo_page, container, false );
		
		mProgressBar = v.findViewById( R.id.progress_bar );
		mProgressBar.setMax(100);
		
		mWebView = v.findViewById( R.id.web_view );
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setWebChromeClient( new WebChromeClient() {
			public void onProgressChanged( WebView webView, int newProgress ) {
				if( newProgress == 100 )
					mProgressBar.setVisibility( View.GONE );
				else {
					mProgressBar.setVisibility( View.VISIBLE );
					mProgressBar.setProgress(newProgress);
				}
			}
			
			public void onReceivedTitle( WebView webView, String title ) {
				AppCompatActivity activity = (AppCompatActivity) getActivity();
				if( activity != null && activity.getSupportActionBar() != null )
					activity.getSupportActionBar().setSubtitle(title);
			}
			
			void onJsAlert() {
				
			}
		} );
		mWebView.setWebViewClient( new WebViewClient(){
			@Override
			public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
				super.onReceivedError(view, request, error);
				Log.i("WebView", "Got an error - " + error.toString());
			}

			@RequiresApi(Build.VERSION_CODES.N)
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
				Uri uri = request.getUrl();
				return load(view, uri);
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				Uri uri = Uri.parse(url);
				return load(view, uri);
			}

			private boolean load(WebView view, Uri uri){
				String scheme = uri.getScheme();

				Log.i("WebView", "catch a url - " + uri);

				if( scheme.equals("https") || scheme.equals("http") )
					return false;
				else {
					Intent intent = new Intent(Intent.ACTION_VIEW, uri);
					view.getContext().startActivity(intent);
					return true;
				}
			}
		} );

		mWebView.loadUrl( mUri.toString() );
		return v;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.menu_photo_page, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch( item.getItemId() ) {
			case R.id.menu_item_reload:
				mWebView.reload();
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onBackPressed() {
		if( mWebView.canGoBack() )
			mWebView.goBack();
		else
			if( getActivity() != null )
				getActivity().finish();
	}
}
