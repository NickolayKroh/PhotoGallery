package com.bignerdranch.android.photogallery;

import androidx.lifecycle.Observer;
import android.animation.LayoutTransition;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.os.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import android.view.*;
import android.util.*;
import java.util.*;
import android.graphics.drawable.*;
import android.widget.ImageView;
import android.widget.ProgressBar;
import com.bignerdranch.android.photogallery.PhotoGalleryFragmentComponents.CacheComponentCallbacks2;
import com.bignerdranch.android.photogallery.PhotoGalleryFragmentComponents.ScrollListener;

import static android.content.Context.CONNECTIVITY_SERVICE;

public class PhotoGalleryFragment extends VisibleFragment
		implements
			PhotoGalleryActivity.OnBackPressedListener {
	private static final String TAG = "PhotoGallery";
	private static final int NETWORK_DIALOG = 0;

	private FragmentActivity mActivity;
	private PhotoAdapter mAdapter;
	private ThumbnailDownloader<Integer> mThumbnailDownloader;
	private ImageCache mImageCache;
	private ProgressBar mProgressBar;
	private SearchView mSearchView;
	private GalleryViewModel mViewModel;

	public void preCachingDown(int i) {
		mAdapter.preCachingUnder(i);
	}
	
	public void preCachingUp(int i) {
		mAdapter.preCachingUpper(i);
	}

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setHasOptionsMenu(true);

		if( getActivity() != null )
			mActivity = getActivity();

		mViewModel = ViewModelProviders.of(this).get(GalleryViewModel.class);

		if(mViewModel.getItems().getValue() == null)
			loadFirstPage();

		mImageCache = ImageCache.getInstance();

		Handler responseHandler = new Handler();
		mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
		mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloaderListener<Integer>() {
			@Override
			public void onThumbnailDownloaded(Integer i) {
				mAdapter.notifyItemChanged( i, PhotoAdapter.PAYLOAD_DRAWABLE );
			}
		} );
		mThumbnailDownloader.start();
		mThumbnailDownloader.getLooper();
//manually config change
		getActivity().registerComponentCallbacks(new CacheComponentCallbacks2());
	}
	
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.fragment_photo_gallery, container,
										false);

		mProgressBar = v.findViewById(R.id.progress_bar_gallery);

		RecyclerView mPhotoRecyclerView = v.findViewById(R.id.photo_recycler_view);
		mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), getSpanCount()));
		mAdapter = new PhotoAdapter();
		mPhotoRecyclerView.setAdapter(mAdapter);

		mPhotoRecyclerView.addOnScrollListener(new ScrollListener(mViewModel, this ));

		LayoutTransition lt = mPhotoRecyclerView.getLayoutTransition();
		if(lt != null) {
			lt.enableTransitionType(LayoutTransition.CHANGING);
			mPhotoRecyclerView.setLayoutTransition(lt);
		}
		else
			Log.i("anim","no LT");

		mViewModel.getItems().observe(this, new Observer<List<GalleryItem>>() {
			@Override
			public void onChanged(List<GalleryItem> items) {
				Log.i(TAG, "onChanged Fragment");
				if( isAdded() )
					mAdapter.setItems(items);
			}
		});

		return v;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mThumbnailDownloader.clearQueue();
		Log.i(TAG,"Thumbnail queue clear");
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mThumbnailDownloader.quit();
		Log.i( TAG, "Background thread Thumbnail destroyed");
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

//		MenuItem toggleItem = menu.findItem( R.id.menu_item_toggle_polling );
//		if( PollService.isServicesAlarmOn( getActivity() ) )
//			toggleItem.setTitle( R.string.stop_polling );
//		else
//			toggleItem.setTitle( R.string.start_polling );
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item ) {
		switch( item.getItemId() ) {
			case R.id.menu_item_clear:
				mThumbnailDownloader.clearQueue();
				QueryPreferences.setStoredQuery( getActivity(), null );
				mAdapter.clearAdapter();
				loadFirstPage();
				return true;
//			case R.id.menu_item_toggle_polling:
//				boolean shouldStartAlarm = !PollService.isServicesAlarmOn( getActivity() );
//				PollService.setServiceAlarm( getActivity(), shouldStartAlarm );
//				getActivity().invalidateOptionsMenu();
//				return true;
			default:{
				Log.i(TAG, "pressed -" + item.getTitle() );
				return super.onOptionsItemSelected(item);
			}
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
		Log.i(TAG,"onActResult");
		if(requestCode == NETWORK_DIALOG)
			loadPage();
		else
			super.onActivityResult(requestCode, resultCode, data);
	}

	private void loadFirstPage() {
		Log.i(TAG,"loadFirstPage");
		mViewModel.setPage(1);
		loadPage();
	}

	private void loadPage() {
		if(isNetworkAvailableAndConnected())
			mViewModel.loadNewPage();
		else
			showDialogNoNetworkAvailable();
	}

	public int getSpanCount() {
		Resources r = mActivity.getResources();
		float itemHeightPx = r.getDimension(R.dimen.item_height_dp);
		float screenWidthPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				r.getConfiguration().screenWidthDp,
				r.getDisplayMetrics());
		return Math.round(screenWidthPx / itemHeightPx);
	}

	private void showDialogNoNetworkAvailable() {
		AlertDialog dialog = new AlertDialog.Builder(mActivity)
			.setTitle("Network is not available")
			.setMessage("Please check your connection")
			.setCancelable(true)
			.setPositiveButton("Open settings", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int i) {
					Intent intent = new Intent("android.settings.SETTINGS");
					startActivityForResult(intent, NETWORK_DIALOG);
				}
			})
			.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int i) {
					mActivity.finish();
				}
			})
			.create();
		dialog.show();
	}

	private boolean isNetworkAvailableAndConnected() {
		ConnectivityManager cm = (ConnectivityManager) mActivity
				.getSystemService( CONNECTIVITY_SERVICE );
		if(cm == null)
			return false;
		boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
		return isNetworkAvailable && cm.getActiveNetworkInfo().isConnected();
	}

	private class PhotoHolder extends RecyclerView.ViewHolder
							implements View.OnClickListener {
		private ImageView mItemImageView;
		private GalleryItem mGalleryItem;
		
		private PhotoHolder(View itemView) {
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
		public void onClick(View v) {
			Intent i = PhotoPageActivity.newIntent( getActivity(), mGalleryItem.getPhotoPageUri() );
			startActivity(i);
		}
	}
	
	private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
		public static final String PAYLOAD_DRAWABLE = "PAYLOAD_DRAWABLE";
		private List<GalleryItem> mItems;
		private int mItemsOnScreen;
		private int mPrevSize = 0;
		
		private PhotoAdapter() {
			mItems = new ArrayList<>();
			mItemsOnScreen = getItemsCountOnAPage();
		}

		private void savePrevSize() {
			mPrevSize = mItems.size();
		}

		private void setItems(@NonNull List<GalleryItem> galleryItems) {
			mItems = galleryItems;
			notifyItemRangeInserted(mPrevSize, galleryItems.size() - mPrevSize);
			preCachingUnder(mPrevSize - getItemsCountOnAPage() - 1 );
			savePrevSize();
			mProgressBar.setVisibility(View.INVISIBLE);
		}
		
		private void clearAdapter() {
			mItems.clear();
			savePrevSize();
			mAdapter.notifyDataSetChanged();
			mProgressBar.setVisibility(View.VISIBLE);
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
			photoHolder.bindDrawable( getDrawable(galleryItem.getUrl()) );
			//preCachingUnder(position);
		}

		@Override
		public void onBindViewHolder(@NonNull PhotoHolder photoHolder, int position,
									 @NonNull List<Object> payloads) {
			if( !payloads.isEmpty() ) {
				GalleryItem galleryItem = mItems.get(position);
				photoHolder.bindDrawable( getDrawable(galleryItem.getUrl()) );
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

		private void preCachingUpper(int firstVisible) {
			int i = firstVisible - 2*mItemsOnScreen;
			if(i < 0)
				i = 0;
			for(; i < firstVisible; ++i)
				cache(i);
		}
		
		private void preCachingUnder(int firstVisible) {
			int end = firstVisible + 3*mItemsOnScreen;
			if( end > mItems.size() )
				end = mItems.size();
			for(int i = firstVisible + mItemsOnScreen + 1; i < end; ++i)
				cache(i);
		}

		private void cache(int itemPos) {
			String url = mItems.get(itemPos).getUrl();
			if( mImageCache.get(url) == null )
				mThumbnailDownloader.queueThumbnail(itemPos, url);
		}

		private int getItemsCountOnAPage() {
			Resources r = mActivity.getResources();
			float itemHeightPx = r.getDimension(R.dimen.item_height_dp);
			float screenHeightPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
					r.getConfiguration().screenHeightDp,
					r.getDisplayMetrics());
			return getSpanCount()*Math.round(screenHeightPx / itemHeightPx);
		}
	}

	private class SearchViewListener implements SearchView.OnQueryTextListener {
		@Override
		public boolean onQueryTextSubmit(String s) {
			mThumbnailDownloader.clearQueue();
			QueryPreferences.setStoredQuery(getActivity(), s);
			mAdapter.clearAdapter();
			loadFirstPage();
			mSearchView.setQuery("", false);
			mSearchView.setIconified(true);
			mSearchView.clearFocus();
			return true;
		}

		@Override
		public boolean onQueryTextChange(String s) {
			return false;
		}
	}
}
