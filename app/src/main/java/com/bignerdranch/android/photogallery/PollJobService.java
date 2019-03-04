package com.bignerdranch.android.photogallery;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.bignerdranch.android.photogallery.PollService.ACTION_SHOW_NOTIFICATION;
import static com.bignerdranch.android.photogallery.PollService.NOTIFICATION;
import static com.bignerdranch.android.photogallery.PollService.PERM_PRIVATE;
import static com.bignerdranch.android.photogallery.PollService.REQUEST_CODE;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class PollJobService extends JobService {
	private static final int JOB_ID = 1;

	private PollTask mCurrentTask;

	@Override
	public boolean onStartJob(JobParameters jobParameters) {
		mCurrentTask = new PollTask(jobParameters);
		mCurrentTask.execute();
		return true;
	}

	@Override
	public boolean onStopJob(JobParameters jobParameters) {
		if(mCurrentTask != null)
			mCurrentTask.cancel(true);
		return true;
	}

	public static void scheduleJob(Context context){
		JobScheduler scheduler = (JobScheduler) context
				.getSystemService(Context.JOB_SCHEDULER_SERVICE);

		if( scheduler == null )
			return;

		if( isJobScheduled(context) ) {
			scheduler.cancel(JOB_ID);
			return;
		}

		JobInfo jobInfo = new JobInfo
				.Builder(JOB_ID, new ComponentName(context, PollJobService.class))
				.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
				.setPeriodic( TimeUnit.MINUTES.toMillis(15) )
				.setPersisted(true)
				.build();

		scheduler.schedule(jobInfo);
	}

	public static boolean isJobScheduled(Context context) {
		JobScheduler scheduler = (JobScheduler) context
				.getSystemService(Context.JOB_SCHEDULER_SERVICE);

		if( scheduler == null )
			return false;

		boolean hasBeenScheduled = false;
		for(JobInfo jobInfo : scheduler.getAllPendingJobs())
			if(jobInfo.getId() == JOB_ID)
				hasBeenScheduled = true;

		return hasBeenScheduled;
	}

	private void showBackgroundNotification(Notification notification) {
		Intent i = new Intent( ACTION_SHOW_NOTIFICATION );
		i.putExtra( REQUEST_CODE, 0 );
		i.putExtra( NOTIFICATION, notification );
		sendOrderedBroadcast( i, PERM_PRIVATE, null, null, Activity.RESULT_OK, null, null );
	}

	private class PollTask extends AsyncTask<Void, Void, Void> {
		private JobParameters params;

		PollTask(JobParameters parameters){
			params = parameters;
		}

		@Override
		protected Void doInBackground(Void... params) {
			Context context = getApplicationContext();

			String query = QueryPreferences.getStoredQuery(context);
			String lastResultId = QueryPreferences.getLastResultId(context);
			List<GalleryItem> item;

			if( query == null )
				item = new FlickrFetcher().fetchRecentPhotos(1);
			else
				item = new FlickrFetcher().searchPhotos(query, 1);

			if( item.size() == 0 )
				return null;

			String resultId = item.get(0).getId();

			if( !resultId.equals(lastResultId) ) {
				Resources resources = getResources();
				Intent i = PhotoGalleryActivity.newIntent(context);
				PendingIntent pi = PendingIntent.getActivity( context, 0, i, 0 );

				Notification notification = new Notification.Builder( getApplicationContext() )
						.setTicker( resources.getString( R.string.new_pictures_title ) )
						.setSmallIcon( android.R.drawable.ic_menu_camera )
						.setContentTitle( resources.getString( R.string.new_pictures_title ) )
						.setContentText( resources.getString( R.string.new_pictures_text ) )
						.setContentIntent(pi)
						.setAutoCancel(true)
						.build();

				showBackgroundNotification(notification );
			}

			QueryPreferences.setLastResultId( context, resultId );

			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);
			jobFinished(params, false);
		}
	}
}
