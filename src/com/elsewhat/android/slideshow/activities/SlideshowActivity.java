package com.elsewhat.android.slideshow.activities;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.elsewhat.android.slideshow.R;
import com.elsewhat.android.slideshow.api.Analytics;
import com.elsewhat.android.slideshow.api.AndroidUtils;
import com.elsewhat.android.slideshow.api.CustomGallery;
import com.elsewhat.android.slideshow.api.DownloadableObject;
import com.elsewhat.android.slideshow.api.FileDownloader;
import com.elsewhat.android.slideshow.api.FileDownloader.FileDownloaderListener;
import com.elsewhat.android.slideshow.api.SlideshowPhoto;
import com.elsewhat.android.slideshow.api.SlideshowPhotoCached;
import com.elsewhat.android.slideshow.api.SlideshowPhotoDrawable;
import com.elsewhat.android.slideshow.backend.FlickrPublicSetBackend;


public class SlideshowActivity extends Activity implements FileDownloaderListener, OnSharedPreferenceChangeListener{
		protected ImageAdapter imageAdapter;
		public static final String LOG_PREFIX = "ElsewhatSlideshow";
		protected File rootFileDirectory;
		protected CustomGallery gallery;
		protected SlideshowTimerTask slideshowTimerTask;
		protected boolean isSlideshowRunning=true;
		//for downloading the photos
		protected FileDownloader fileDownloader;
		
		//temp list in order to make sure the image adapter is not updated too often
		protected ArrayList<SlideshowPhoto> queuedSlideshowPhotos;
		
		boolean cachedPhotosDeleted=false;
		boolean userCreatedTouchEvent=false;
		
	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	    	//make full screen for pre-honeycomb devices
	    	if(AndroidUtils.isAndroid30()){
		    	//9 == Window.FEATURE_ACTION_BAR_OVERLAY. Done in order to avoid having to use reflection as value is not present in 2.2
	    		getWindow().requestFeature(9);	
	    	}else {//all pre-3.0 version
	    		requestWindowFeature(Window.FEATURE_NO_TITLE);
		    	getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
		    			WindowManager.LayoutParams.FLAG_FULLSCREEN);
	    	}

	    	Analytics.trackPageView(getApplicationContext(), "/start");

			String rootPath =  Environment.getExternalStorageDirectory()+ SlideshowPreferences.CACHE_DIRECTORY;
			rootFileDirectory= new File (rootPath);
			
			//register listener so we can handle if the cached photos are deleted
			SharedPreferences settings = getSharedPreferences(SlideshowPreferences.PREFS_NAME, MODE_WORLD_READABLE);
			settings.registerOnSharedPreferenceChangeListener(this);
			
			
	        super.onCreate(savedInstanceState);
	        //getActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar_background));
	        setContentView(R.layout.gallery_1);

	        // Reference the Gallery view
	        gallery = (CustomGallery) findViewById(R.id.gallery);
	        //lights out mode for the activity
	        //Reflection call similar to gallery.setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
	        //Needed since android 2.2 doesn't have method
	        onCreateReflectionCalls(gallery,this);
	        
	        ////transition time in millis
	        gallery.setAnimationDuration(5500);
	        //disable annoying click sound on next photo
	        gallery.setSoundEffectsEnabled(false);
	        //disable sleep
	        gallery.setKeepScreenOn (true);
	        boolean doCustomTransition= SlideshowPreferences.doCustomTransition(getApplicationContext());
			gallery.setDoCustomTransition(doCustomTransition);
	        
	        //Add some hardcoded photos that will be displayed untill we have download the others
	        ArrayList<SlideshowPhoto> cachedDrawables = new ArrayList<SlideshowPhoto>(10);
	        //FYI the url is only used during share photo
	        cachedDrawables.add(new SlideshowPhotoDrawable(this,"Father", "Graffiti art captured in Bergen, Norway",R.drawable.photo_father,"http://dl.dropbox.com/u/4379928/Slideshow/father.JPG"));
	        cachedDrawables.add(new SlideshowPhotoDrawable(this,"Handstand","The lightning was just perfect this day, so why not use it for something productively",R.drawable.photo_handstand,"http://dl.dropbox.com/u/4379928/Slideshow/handstand.jpg"));
	        cachedDrawables.add(new SlideshowPhotoDrawable(this,"Lexus", "A showcase photo of the Lexus IS series",R.drawable.photo_lexus,"http://dl.dropbox.com/u/4379928/Slideshow/lexus_is%2Cjpg.jpg"));
	        
	        //lets randomize the three hardcoded photos
			long seed = System.nanoTime();
			Collections.shuffle(cachedDrawables, new Random(seed));
	        
	        //cachedDrawables.add(new SlideshowPhotoDrawable(this,"test", "test",R.drawable.logo));

	        boolean doDisplayPhotoTitle= SlideshowPreferences.doDisplayPhotoTitle(getApplicationContext());
	        //create the adapter holding the slideshow photos
	        imageAdapter=new ImageAdapter(this,0,cachedDrawables,rootFileDirectory,doDisplayPhotoTitle);
	        gallery.setAdapter(imageAdapter);
	        
	        // Set a item click listener, and just Toast the clicked position
	        //gallery.setOnItemClickListener(new OnItemClickListener() {
	        //    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
	        //        Toast.makeText(SlideshowActivity.this, "Photo in position " + position + " clicked", Toast.LENGTH_SHORT).show();
	        //    }
	        //});
	        
	        // We also want to show context menu for longpressed items in the gallery
	        registerForContextMenu(gallery);
	        
	        //The SlideshowTimerTask task is started by onResume (which is called soon after onCreate
	        //slideshowTimerTask=  new SlideshowTimerTask();
	        //slideshowTimerTask.execute();
	        
	        //Canvas test = new Canvas();
	        //Log.i(LOG_PREFIX, " Canvas max " + test.getMaximumBitmapHeight()+","+test.getMaximumBitmapHeight());
	        
	        new PhotoUrlsTask().execute();
	    }
	    
	    @SuppressWarnings({ "unchecked", "rawtypes" })
		public void onCreateReflectionCalls(Gallery aGallery, Activity activity){
	    	try {
	    		//same as gallery.setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
	    		Class GalleryClass = aGallery.getClass();
	    		Method setSystemMethod = GalleryClass.getMethod("setSystemUiVisibility", int.class);
	    		setSystemMethod.invoke(aGallery, 1);
	    		
	    		Class ActivityClass = activity.getClass();
	    		Method getActionBar = ActivityClass.getMethod("getActionBar");
	    		Object objectActionBar = getActionBar.invoke(activity);
	    		
	    		Class ActionBarClass = objectActionBar.getClass();
	    		Method setDisplayShowTitleEnabled = ActionBarClass.getMethod("setDisplayShowTitleEnabled", boolean.class);
	    		setDisplayShowTitleEnabled.invoke(objectActionBar, false);
	    		
	    		
	    	}catch (NoSuchMethodException e) {
				//ignore
	    		// TODO: handle exception
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
	    	
	    	
	    }

	    /*
	    @Override
	    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
	        menu.add("Context menu");
	    }
	    */
	    
	    /*
	    @Override
	    public boolean onContextItemSelected(MenuItem item) {
	        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	        Toast.makeText(this, "Longpress: " + info.position, Toast.LENGTH_SHORT).show();
	        return true;
	    }*/
	    
		@Override
		public boolean onCreateOptionsMenu(Menu menu) {
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.menu, menu);
			return true;
		}

		@Override
		public boolean onMenuItemSelected(int featureId, MenuItem item) {
			switch (item.getItemId()) {
			case R.id.menuPreferences:
				Analytics.trackPageView(getApplicationContext(), "/preferences");
				Intent iPreferences = new Intent(this, SlideshowPreferences.class);
				startActivity(iPreferences);
				return true;
			case R.id.menuShare:
				SlideshowPhoto currentPhoto = imageAdapter.getItem(gallery.getSelectedItemPosition());
				Analytics.trackPageView(getApplicationContext(), "/share");
				Analytics.trackEvent(getApplicationContext(), "actions", "share", currentPhoto.getTitle());
				actionSharePhoto(currentPhoto);
				
				return true;
			case R.id.menuTitle:
				actionToggleTitle();
				
				return true;				
			default:
				return super.onMenuItemSelected(featureId, item);
			}

		}
		/** 
		 * This method is overwritten inorder to avoid the activity to be reloaded if orientation changes
		 * Has a flag set in android manifest that triggers the method for orientation calls
		 * @see android.app.Activity#onConfigurationChanged(android.content.res.Configuration)
		 */
		@Override
		public void onConfigurationChanged(Configuration newConfig) {
			// TODO Auto-generated method stub
			super.onConfigurationChanged(newConfig);
		}

		/* (non-Javadoc)
		 * @see android.app.Activity#onPause()
		 */
		@Override
		protected void onPause() {
			Log.d(LOG_PREFIX, "onPause called");
			if(slideshowTimerTask!=null){
				slideshowTimerTask.cancel(false);
			}
			super.onPause();
		}
		
		

		/* (non-Javadoc)
		 * @see android.app.Activity#onStop()
		 */
		@Override
		protected void onStop() {
			Log.d(LOG_PREFIX, "onStop called");
			if(slideshowTimerTask!=null){
				//interupt thread if necessary... we need to kill it
				slideshowTimerTask.cancel(true);
			}
			if(fileDownloader!=null && fileDownloader.hasRemainingDownloads()){
				Log.d(LOG_PREFIX, "Stopping downloading of photos");
				fileDownloader.stop();
			}
			
			super.onStop();
		}
		
		

		/* (non-Javadoc)
		 * @see android.app.Activity#onRestart()
		 */
		@Override
		protected void onRestart() {
			Log.d(LOG_PREFIX, "onRestart called");
			//restarting file download
			if(fileDownloader!=null && fileDownloader.hasRemainingDownloads()){
				Log.d(LOG_PREFIX, "Continuing downloading of photos");
				fileDownloader.execute();
			}
			super.onRestart();
		}

		/* (non-Javadoc)
		 * @see android.app.Activity#onResume()
		 */
		@Override
		protected void onResume() {
			Log.d(LOG_PREFIX, "onResume called");
			if(cachedPhotosDeleted){
	            Intent homeIntent = new Intent(this, SlideshowActivity.class);
	            homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            startActivity(homeIntent);
	            super.onResume();
	            return;
			}
			if (isSlideshowRunning){
				slideshowTimerTask= new SlideshowTimerTask();
				slideshowTimerTask.execute();
			}
			
			
			
			super.onResume();
		}

		/**
		 * Share the provided photo through other android apps
		 * 
		 * Will share the image as a image/jpg content type and include title and description as extra
		 * 
		 * @param slideshowPhoto
		 */
		public void actionSharePhoto(SlideshowPhoto slideshowPhoto){
			Log.i(LOG_PREFIX, "Attempting to share photo " + slideshowPhoto);
			//TODO: Refactor this code.. rather ugly due to some GoogleTV related hacks
			
			if(slideshowPhoto!=null){
				Intent shareIntent = new Intent();
				shareIntent.setAction(Intent.ACTION_SEND);
				//we assume the type is image/jpg
				shareIntent.setType("image/jpg");
				
				String sharedText = slideshowPhoto.getTitle()+": "+slideshowPhoto.getDescription() + "\n\n" 
				+ getResources().getString(R.string.share_footer);
				
				//if we have a cached file, add the stream and the sharedText
				//if not, add the url and the sharedText
				if(slideshowPhoto.isCacheExisting(rootFileDirectory)){
					String path = "file://" + rootFileDirectory.getAbsolutePath()+"/"+slideshowPhoto.getFileName();
					Log.i(LOG_PREFIX, "Attempting to pass stream url " + path);
					shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(path));
					shareIntent.putExtra(Intent.EXTRA_TEXT, sharedText);
				}else {
					shareIntent.putExtra(Intent.EXTRA_TEXT, slideshowPhoto.getLargePhoto() + "\n\n"+ sharedText);
				}
				
				shareIntent.putExtra(Intent.EXTRA_SUBJECT,slideshowPhoto.getTitle());
				
				
				//Start the actual sharing activity
				try {
					List<ResolveInfo> relevantActivities=getPackageManager().queryIntentActivities(shareIntent,0);
					if(AndroidUtils.isGoogleTV(getApplicationContext()) || relevantActivities==null || relevantActivities.size()==0){
						Log.i(LOG_PREFIX, "No activity found that can handle image/jpg. Performing simple text share");
						Intent backupShareIntent = new Intent();
						backupShareIntent.setAction(Intent.ACTION_SEND);
						backupShareIntent.setType("text/plain");
						String backupSharedText = slideshowPhoto.getLargePhoto() +"\n\n"+ sharedText;
						backupShareIntent.putExtra(Intent.EXTRA_TEXT,backupSharedText );
						startActivity(backupShareIntent);
					}else {
						startActivity(shareIntent);
					}
					
				}catch (ActivityNotFoundException e) {
					notifyUser("Unable to share current photo");
					
				}
				
			}else {
				notifyUser("Unable to share current photo");
			}
		}
		
		/* (non-Javadoc)
		 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
		 */
		@Override
		public boolean onKeyDown(int keyCode, KeyEvent event) {
			Log.d(LOG_PREFIX, "Keyevent in activity"+ keyCode);
			//Basically some key-aliases for GoogleTV buttons
			switch (keyCode) {
			//hardcoded some keyevents in order to support 2.1
			//case KeyEvent.KEYCODE_MEDIA_STOP:
			//case KeyEvent.KEYCODE_MEDIA_PAUSE:
			case 86:
			case 127:
				actionPauseSlideshow();
				Toast.makeText(this, R.string.msg_pause_slideshow, Toast.LENGTH_SHORT).show();
				return true;
			//case KeyEvent.KEYCODE_MEDIA_PLAY:
			case 126:
				actionResumeSlideshow();
				Toast.makeText(this, R.string.msg_resume_slideshow, Toast.LENGTH_SHORT).show();
				return true;
				
			case KeyEvent.KEYCODE_MEDIA_NEXT:
			case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
				userCreatedTouchEvent=true;
				gallery.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, new KeyEvent(0,0));
				return true;
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
			case KeyEvent.KEYCODE_MEDIA_REWIND:
				userCreatedTouchEvent=true;
				gallery.onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT, new KeyEvent(0,0));
				return true;
				
			default:
				Log.d(LOG_PREFIX, "Unhandled keyevent "+ keyCode);
				break;
			}
			
			return super.onKeyDown(keyCode, event);
		}
		
		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
			if(SlideshowPreferences.KEY_DO_DELETE_CACHE.equals(key)){
				//reset photos
				notifyUser(getString(R.string.msg_cachedphotos_slideshow));
				Intent intent = new Intent(Intent.ACTION_MAIN);
				intent.addCategory(Intent.CATEGORY_HOME);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
				
				cachedPhotosDeleted=true;
				
				/*ArrayList<SlideshowPhoto> cachedDrawables = new ArrayList<SlideshowPhoto>(10);
		        SlideshowPhoto initialPhoto = new SlideshowPhotoDrawable(this,"title", "description",R.drawable.trey3);
		        cachedDrawables.add(initialPhoto);
		        
		        //TODO: Find better way for the rootFile to be passed around
		        imageAdapter=null;
		        imageAdapter=new ImageAdapter(this,0,cachedDrawables,rootFileDirectory);
		        gallery.setAdapter(imageAdapter);
		        gallery.setSelection(0);
				new PhotoUrlsTask().execute();*/
			}else if(SlideshowPreferences.KEY_TRANSITION_TYPE.equals(key)){
				boolean doCustomTransition= SlideshowPreferences.doCustomTransition(getApplicationContext());
				//if doTransition, we should normally check which transition and set the corresponding 
				//in and out animations on the gallery. Currently we have only one, so we skip it
				gallery.setDoCustomTransition(doCustomTransition);
			}else if(SlideshowPreferences.KEY_DISPLAY_TITLE.equals(key)){
				boolean doDisplayPhotoTitle= SlideshowPreferences.doDisplayPhotoTitle(getApplicationContext());
				imageAdapter.setDoDisplayPhotoTitle(doDisplayPhotoTitle);
			}else if(SlideshowPreferences.KEY_DO_DOWNLOAD_ON_3G.equals(key)){
				//attempt to download photos again
				new PhotoUrlsTask().execute();
			}
			
		}

	    
	    /**
	     * Scroll to the next photo. If we reach the end, let's start over
	     */
	    public void actionNextPhoto(){
	    	CustomGallery gallery = (CustomGallery) findViewById(R.id.gallery);
	    	if(gallery==null){
	    		Log.w(LOG_PREFIX, "Gallery view is not found in actionNextPhoto! Let's make sure this doesn't crash the app");
	    		return;
	    	}
	    	
	    	if(userCreatedTouchEvent || gallery.hasUserCreatedTouchEvent()){
	    		Log.i(LOG_PREFIX, "User created a touch even since time task started. Will not skip to next photo yet");
	    		return;
	    	}
	    	
	    	//Log.i(LOG_PREFIX, "Selected position is " + gallery.getSelectedItemPosition()+ " out of "+ gallery.getCount());
	    	
	    	//TODO: Evaluate if we should add all queued photos if we are almost at the end
	    	
	    	if(gallery.getSelectedItemPosition()+1 == gallery.getCount()){
	    		Log.i(LOG_PREFIX, "At the end of the slideshow. Starting on the first photo again");
	    		Analytics.trackEvent(getApplicationContext(), "actions", "slideshow", "cycles-free");
	    		gallery.setSelection(0);
	    	}else {//skip to next photo
	    		gallery.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, new KeyEvent(0,0));
	    	}
	    	
	    	
	    }
	    
	    public void actionToggleTitle(){   
	    	Analytics.trackEvent(getApplicationContext(), "actions", "slideshow", "tittle-toggle");
	    	Log.d(LOG_PREFIX, "action Toggle Title");
	    	//we need the adapter to change the display title setting
	    	CustomGallery gallery = (CustomGallery) findViewById(R.id.gallery);
	    	View selectedView = gallery.getSelectedView();
	    	ImageAdapter adapter=null;
	    	if(gallery==null || selectedView==null || gallery.getAdapter() instanceof ImageAdapter==false){
	    		Log.w(LOG_PREFIX, "Gallery view is not found in actionNextPhoto or adapter is of wrong instance! Let's make sure this doesn't crash the app");
	    		return;
	    	}
	    	adapter= (ImageAdapter)gallery.getAdapter();
	    	
	    	
	    	TextView slideshowTitle = (TextView)selectedView.findViewById(R.id.slideshow_title);
	    	if(slideshowTitle==null){
	    		Log.w(LOG_PREFIX, "slideshowTitle is null. Cannot change visibility");
	    		return;
	    	}
            int currentVisibility=slideshowTitle.getVisibility();
            int newVisibility=0;
            if(currentVisibility==View.INVISIBLE){
            	newVisibility=View.VISIBLE;
            	setVisibilityOfSlideshowText(selectedView, newVisibility);
            	adapter.setDoDisplayPhotoTitle(true);
            }else {
            	newVisibility=View.INVISIBLE;
            	setVisibilityOfSlideshowText(selectedView, newVisibility);
            	adapter.setDoDisplayPhotoTitle(false);
            }
            
            //trick to get cached views to update themselves
            adapter.notifyDataSetChanged();
            //let's extend the time untill the photo changes
            userCreatedTouchEvent=true;
            
            
            //View nextView = gallery.getChildAt(gallery.getSelectedItemPosition()+1);
	    	//setVisibilityOfSlideshowText(nextView, newVisibility);
	    }
	    
	    public static void setVisibilityOfSlideshowText(View slideshowView, int viewVisibilitiy){
	    	if(slideshowView==null){
	    		return;
	    	}
	    	 //let's get the views we want to toggle visibility on
	    	//the values are already populated
	    	TextView slideshowTitle = (TextView)slideshowView.findViewById(R.id.slideshow_title);
            TextView slideshowDescription =(TextView)slideshowView.findViewById(R.id.slideshow_description);
            View layout= (View)slideshowView.findViewById(R.id.slideshow_text_background);
            
            
            if(slideshowTitle==null ||slideshowDescription==null|| layout==null ){
            	Log.w(LOG_PREFIX, "Some of the views we want to toggle are null in setVisibilityOfSlideshowText! Let's make sure this doesn't crash the app"); 
            	return;
            }
            
            //do nothing  if we have an empty title
            if(slideshowTitle.getText()==null|| "".equals(slideshowTitle.getText())){
            	return;
            }
                   
            if(viewVisibilitiy==View.VISIBLE){
            	//Log.d(LOG_PREFIX, "TITLE VISIBLE");
            	slideshowTitle.setVisibility(View.VISIBLE);
            	slideshowDescription.setVisibility(View.VISIBLE);
            	layout.setVisibility(View.VISIBLE);
            	
            }else {
            	//Log.d(LOG_PREFIX, "TITLE INVISIBLE");
            	slideshowTitle.setVisibility(View.INVISIBLE);
            	slideshowDescription.setVisibility(View.INVISIBLE);
            	layout.setVisibility(View.INVISIBLE);
            }
	    	
	    }
	    

		public void actionNextTimerTask() {
			if(isSlideshowRunning){
				userCreatedTouchEvent=false;
				gallery.setUserCreatedTouchEvent(false);
				slideshowTimerTask = new SlideshowTimerTask();
				slideshowTimerTask.execute();
			}
		}
		
		public void actionPauseSlideshow(){
			isSlideshowRunning=false;
			slideshowTimerTask=null;
		}
		
		public void actionResumeSlideshow(){
			isSlideshowRunning=true;
			slideshowTimerTask = new SlideshowTimerTask();
			slideshowTimerTask.execute();
		}
		
	    
	    /**
	     * Called when the list of all photos have been downloaded from backend
	     * 
	     * 
	     * @param slideShowPhotos Photos in the feed, may not exist in cache yet
	     */
		private void actionOnPhotoUrlsDownloaded(List<SlideshowPhoto> slideShowPhotos) {
			Log.i(LOG_PREFIX, "Photo gallery definition downloaded, now looking through the results" );
			
			//Let's add the existing one to the adapter immediately, and send the other to the FileDownloader
			ArrayList<DownloadableObject> notCachedPhotos = new ArrayList<DownloadableObject>(100);
			ArrayList<SlideshowPhoto> cachedPhotos = new ArrayList<SlideshowPhoto>(200);
			
			for (Iterator<SlideshowPhoto> iterator = slideShowPhotos.iterator(); iterator
					.hasNext();) {
				SlideshowPhoto slideshowPhoto = iterator.next();
				if(slideshowPhoto.isCacheExisting(rootFileDirectory)){
					cachedPhotos.add(slideshowPhoto);
				}else {
					notCachedPhotos.add(slideshowPhoto);
				}
			}
			
			if(cachedPhotos.size()>0) {
				//lets randomize all the cached photos
				long seed = System.nanoTime();
				Collections.shuffle(cachedPhotos, new Random(seed));
				
				addSlideshowPhoto(cachedPhotos);
			}
			
			if(notCachedPhotos.size()>0){
				//Rules for download 
				//1. Never download on roaming
				if(AndroidUtils.isConnectedRoaming(getApplicationContext())){
					notifyUser(getString(R.string.msg_connected_roaming));
					return;
				}
				
				boolean connectOn3G = SlideshowPreferences.doDownloadOn3G(getApplicationContext());
				boolean isConnectedToWifi = AndroidUtils.isConnectedToWifi(getApplicationContext());
				boolean isConnectedToWired = AndroidUtils.isConnectedToWired(getApplicationContext());
				//2. Do not download if not connected to Wifi and user has not changed connect to Wifi setting
				if(isConnectedToWifi==false && isConnectedToWired==false && connectOn3G==false){
					notifyUser(getString(R.string.msg_connected_mobile));
				}
				
				//3. Connect if on wifi or if not connected to wifi and wifi setting is changed
				if((isConnectedToWifi==true||isConnectedToWired) || connectOn3G==true){
					Log.i(LOG_PREFIX, "Downloading photos. ConnectedToWifi=" +isConnectedToWifi + " ConnectOn3G="+connectOn3G );
					
					//lets randomize all the non-cached photos
					long seed = System.nanoTime();
					Collections.shuffle(notCachedPhotos, new Random(seed));
					fileDownloader = new FileDownloader(this.getBaseContext(), this, rootFileDirectory, notCachedPhotos);
					fileDownloader.execute();
				}
				
			}else {
				Log.i(LOG_PREFIX, "No new photos to download");	
			}
			
			
		}
		/**
		 * Method called if the download of the photo urls failed. 
		 * Should revert to only cached photos
		 * 
		 */
		private void actionOnPhotoUrlsFailed(){
			notifyUser(getString(R.string.msg_unableto_connect));
			ArrayList<SlideshowPhoto> cachedPhotos = new ArrayList<SlideshowPhoto>(200);
			
			File[] filePhotos = rootFileDirectory.listFiles();
			if(filePhotos!=null){	
				for (int i = 0; i < filePhotos.length; i++) {
					cachedPhotos.add(new SlideshowPhotoCached(getApplicationContext(), filePhotos[i]));
				}
				
				if(cachedPhotos.size()>0) {
					addSlideshowPhoto(cachedPhotos);
				}
			}
		}
		
		@Override
		public void onDownloadCompleted(DownloadableObject downloadableObject) {
			//unsafe cast, but we have control
			SlideshowPhoto slideshowPhoto= (SlideshowPhoto)downloadableObject;
			if(queuedSlideshowPhotos==null){
				queuedSlideshowPhotos=new ArrayList<SlideshowPhoto>(20);
			}
			queuedSlideshowPhotos.add(slideshowPhoto);

			//we want to add the slideshow photos as seldom as possible, as it creates a refresh of views
			if(gallery.getCount()<=5 &&queuedSlideshowPhotos.size() >=5 ){
				addSlideshowPhoto(queuedSlideshowPhotos);
				queuedSlideshowPhotos=null;
			}else if(gallery.getCount()-gallery.getSelectedItemPosition()<=3 && queuedSlideshowPhotos.size()>=10){
				addSlideshowPhoto(queuedSlideshowPhotos);
				queuedSlideshowPhotos=null;
			}else if(queuedSlideshowPhotos.size()>=50){
				addSlideshowPhoto(queuedSlideshowPhotos);
				queuedSlideshowPhotos=null;
			}
			
			
		}

		@Override
		public void onDownloadError(DownloadableObject downloadableObject) {
			SlideshowPhoto slideshowPhoto= (SlideshowPhoto)downloadableObject;
			Log.w(LOG_PREFIX, "Unable to download slideshow photo with large photo url "+ slideshowPhoto.getLargePhoto());
		}
		
		public void onAllDownloadsFailed (String message){
			notifyUser(message);
		}
		
		@Override
		public void onAllDownloadsCompleted() {
			if(queuedSlideshowPhotos!=null){
				addSlideshowPhoto(queuedSlideshowPhotos);
				queuedSlideshowPhotos=null;
			}
			
		}


	    
	    
	    public void addSlideshowPhoto(SlideshowPhoto slideshowPhoto){
	    	imageAdapter.add(slideshowPhoto);
	    	imageAdapter.notifyDataSetChanged();
	    }
	    
	    public void addSlideshowPhoto(List<SlideshowPhoto> slideshowPhotos){
	    	//addAll is API 11+ 
	    	//imageAdapter.addAll(slideshowPhotos);
	    	for (Iterator<SlideshowPhoto> iterator = slideshowPhotos.iterator(); iterator
					.hasNext();) {
				imageAdapter.add( (SlideshowPhoto) iterator.next());
			}
	    	
	    	imageAdapter.notifyDataSetChanged();
	    }
	    
		protected void notifyUser(CharSequence msg) {
			Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
		}
		

	    
	    /**
	     * The image adapter for the gallery which binds it to the data
	     * 
	     * @author dagfinn.parnas
	     *
	     */
	    public class ImageAdapter extends ArrayAdapter<SlideshowPhoto> {
	        private final int mGalleryItemBackground;
	        private final Context context;
	        private File rootPhotoFolder;
	        private boolean doDisplayPhotoTitle;


	        public ImageAdapter(Context context, int textViewResourceId, List<SlideshowPhoto> listObjects, File rootPhotoFolder, boolean doDisplayPhotoTitle) {
	        	super(context,textViewResourceId,listObjects);
	        	this.context=context;
	        	this.rootPhotoFolder=rootPhotoFolder;
	        	this.doDisplayPhotoTitle=doDisplayPhotoTitle;
	            // See res/values/attrs.xml for the <declare-styleable>
	        	// ensures the default handling of the theme is used
	            TypedArray a = obtainStyledAttributes(R.styleable.Gallery);
	            mGalleryItemBackground = a.getResourceId(
	                    R.styleable.Gallery_android_galleryItemBackground, 0);
	            a.recycle();

	        }
	        
	        public void setDoDisplayPhotoTitle( boolean doDisplayPhotoTitle){
	        	this.doDisplayPhotoTitle=doDisplayPhotoTitle;
	        }
	        
	        public boolean shouldDisplayPhotoTitle(){
	        	return doDisplayPhotoTitle;
	        }
	        

	        public View getView(int position, View convertView, ViewGroup parent) {
	            
	        	
	        	View slideshowView=convertView;
	        	if(position>= getCount()){
	        		return null;
	        	}
	        	SlideshowPhoto slideshowPhoto=getItem(position);
	        	Log.i(LOG_PREFIX,position + " title:"+slideshowPhoto.getTitle());
	        	
	        	
	            if (slideshowView == null) {
	            	LayoutInflater viewInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	            	slideshowView = viewInflater.inflate(R.layout.slideshow_item, null);
	            }

	            ImageView slideshowImageView = (ImageView)slideshowView.findViewById(R.id.slideshow_photo);	
	            slideshowImageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
	            slideshowImageView.setAdjustViewBounds(true);
	            //make sure we do not exceed the opengl hardware accl max size
	            slideshowImageView.setMaxHeight(2048);
	            slideshowImageView.setMaxWidth(2048);
            
                // The preferred Gallery item background
	            slideshowImageView.setBackgroundResource(mGalleryItemBackground);
	            slideshowImageView.setBackgroundColor(Color.TRANSPARENT);
	            
	            slideshowImageView.setTag(slideshowPhoto.getFileName());
	            //These lines of code are in a separate async task in order to not block the UI thread for approx 300 ms
	            //Drawable drawable = slideshowPhoto.getLargePhotoDrawable(rootPhotoFolder);
	            //slideshowImageView.setImageDrawable(drawable);
	            new ReadPhotoFromFileTask(slideshowImageView,slideshowPhoto,rootPhotoFolder).execute();
	            
	           

					
		
	            
	            //if it is a promotion, add a click listener
	            if(slideshowPhoto.isPromotion()){
	            	//uncomment if you want certain promotion photos to link to the paid version      
	            	/*		
	            	slideshowImageView.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							Analytics.trackEvent(getApplicationContext(), "actions", "promotion", "clicked");
							try {
								startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:com.elsewhat.android.slideshow.paid")));
							}catch (ActivityNotFoundException e) {
								startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://market.android.com/details?id=com.elsewhat.android.slideshow.paid")));
							}
						}
					});*/
	            }else {//should we display title and description on click?
	            	//conflict between onFling and on click means we cannot use this approach
	            	/*
	            	slideshowImageView.setOnTouchListener(new OnTouchListener() {
						long timeMSDown;
						@Override
						public boolean onTouch(View v, MotionEvent event) {
							switch(event.getActionMasked())
					          {
					             case MotionEvent.ACTION_DOWN:
					            	 timeMSDown=System.currentTimeMillis();
					            	 return true;
					                 //break;
					             case MotionEvent.ACTION_UP:
					            	 long differenceMS= System.currentTimeMillis()-timeMSDown;
					            	 Log.d(LOG_PREFIX, "Touch event difference in MS : "+differenceMS);
					            	 if(differenceMS<200){
					            		 actionToggleTitle(); 
					            	 }
					            	 break;
					          }
							return false;
						}
					});*/
	            }
	            
	            
	            TextView slideshowTitle = (TextView)slideshowView.findViewById(R.id.slideshow_title);
	            slideshowTitle.setText(slideshowPhoto.getTitle());
	            
	            TextView slideshowDescription = (TextView)slideshowView.findViewById(R.id.slideshow_description);
	            slideshowDescription.setText(slideshowPhoto.getDescription());
	          
	            //find out if we should hide the text descriptions
	            boolean isEmptyTitle =false;
	            if(slideshowPhoto.getTitle()==null|| "".equals(slideshowPhoto.getTitle().trim())){
	            	isEmptyTitle=true;
	            }
	            
	            if(doDisplayPhotoTitle==false || slideshowPhoto.isPromotion() || isEmptyTitle){
	            	slideshowTitle.setVisibility(View.INVISIBLE);
	            	slideshowDescription.setVisibility(View.INVISIBLE);
	            	View layout= slideshowView.findViewById(R.id.slideshow_text_background);
	            	layout.setVisibility(View.INVISIBLE);
	            }
	           
	            //lastView = slideshowView;
	            //lastFileName=slideshowPhoto.getFileName();
	            
	            return slideshowView;
	        }
	    }	    
	    
	    /**
	     * Asynch task for displaying a new photo at a regular interval
	     * 
	     */
	    public class SlideshowTimerTask extends AsyncTask<Void, Void, Void> {
	    	long sleepMS=15000;
			
			public SlideshowTimerTask(){
				sleepMS=SlideshowPreferences.getDisplayTimeMS(getApplicationContext());
			}
			
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				if(isSlideshowRunning){
					actionNextPhoto();
					actionNextTimerTask();
				}
			}

			@Override
			protected Void doInBackground(Void... arg0) {	
				try
				    {
				        Thread.sleep(sleepMS);
				    }
				    catch (InterruptedException ie)
				    {
				    	Log.d(LOG_PREFIX, "SlideshowTimerTask received  InterruptedException", ie);
				    }
				// TODO Auto-generated method stub
				return null;
			}

			/* (non-Javadoc)
			 * @see android.os.AsyncTask#onCancelled()
			 */
			@Override
			protected void onCancelled() {
				Log.d(LOG_PREFIX, "SlideshowTimerTask onCancelled called");
				super.onCancelled();
			}
	    }
	    
	    
	    
	    /**
	     * Asynch task for retrieving photo urls from the backend
	     * 
	     */
	    public class PhotoUrlsTask extends AsyncTask<Void, Void, Void> {
	    	String photoUrls[];
	    	Throwable exception=null;
	    	
	    	List<SlideshowPhoto> slideshowPhotos;
			@Override
			protected Void doInBackground(Void... arg0) {
	    		try {
	    			//TODO-FORK Update flickr public photo set id
	    			String flickrPhotosetID = "72157628899979341";
	    			slideshowPhotos = new FlickrPublicSetBackend(flickrPhotosetID).getSlideshowPhotos(getBaseContext());
	    			
					//slideshowPhotos = new OPMLBackend().getSlideshowPhotos(getBaseContext());
				} catch (Throwable e) {
					Log.w(LOG_PREFIX, "Got exception while downloading photos",e);
					exception=e;
				}
				return null;
			}
			
			
			@Override
			protected void onPostExecute(Void result) {
				//TODO: Handle failure to download
				if(exception==null && slideshowPhotos!=null){
					actionOnPhotoUrlsDownloaded(slideshowPhotos);
				}else {
					actionOnPhotoUrlsFailed();
				}
				
				
				/*if(photoUrls!=null){
					Toast.makeText(getBaseContext(), "Found " + photoUrls.length + " photos from backend", Toast.LENGTH_SHORT).show();
					for (int i = 0; i < photoUrls.length && i<4; i++) {
						Log.i("SmugMug", photoUrls[i]);
						new AddDrawableTask(photoUrls[i]).execute();
					}
					
				}*/
			}
	    }
	    
	    /**
	     * Asynch task for loading a photo from File I/O.
	     * 
	     * A temporary loading drawable is set while waiting
	     * 
	     */
	    public class ReadPhotoFromFileTask extends AsyncTask<Void, Void, Void> {
	    	ImageView imageView;
	    	SlideshowPhoto slideshowPhoto;
	    	File fileFolder;
	    	Drawable drawable;
	    	boolean outOfMemoryError=false;
	    	public ReadPhotoFromFileTask(ImageView imageView, SlideshowPhoto slideshowPhoto, File fileFolder){
	    		this.imageView=imageView;
	    		this.slideshowPhoto=slideshowPhoto;
	    		this.fileFolder=fileFolder;
	    		//TODO: Create new photo
	    		imageView.setImageResource(R.drawable.loading);
	    	}
			@Override
			protected Void doInBackground(Void... arg0) {
				try {
				drawable = slideshowPhoto.getLargePhotoDrawable(fileFolder);
				}catch (OutOfMemoryError e) {
					outOfMemoryError=true;
				}
				return null;
			}
			
			
			@Override
			protected void onPostExecute(Void result) {
				if(drawable!=null){
					//special ICS bugfix since resources.getDrawable returns too large (>2048) drawable
					//no longer needed as we read the drawable through the raw stream and BitMap drawable.
					//int width= drawable.getIntrinsicWidth();
					//int height = drawable.getIntrinsicHeight();
					/*
					if(width>2048 || height>2048){
						BitmapDrawable bitmapDrawable =(BitmapDrawable)drawable;
						Bitmap bitmap = bitmapDrawable.getBitmap();
					    
						boolean widthLargerThanHeight = (width>height)?true:false;
						
						double scaleFactor=1.0;
						if(widthLargerThanHeight){
							scaleFactor=2048.0/width;
						}else {
							scaleFactor=2048.0/height;
						}
						int newWidth=(int)(width*scaleFactor);
						int newHeight=(int)(height*scaleFactor);;
						
						
						Bitmap bitmapScaled = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false);
					    drawable = new BitmapDrawable(bitmapScaled);
					}*/
					
					imageView.setImageDrawable(drawable);
					//required because of a bug on Sony Google TV that retain the size of the loading image
					//imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
					imageView.requestLayout();
					//Log.d(LOG_PREFIX, "ImageView (" + imageView.getWidth() + ","+imageView.getHeight()+")");
				}else if(outOfMemoryError==true){
					notifyUser(getString(R.string.msg_outofmemoryerror));
				}else {
					notifyUser(getString(R.string.msg_unable_to_display_photo));
				}
				
			}
	    }

}