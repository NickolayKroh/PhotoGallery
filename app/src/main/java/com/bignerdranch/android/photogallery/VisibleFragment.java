package com.bignerdranch.android.photogallery;
import android.app.Activity;
import android.content.*;
import android.support.v4.app.Fragment;

public class VisibleFragment extends Fragment
{
	private static final String TAG = "VisibleFragment";
	
	@Override
	public void onStart()
	{
		super.onStart();
		IntentFilter filter = new IntentFilter( PollService.ACTION_SHOW_NOTIFICATION );
		getActivity().registerReceiver( mOnShowNotification, filter, PollService.PERM_PRIVATE, null );
	}
	
	@Override
	public void onStop()
	{
		super.onStop();
		getActivity().unregisterReceiver(mOnShowNotification);
	}
	
	private BroadcastReceiver mOnShowNotification = new BroadcastReceiver()
	{

		@Override
		public void onReceive( Context context, Intent intent )
		{
			setResultCode( Activity.RESULT_CANCELED );
		}
	};
}
