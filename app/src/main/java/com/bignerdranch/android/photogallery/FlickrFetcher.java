package com.bignerdranch.android.photogallery;

import android.net.Uri;
import android.util.Log;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class FlickrFetcher {
	private static final String TAG = "FlickrFetcher";
	private static final String API_KEY = "bdb9a79231df65a7ad8ccb6c2ba97111";
	private static final String FETCH_RECENT_METHOD = "flickr.photos.getRecent";
	private static final String SEARCH_METHOD = "flickr.photos.search";
	private static final Uri ENDPOINT = Uri
		.parse("https://api.flickr.com/services/rest/")
		.buildUpon()
		.appendQueryParameter( "api_key", API_KEY )
		.appendQueryParameter( "format", "json" )
		.appendQueryParameter( "nojsoncallback", "1" )
		.appendQueryParameter( "extras", "url_s" )
		.build();
	
	
	public byte[] getUrlBytes( String urlSpec ) throws IOException {
		URL url = new URL(urlSpec);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			InputStream in = connection.getInputStream();

			if( connection.getResponseCode() != HttpURLConnection.HTTP_OK )
				throw new IOException( connection.getResponseMessage() + ": with " + urlSpec );

			int bytesRead;
			byte[] buffer = new byte[1024];

			while( ( bytesRead = in.read(buffer) ) > 0 ) {
				out.write( buffer, 0, bytesRead );
			}

			out.close();
			return out.toByteArray();
		} finally {
			connection.disconnect();
		}
	}
	
	private String getUrlString( String urlSpec ) throws IOException {
		return new String( getUrlBytes( urlSpec ) );
	}
	
	public List<GalleryItem> fetchRecentPhotos(int page) {
		String url = buildUrl( FETCH_RECENT_METHOD, null, page );
		return downloadGalleryItems(url);
	}
	
	public List<GalleryItem> searchPhotos( String query, int page ) {
		String url = buildUrl( SEARCH_METHOD, query, page );
		return downloadGalleryItems(url);
	}
	
	private List<GalleryItem> downloadGalleryItems( String url ) {
		List<GalleryItem> items = new ArrayList<>();
		
		try {
			String jsonString = getUrlString(url);
			Log.i( TAG, "Received JSON: " + jsonString );
			parseItems( items, jsonString );
		} catch ( IOException ioe ){
			Log.e( TAG, "Failed to fetch items", ioe );
		}

		return items;
	}
	
	private String buildUrl( String method, String query, int page ) {
		Uri.Builder uriBuilder = ENDPOINT.buildUpon()
				.appendQueryParameter( "method", method )
				.appendQueryParameter( "page", String.valueOf(page) );
		
		if( method.equals( SEARCH_METHOD ) )
			uriBuilder.appendQueryParameter( "text", query );
			
		return uriBuilder.build().toString();
	}

	private void parseItems( List<GalleryItem> items, String jsonString  ) throws IOException {
		JsonReader in = new JsonReader( new StringReader(jsonString) );

		in.beginObject();
		in.nextName();
		in.beginObject();
		while( !in.nextName().equals( "photo" ) )
			in.skipValue();

		in.beginArray();
		while( in.hasNext() && in.peek().equals(JsonToken.BEGIN_OBJECT) ){
			GalleryItem newItem = new GalleryItem();

			in.beginObject();
			while( in.hasNext() ){
				switch( in.nextName() ){
					case "id":
						newItem.setId( in.nextString() );
						break;
					case "owner":
						newItem.setOwner( in.nextString() );
						break;
					case "title":
						newItem.setCaption( in.nextString() );
						break;
					case "url_s":
						newItem.setUrl( in.nextString() );
						items.add(newItem);
						break;
					default:
						in.skipValue();
						break;
				}
			}
			in.endObject();
		}
		in.close();
	}
}

