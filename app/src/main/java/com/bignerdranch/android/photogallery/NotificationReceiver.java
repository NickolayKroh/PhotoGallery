package com.bignerdranch.android.photogallery;

import android.app.Activity;
import android.app.Notification;
import android.content.*;
import android.support.v4.app.NotificationManagerCompat;

public class NotificationReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive( Context c, Intent i )
	{
		if( getResultCode() != Activity.RESULT_OK )
			return;
		
			int requestCode = i.getIntExtra( PollService.REQUEST_CODE, 0 );
			Notification notification = (Notification) i.getParcelableExtra( PollService.NOTIFICATION );
			
			NotificationManagerCompat notificationManager = NotificationManagerCompat.from(c);
			notificationManager.notify( requestCode, notification );
	}
}
