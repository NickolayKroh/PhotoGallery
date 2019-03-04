package com.bignerdranch.android.photogallery;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.os.*;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.*;
import android.util.*;
import java.util.*;
import android.graphics.drawable.*;
import android.widget.ImageView;
import android.widget.ProgressBar;
import com.bignerdranch.android.photogallery.PhotoGalleryFragmentComponents.CacheComponentCallbacks2;
import com.bignerdranch.android.photogallery.PhotoGalleryFragmentComponents.ItemsLoader;
import com.bignerdranch.android.photogallery.PhotoGalleryFragmentComponents.ScrollListener;

import static android.content.Context.CONNECTIVITY_SERVICE;

public class PhotoGalleryFragment extends VisibleFragment
		implements
			PhotoGalleryActivity.OnBackPressedListener {
	private static final String TAG = "PhotoGallery";
	private static final int LOADER_ID = 1;

	private RecyclerView mPhotoRecyclerView;
	private PhotoAdapter mAdapter;
	private List<GalleryItem> mItems = new ArrayList<>();
	private ThumbnailDownloader<Integer> mThumbnailDownloader;
	private ImageCache mImageCache;
	private ProgressBar mProgressBar;
	private SearchView mSearchView;
	private boolean isNextPagePreparing = false;
	private int mPage;

	private ItemsLoaderManager mItemsLoaderManager;

	@Override
	public void onCreate( Bundle bundle ) {
		super.onCreate(bundle);
		setRetainInstance(true);
		setHasOptionsMenu(true);

		mItemsLoaderManager = new ItemsLoaderManager();
		loadFirstPage();

		mImageCache = ImageCache.getInstance();

		Handler responseHandler = new Handler();
		mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
		mThumbnailDownloader.setThumbnailDownloadListener(
				new ThumbnailDownloader.ThumbnailDownloaderListener<Integer>() {
			@Override
			public void onThumbnailDownloaded(Integer i) {
				mAdapter.notifyItemChanged( i, PhotoAdapter.PAYLOAD_DRAWABLE );
			}
		} );
		mThumbnailDownloader.start();
		mThumbnailDownloader.getLooper();
		Log.i( TAG, "Background thread started" );

		getActivity().registerComponentCallbacks(new CacheComponentCallbacks2());
	}
	
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
							 							Bundle savedInstanceState ) {
		final View v = inflater.inflate( R.layout.fragment_photo_gallery, container,
																	false );

		mProgressBar = v.findViewById(R.id.progress_bar_gallery);

		mPhotoRecyclerView = v.findViewById( R.id.photo_recycler_view );
		mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), getSpanCount()));
		mPhotoRecyclerView.addOnScrollListener(new ScrollListener(this) );

		return v;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mThumbnailDownloader.clearQueue();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mThumbnailDownloader.quit();
		Log.i( TAG, "Background thread destroyed");
	}
	
	@Override
	public void onCreateOptionsMenu( Menu menu, MenuInflater menuInflater ) {
		super.onCreateOptionsMenu(menu, menuInflater);
		menuInflater.inflate(R.menu.fragment_photo_gallery, menu);

		final MenuItem searchItem = menu.findItem(R.id.menu_item_search);
		mSearchView = (SearchView) searchItem.getActionView();
		mSearchView.setOnQueryTextListener(new SearchViewListener());
		
		mSearchView.setOnSearchClickListener( new View.OnClickListener() {
			@Override
			public void onClick( View v ) {
				String query = QueryPreferences.getStoredQuery( getActivity() );
				mSearchView.setQuery( query, false );
			}
		} );

		MenuItem toggleItem = menu.findItem( R.id.menu_item_toggle_polling );
		if( PollService.isServicesAlarmOn( getActivity() ) )
			toggleItem.setTitle( R.string.stop_polling );
		else
			toggleItem.setTitle( R.string.start_polling );
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item ) {
		switch( item.getItemId() ) {
			case R.id.menu_item_clear:
				mThumbnailDownloader.clearQueue();
				QueryPreferences.setStoredQuery( getActivity(), null );
				clearAdapter();
				loadFirstPage();
				return true;
			case R.id.menu_item_toggle_polling:
				boolean shouldStartAlarm = !PollService.isServicesAlarmOn( getActivity() );
				PollService.setServiceAlarm( getActivity(), shouldStartAlarm );
				getActivity().invalidateOptionsMenu();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onBackPressed() {
		if( mSearchView.isIconified() ) {
			FragmentActivity activity = getActivity();
			if( activity != null )
					activity.finish();
		} else {
			mSearchView.setQuery("", false);
			mSearchView.setIconified(true);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == 0)
			loadPage();
		else
			super.onActivityResult(requestCode, resultCode, data);
	}

	private void loadFirstPage(){
		mPage = 1;
		loadPage();
	}

	private void clearAdapter() {
		mItems.clear();
		if(mAdapter != null)
			mAdapter.notifyDataSetChanged();
		mProgressBar.setVisibility(View.VISIBLE);
	}

	public void onReachedEndOfPage(){
		if( !isNextPagePreparing ) {
			isNextPagePreparing = true;
			++mPage;
			loadPage();
		}
	}

	private void loadPage() {
		if( !isNetworkAvailableAndConnected() )
			showDialogNoNetworkAvailable();
		else
			startItemsLoader();
	}

	private void startItemsLoader() {
		Bundle bundle = new Bundle();
		String query = QueryPreferences.getStoredQuery(getActivity());
		bundle.putString(ItemsLoader.ARGS_QUERY, query);
		bundle.putInt(ItemsLoader.ARGS_PAGE, mPage);
		Loader<List<GalleryItem>> loader = getLoaderManager()
				.restartLoader(LOADER_ID, bundle, mItemsLoaderManager);
		loader.forceLoad();
	}

	private int getSpanCount() {
		Resources r = getActivity().getResources();
		float itemHeightPx = r.getDimension(R.dimen.item_height_dp);
		float screenWidthPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				r.getConfiguration().screenWidthDp,
				r.getDisplayMetrics());
		return Math.round(screenWidthPx / itemHeightPx);
	}

	private int countItemsOnPage(){
		Resources r = getActivity().getResources();
		float itemHeightPx = r.getDimension(R.dimen.item_height_dp);
		float screenHeightPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				r.getConfiguration().screenHeightDp,
				r.getDisplayMetrics());
		return 3*Math.round(screenHeightPx / itemHeightPx);
	}

	private void showDialogNoNetworkAvailable() {
		AlertDialog dialog = new AlertDialog.Builder(getActivity())
			.setTitle("Network is not available")
			.setMessage("Please check your connection")
			.setCancelable(true)
			.setPositiveButton("Open settings", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int i) {
					Intent intent = new Intent("android.settings.SETTINGS");
					startActivityForResult(intent, 0);
				}
			})
			.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int i) {
					getActivity().finish();
				}
			})
			.create();
		dialog.show();
	}

	private boolean isNetworkAvailableAndConnected() {
		ConnectivityManager cm = (ConnectivityManager) getActivity()
				.getSystemService( CONNECTIVITY_SERVICE );
		boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
		return isNetworkAvailable && cm.getActiveNetworkInfo().isConnected();
	}

	private void setupAdapter( List<GalleryItem> list ) {
		if( isAdded() ) {
			if( isNextPagePreparing && mAdapter != null ) {
				mAdapter.addGalleryItems(list);
				isNextPagePreparing = false;
			} else {
				mAdapter = new PhotoAdapter(list);
				mPhotoRecyclerView.setAdapter(mAdapter);
			}

			mProgressBar.setVisibility(View.INVISIBLE);
		}
	}

	private class PhotoHolder extends RecyclerView.ViewHolder
							implements View.OnClickListener {
		private ImageView mItemImageView;
		private GalleryItem mGalleryItem;
		
		private PhotoHolder( View itemView ) {
			super(itemView);
			
			mItemImageView = (ImageView) itemView;
			itemView.setOnClickListener(this);
		}

		public void bindDrawable( Drawable drawable ) {
			mItemImageView.setImageDrawable(drawable);
		}
		
		public void bindGalleryItem( GalleryItem galleryItem ) {
			mGalleryItem = galleryItem;
		}
		
		@Override
		public void onClick( View v ) {
			Intent i = PhotoPageActivity.newIntent( getActivity(), mGalleryItem.getPhotoPageUri() );
			startActivity(i);
		}
	}
	
	private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
		public static final String PAYLOAD_DRAWABLE = "PAYLOAD_DRAWABLE";
		
		private PhotoAdapter( List<GalleryItem> galleryItems ){
			mItems = galleryItems;
		}

		private void addGalleryItems(List<GalleryItem> galleryItems){
			int positionStart = mItems.size();
			mItems.addAll(galleryItems);
			this.notifyItemRangeInserted( positionStart, galleryItems.size() );
		}
		
		@Override
		@NonNull
		public PhotoHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType ) {
			LayoutInflater inflater = LayoutInflater.from( getActivity() );
			View view = inflater.inflate( R.layout.gallery_item, viewGroup, false );
			return new PhotoHolder(view);
		}
		
		@Override
		public void onBindViewHolder(@NonNull PhotoHolder photoHolder, int position ) {
			GalleryItem galleryItem = mItems.get(position);
			photoHolder.bindGalleryItem(galleryItem);
			photoHolder.bindDrawable( getDrawable( galleryItem.getUrl() ) );
			preCaching(position);
		}

		@Override
		public void onBindViewHolder(@NonNull PhotoHolder photoHolder, int position,
									 @NonNull List<Object> payloads) {
			if( !payloads.isEmpty() ) {
				GalleryItem galleryItem = mItems.get(position);
				photoHolder.bindDrawable( getDrawable( galleryItem.getUrl() ) );
			} else
				super.onBindViewHolder(photoHolder, position, payloads);
		}

		@Override
		public int getItemCount() {
			return mItems.size();
		}

		private Drawable getDrawable(String url) {
			Drawable drawable;
			Bitmap bitmap = mImageCache.get(url);

			if( bitmap == null )
				drawable = getResources().getDrawable( R.drawable.kenny );
			else
				drawable = new BitmapDrawable( getResources(), bitmap );

			return drawable;
		}

		private void preCaching(int current){
			int start = current - countItemsOnPage();
			if(	start < 0 )
				start = 0;

			int end = current + countItemsOnPage();
			if( end > mItems.size() )
				end = mItems.size();

			for( int i = start; i < end; ++i ) {
				String url = mItems.get(i).getUrl();
				if( mImageCache.get(url) == null )
					mThumbnailDownloader.queueThumbnail( i, url );
			}
		}
	}

	private class SearchViewListener implements SearchView.OnQueryTextListener {
		@Override
		public boolean onQueryTextSubmit(String s) {
			mThumbnailDownloader.clearQueue();
			QueryPreferences.setStoredQuery(getActivity(), s);
			loadFirstPage();
			mSearchView.setQuery( "",false );
			mSearchView.setIconified(true);
			mSearchView.clearFocus();
			clearAdapter();
			return true;
		}

		@Override
		public boolean onQueryTextChange(String s) {
			return false;
		}
	}

	private class ItemsLoaderManager
			implements LoaderManager.LoaderCallbacks<List<GalleryItem>> {
		@NonNull
		@Override
		public Loader<List<GalleryItem>> onCreateLoader(int id, Bundle bundle) {
			return new ItemsLoader(getActivity(), bundle);
		}

		@Override
		public void onLoadFinished(@NonNull Loader<List<GalleryItem>> loader, List<GalleryItem> items) {
			if( items.size() > 0 )
				setupAdapter(items);
			else {
				--mPage;
				isNextPagePreparing = false;
			}
		}

		@Override
		public void onLoaderReset(@NonNull Loader<List<GalleryItem>> loader) {}
	}
}
