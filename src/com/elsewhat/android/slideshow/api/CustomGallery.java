package com.elsewhat.android.slideshow.api;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.Adapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ViewAnimator;

import com.elsewhat.android.slideshow.R;
import com.elsewhat.android.slideshow.activities.SlideshowActivity;


public class CustomGallery extends Gallery implements AnimationListener {


	boolean mDoCustomTransition=false;
	Animation inAnimation=null;
	Animation outAnimation=null;
	ViewAnimator viewAnimator=null;
	boolean userCreatedTouchEvent = false;
	boolean ongoingAnimation= false;
	
	


	public CustomGallery(Context context) {
		super(context);
		 initAnimators();
	}

	public CustomGallery(Context context, AttributeSet attrs) {
		super(context, attrs);
		 initAnimators();
	}

	public CustomGallery(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		 initAnimators();
	}
	
	protected void initAnimators() {
		inAnimation = new AlphaAnimation(0.0f, 1.0f);
		inAnimation.setDuration(2500);
		inAnimation.setAnimationListener(this);
		
		outAnimation = new AlphaAnimation(1.0f, 0.0f);
		outAnimation.setDuration(1500);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Gallery#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.i("CustomGallery", "onKeydown in Customer Gallery . Key code "+keyCode);
		if(event instanceof FlingKeyEvent){
			//use the default handling if this came from a fling event
			return super.onKeyDown(keyCode,null);
		}if(!mDoCustomTransition){
			//leave the transition to the super class
			userCreatedTouchEvent=true;
			//we hardcode this here, since it is changed in onFling and we do not know when that action is finished
			setAnimationDuration(5500);
			return super.onKeyDown(keyCode, event);
		}else {
			//handle the switch without transition ourselves
			switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_LEFT:
				userCreatedTouchEvent=true;
				if (getCount() > 0 && getSelectedItemPosition() >0) {
					setSelection(getSelectedItemPosition()-1);
				}
				return true;

			case KeyEvent.KEYCODE_DPAD_RIGHT:
				userCreatedTouchEvent=true;
				//Log.i("CustomGallery", "Before anim Current: " + getSelectedItemPosition() + " visible:"+ getFirstVisiblePosition()+ " count:"+ getCount());
				if (getCount() > 0 && getSelectedItemPosition() < getCount() - 1) {
					View currentView = getSelectedView();
					if(currentView==null){
						Log.w("CustomGallery", "CurrentView is null in onKeyDown. Should not happen, but lets try to skip to the next photo.");
						nextSelection();
						return true;
					}
					
					View unboundNewView= getAdapter().getView(getSelectedItemPosition()+1, null, null);
					
					Log.d("CustomGallery","About to evaluate an animation. OngoignAnimation="+ongoingAnimation+ " inanim="+inAnimation+" outanim="+outAnimation);
					
					//check if we already have an animation as well
					if(inAnimation==null && outAnimation==null || ongoingAnimation){
						nextSelection();
						//assume it is not ongoing anymore
						//could also be the action listener which is invalid
						//ongoingAnimation=false;
					}else {
						
						viewAnimator = (ViewAnimator)currentView;
						viewAnimator.addView(unboundNewView);
						//setup animation
						
						viewAnimator.setInAnimation(inAnimation);
						viewAnimator.setOutAnimation(outAnimation);
						//trigger the animation and the event listener when finished
						ongoingAnimation=true;
						viewAnimator.showNext();
						
					}
				}
				return true;
			}

			return super.onKeyDown(keyCode, event);
		}

	}
	
	public void nextSelection(){
		//first condition should not happen
		if(getSelectedItemPosition()+1 >= getCount()){
			setSelection(0);
		}else {
			setSelection(getSelectedItemPosition()+1);
		}
		//make sure we clean up after us. Remove the temporary view
		if(viewAnimator!=null && viewAnimator.getChildCount()==2){
			//remove animations in order to make sure event listener is not triggered
			viewAnimator.setInAnimation(null);
			viewAnimator.setOutAnimation(null);
			viewAnimator.removeViewAt(1);
			ongoingAnimation=false;
		}
		//
		
		//let's make sure the visibility of the title matches what the user wants
		//this is required since the view may be cached when the visibility was changed
		View selectedView = getSelectedView();
		ImageAdapter adapter = (ImageAdapter)getAdapter();
		int visibility =adapter.shouldDisplayPhotoTitle()?View.VISIBLE:View.INVISIBLE;
		SlideshowActivity.setVisibilityOfSlideshowText(selectedView,visibility);
		
		//Notify the adapter that a new photo is being display.
		//this is used for initiating the TextSwitcher of the description
		ImageAdapter imageAdapter = (ImageAdapter)adapter;
		imageAdapter.onGalleryNewPhoto(selectedView);
		
		//this is an experimental feature for zooming in on the photo.not quite ready due to performance
		//new ZoomCurrentViewSoonTask(100).execute();

	}
	

	
	/**
	 * @return the inAnimation
	 */
	public Animation getInAnimation() {
		return inAnimation;
	}

	/* See http://stackoverflow.com/questions/2373617/how-to-stop-scrolling-in-a-gallery-widget
	 * @see android.widget.Gallery#onFling(android.view.MotionEvent, android.view.MotionEvent, float, float)
	 */
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		//Log.d("CustomGallery", "onFling called");
		userCreatedTouchEvent=true;
		//the animation event listener might not trigger
		ongoingAnimation=false;
		
		float velMax = 2500f;
	    float velMin = 1000f;
	    float velX = Math.abs(velocityX);
	    if (velX > velMax) {
	      velX = velMax;
	    } else if (velX < velMin) {
	      velX = velMin;
	    }
	    velX -= 600;
	    int k = 500000;
	    int speed = (int) Math.floor(1f / velX * k);
	    setAnimationDuration(speed);

	    int kEvent;
	    if (isScrollingLeft(e1, e2)) {
	      // Check if scrolling left
	      kEvent = KeyEvent.KEYCODE_DPAD_LEFT;
	    } else {
	      // Otherwise scrolling right
	      kEvent = KeyEvent.KEYCODE_DPAD_RIGHT;
	    }
	    onKeyDown(kEvent, new FlingKeyEvent(0, 0));

	    return true;
		
	    //TODO:remove
		//nextSelection();
		//reducing the speed of the fling to a more suitable level
		//return super.onFling(e1, e2, velocityX/3, velocityY/3);
	}
	
	/* (non-Javadoc)
	 * @see android.widget.Gallery#onSingleTapUp(android.view.MotionEvent)
	 */
	@Override
	public boolean onSingleTapUp(MotionEvent motionEvent) {
		Log.d("CustomGallery", "onSingleTapUp called for selection " + getSelectedItemPosition());
		
		Object currentObject = getAdapter().getItem(getSelectedItemPosition());
		if(currentObject!=null && currentObject instanceof SlideshowPhoto){
			SlideshowPhoto currentSlideshowPhoto = (SlideshowPhoto)currentObject;
			if(currentSlideshowPhoto.isPromotion()){
				
				Analytics.trackEvent(getContext(), "actions", "promotion", "clicked");
				Log.d("CustomGallery", "Promotion clicked, sending the user to the market for the app com.stuckincustoms.slideshow.premium" );
				try {
					getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:com.stuckincustoms.slideshow.premium")));
				}catch (ActivityNotFoundException e) {
					getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://market.android.com/details?id=com.stuckincustoms.slideshow.premium")));
				}
				return true;
			}else {
				Adapter adapter = getAdapter();
				if(adapter instanceof ImageAdapter){
					ImageAdapter imageAdapter = (ImageAdapter)adapter;
					imageAdapter.triggerActionToggleTitle();
					return true;
				}
			}
			
		}
		return super.onSingleTapUp(motionEvent);
	}
	
	private boolean isScrollingLeft(MotionEvent e1, MotionEvent e2){
		  return e2.getX() > e1.getX();
		}
	
	/**
	 * Set when a touch event is called on this gallery
	 * @return the userCreatedTouchEvent
	 */
	public boolean hasUserCreatedTouchEvent() {
		return userCreatedTouchEvent;
	}

	/**
	 * @param userCreatedTouchEvent the userCreatedTouchEvent to set
	 */
	public void setUserCreatedTouchEvent(boolean userCreatedTouchEvent) {
		this.userCreatedTouchEvent = userCreatedTouchEvent;
	}

	/**
	 * @param inAnimation the inAnimation to set
	 */
	public void setInAnimation(Animation inAnimation) {
		this.inAnimation = inAnimation;
		if(inAnimation!=null){
			inAnimation.setAnimationListener(this);
		}
	}

	/**
	 * @return the outAnimation
	 */
	public Animation getOutAnimation() {
		return outAnimation;
	}

	/**
	 * @param outAnimation the outAnimation to set
	 */
	public void setOutAnimation(Animation outAnimation) {
		this.outAnimation = outAnimation;
	}
	
	public void setDoCustomTransition(boolean doCustomTransition){
		mDoCustomTransition= doCustomTransition;
	}

	
	public boolean getDoCustomTransition(){
		return mDoCustomTransition;
	}

	@Override
	public void onAnimationEnd(Animation arg0) {
		ongoingAnimation=false;
		nextSelection();
		
		


		
	}

	@Override
	public void onAnimationRepeat(Animation animation) {
		//ignore
	}

	@Override
	public void onAnimationStart(Animation animation) {
		//ignore
	}
	
	/* (non-Javadoc)
	 * @see android.widget.Gallery#onLayout(boolean, int, int, int, int)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		try {
			super.onLayout(changed, l, t, r, b);
		}catch (ClassCastException e) {
			Log.e("CustomGallery", "Got a ClassCastException in onLayout most likely caused by a racecondition in the mRecycle bin ", e);
			
			
			Class classGallery = this.getClass().getSuperclass().getSuperclass();
			Method methodRecycle;
			try {
				methodRecycle = classGallery.getDeclaredMethod("recycleAllViews");
				methodRecycle.setAccessible(true);
				methodRecycle.invoke(this);
				Log.i("CustomGallery", "Succeeded in calling recycleAllViews");
				return;
			} catch (SecurityException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (NoSuchMethodException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (IllegalArgumentException e3) {
				// TODO Auto-generated catch block
				e3.printStackTrace();
			} catch (IllegalAccessException e4) {
				// TODO Auto-generated catch block
				e4.printStackTrace();
			} catch (InvocationTargetException e5) {
				// TODO Auto-generated catch block
				e5.printStackTrace();
			}
			Log.i("CustomGallery", "Failed in calling recycleAllViews");
			
			/*
			//let us call onLayout slightly in the future
			final boolean oldChanged=changed;
			final int oldL=l;
			final int oldT=t;
			final int oldR=r;
			final int oldB=b;
			final Runnable callOnLayout = new Runnable() {
		           public void run() {
		        	   onLayout(oldChanged,oldL,oldT,oldR,oldB);
		           }
		    };
		    
		    final Handler handler = new Handler();
		    handler.postAtTime(callOnLayout, 300L);
		      */ 
		}
		
	}	
	
    /**
     * Asynch task for displaying a new photo at a regular interval
     * 
     */
    public class ZoomCurrentViewSoonTask extends AsyncTask<Void, Void, Void> {
    	long sleepMS;
		
		public ZoomCurrentViewSoonTask(long ms){
			sleepMS=ms;
		}
		
		protected void onPostExecute(Void result) {
			View currentView = getSelectedView();
			if(currentView!=null){
				//transfering the image from the already loaded view, to the unbound one
				ImageView photo = (ImageView)currentView.findViewById(R.id.slideshow_photo);
				
				Random random = new Random (System.nanoTime());
				float pivotXFactor = random.nextFloat();
				float pivotYFactor = random.nextFloat();
				float scaleFactor;
				scaleFactor =  1.0f+ random.nextFloat()/4;
				
				
				Animation zoomAnimation = new ScaleAnimation(1.0f,scaleFactor,1.0f,scaleFactor,Animation.RELATIVE_TO_SELF,pivotXFactor,Animation.RELATIVE_TO_SELF,pivotYFactor);
				zoomAnimation.setDuration(10000);
				zoomAnimation.setFillAfter(false);
				zoomAnimation.setFillBefore(false);
				photo.startAnimation(zoomAnimation);
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
			    	Log.d("CustomGallery", "SlideshowTimerTask received  InterruptedException", ie);
			    }
			// TODO Auto-generated method stub
			return null;
		}

    }

}
