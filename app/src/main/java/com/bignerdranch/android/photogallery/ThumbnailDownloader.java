package com.bignerdranch.android.photogallery;
import android.os.*;
import android.util.*;
import java.util.concurrent.*;
import java.io.*;
import android.graphics.*;

public class ThumbnailDownloader<T> extends HandlerThread {
	private static final String TAG = "ThumbnailDownloader";
	private static final int MESSAGE_DOWNLOAD = 0;
	
	private boolean mHasQuit = false;
	private Handler mRequestHandler;
	private ConcurrentHashMap<T,String> mRequestMap = new ConcurrentHashMap<>();
	private Handler mResponseHandler;
	private ThumbnailDownloaderListener<T> mThumbnailDownloaderListener;
	
	public interface ThumbnailDownloaderListener<T> {
		void onThumbnailDownloaded(T target);
	}
	
	public void setThumbnailDownloadListener( ThumbnailDownloaderListener<T> listener ) {
		mThumbnailDownloaderListener = listener;
	}
	
	ThumbnailDownloader( Handler responseHandler ) {
		super(TAG);
		mResponseHandler = responseHandler;
	}

	@Override
	protected void onLooperPrepared() {
		mRequestHandler = new Handler() {
			@Override
			public void handleMessage( Message msg ) {
				if( msg.what == MESSAGE_DOWNLOAD ) {
					T target = (T) msg.obj;
					Log.i( TAG, "Got a request for target: " + target.toString() );
					handleRequest(target);
				}
			}
		};
	}
	
	@Override
	public boolean quit() {
		mHasQuit = true;
		return super.quit();
	}
	
	public void queueThumbnail( T target, String url ) {
		if( mRequestMap.get(target) != null )
			return;

		if( url == null ) {
			mRequestMap.remove(target);
		} else {
			mRequestMap.put( target, url );
			Log.i( TAG, "Got target: " +  target.toString() + " with a url: " + url );
			mRequestHandler.obtainMessage( MESSAGE_DOWNLOAD, target ).sendToTarget();
		}
	}
	
	public void clearQueue() {
		mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
		mRequestMap.clear();
		Log.i(TAG,"-------------clearQueue-------" );
	}
	
	private void handleRequest( final T target ) {
		try {
			final String url = mRequestMap.get(target);
				
			byte[] bitmapBytes = new FlickrFetcher().getUrlBytes(url);
			final Bitmap bitmap = BitmapFactory.decodeByteArray( bitmapBytes, 0 , bitmapBytes.length );
			Log.i( TAG, "Bitmap created. t - " + target.toString() );
			
			mResponseHandler.post( new Runnable() {
				public void run() {
					if( !url.equals( mRequestMap.get(target) ) || mHasQuit )
						return;

					mRequestMap.remove(target);

					if( ImageCache.getInstance().get(url) != null )
						Log.i(TAG,target.toString() + " - ALREADY IN THE CACHE!!! url - " + url );

					ImageCache.getInstance().put( url, bitmap );
					mThumbnailDownloaderListener.onThumbnailDownloaded(target);
				}
			} );
			
		} catch( IOException ioe ) {
			Log.e( TAG, "Error downloading image", ioe );
		}
	}
	
}
