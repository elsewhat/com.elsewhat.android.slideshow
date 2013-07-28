package com.elsewhat.android.slideshow.activities;

import java.io.IOException;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;

import com.google.cast.ApplicationChannel;
import com.google.cast.ApplicationMetadata;
import com.google.cast.ApplicationSession;
import com.google.cast.CastContext;
import com.google.cast.CastDevice;
import com.google.cast.MediaProtocolMessageStream;
import com.google.cast.MediaRouteAdapter;
import com.google.cast.MediaRouteHelper;
import com.google.cast.MediaRouteStateChangeListener;
import com.google.cast.MimeData;
import com.google.cast.SessionError;

public class ChromecastAddin implements MediaRouteAdapter {
    private static final String TAG = "ChromecastAddin";
	private MediaRouteButton mMediaRouteButton;
    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private MediaRouter.Callback mMediaRouterCallback;
    private CastDevice mSelectedDevice;
    private MediaRouteStateChangeListener mRouteStateListener;
	private CastContext mCastContext;
	private ApplicationSession mSession;
    
    
	public void onCreate(Activity activity, int media_route_button_id){
        mMediaRouteButton = (MediaRouteButton) activity.findViewById(media_route_button_id);

        mCastContext = new CastContext(activity.getApplicationContext());
        MediaRouteHelper.registerMinimalMediaRouteProvider(mCastContext, this);
        mMediaRouter = MediaRouter.getInstance(activity.getApplicationContext());
        mMediaRouteSelector = MediaRouteHelper.buildMediaRouteSelector(
                MediaRouteHelper.CATEGORY_CAST);
        mMediaRouteButton.setRouteSelector(mMediaRouteSelector);
        mMediaRouterCallback = new MyMediaRouterCallback();		
		
	}

    private class MyMediaRouterCallback extends MediaRouter.Callback {
        @Override
        public void onRouteSelected(MediaRouter router, RouteInfo route) {
            MediaRouteHelper.requestCastDeviceForRoute(route);
        }

        @Override
        public void onRouteUnselected(MediaRouter router, RouteInfo route) {
            mSelectedDevice = null;
            mRouteStateListener = null;
        }
    }	

	@Override
	public void onDeviceAvailable(CastDevice device, String arg1,
			MediaRouteStateChangeListener listener) {
		mSelectedDevice = device;
        mRouteStateListener = listener;
        
        openSession();
	}


	public boolean isRouteAvailable(){
		return mMediaRouter.isRouteAvailable(mMediaRouteSelector, 0);
	}
	

	@Override
	public void onSetVolume(double arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onUpdateVolume(double arg0) {
		// TODO Auto-generated method stub
		
	}
	
    /**
     * Starts a new video playback session with the current CastContext and selected device.
     * https://github.com/googlecast/cast-android-sample/blob/master/src/com/example/castsample/CastSampleActivity.java
     */
    private void openSession() {
        mSession = new ApplicationSession(mCastContext, mSelectedDevice);

        // TODO: The below lines allow you to specify either that your application uses the default
        // implementations of the Notification and Lock Screens, or that you will be using your own.
        int flags = 0;

        // Comment out the below line if you are not writing your own Notification Screen.
        // flags |= ApplicationSession.FLAG_DISABLE_NOTIFICATION;

        // Comment out the below line if you are not writing your own Lock Screen.
        // flags |= ApplicationSession.FLAG_DISABLE_LOCK_SCREEN_REMOTE_CONTROL;
        mSession.setApplicationOptions(flags);

        Log.v(TAG, "Beginning session with context: " + mCastContext);
        Log.v(TAG, "The session to begin: " + mSession);
        mSession.setListener(new com.google.cast.ApplicationSession.Listener() {

            private MediaProtocolMessageStream mMessageStream;


			@Override
            public void onSessionStarted(ApplicationMetadata appMetadata) {
            	Log.v(TAG, "Getting channel after session start");
                ApplicationChannel channel = mSession.getChannel();
                if (channel == null) {
                    Log.e(TAG, "channel = null");
                    return;
                }
                Log.v(TAG, "Creating and attaching Message Stream");
                mMessageStream = new MediaProtocolMessageStream();
                channel.attachMessageStream(mMessageStream);

                if (mMessageStream.getPlayerState() == null) {

                }
            }


            @Override
            public void onSessionEnded(SessionError error) {
                Log.i(TAG, "onSessionEnded " + error);
            }


			@Override
			public void onSessionStartFailed(SessionError error) {
				Log.i(TAG, "onSessionStartFailed " + error);
			}
        });

        try {
        	//Log.v((TAG, "Starting session with app name " + getString(R.string.app_name));
            String appID="DUMMY";
            // TODO: To run your own copy of the receiver, you will need to set app_name in 
            // /res/strings.xml to your own appID, and then upload the provided receiver 
            // to the url that you whitelisted for your app.
            // The current value of app_name is "YOUR_APP_ID_HERE".
            mSession.startSession(appID);
        } catch (IOException e) {
            Log.e(TAG, "Failed to open session", e);
        }
    }
	
}
