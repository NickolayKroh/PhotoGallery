package com.bignerdranch.android.photogallery;
import android.content.*;

public class StartupReceiver extends BroadcastReceiver
{
	private static final String TAG = "StartupReceiver";

	@Override
	public void onReceive( Context context, Intent intent ) {
		boolean isOn = QueryPreferences.isAlarmOn(context);
		PollService.setServiceAlarm( context, isOn );
	}
}
