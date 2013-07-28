package com.elsewhat.android.slideshow.api;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;

import com.elsewhat.android.slideshow.R;
import com.elsewhat.android.slideshow.activities.ISlideshowInstance;

/**
 * The image adapter for the gallery which binds it to the data
 * 
 * @author dagfinn.parnas
 *
 */
public class ImageAdapter extends ArrayAdapter<SlideshowPhoto> {
    private final int mGalleryItemBackground;
    private final Context context;
    private ISlideshowInstance slideshowInstance;
    private File rootPhotoFolder;
    private boolean doDisplayPhotoTitle;
    private HashMap<Integer,WeakReference<View>> mapWeakRefViews;
    private final String LOADING_TAG="loading";
    private final String LOADED_TAG="Finished";
    private final String LOG_PREFIX="ElsewhatSlideshow";
    private int screenWidthPx;
    private int screenHeightPx;
    

    public ImageAdapter(Context context, ISlideshowInstance slideshowInstance, int textViewResourceId, List<SlideshowPhoto> listObjects, File rootPhotoFolder, boolean doDisplayPhotoTitle) {
    	super(context,textViewResourceId,listObjects);
    	this.context=context;
    	this.slideshowInstance=slideshowInstance;
    	this.rootPhotoFolder=rootPhotoFolder;
    	this.doDisplayPhotoTitle=doDisplayPhotoTitle;
        // See res/values/attrs.xml for the <declare-styleable>
    	// ensures the default handling of the theme is used
        TypedArray a = context.obtainStyledAttributes(R.styleable.Gallery);
        mGalleryItemBackground = a.getResourceId(
                R.styleable.Gallery_android_galleryItemBackground, 0);
        a.recycle();
        
        mapWeakRefViews= new HashMap<Integer,WeakReference<View>>(500);
        
        screenWidthPx = slideshowInstance.getScreenWidth();
        screenHeightPx = slideshowInstance.getScreenHeight();

    }
    
    public void setDoDisplayPhotoTitle( boolean doDisplayPhotoTitle){
    	this.doDisplayPhotoTitle=doDisplayPhotoTitle;
    }
    
    public boolean shouldDisplayPhotoTitle(){
    	return doDisplayPhotoTitle;
    }
    
    /**
     * Trigger the actionToggleTitle method to be call.
     * This is useful since the adapter is the only visible part for the Gallery
     * 
     */
    public void triggerActionToggleTitle(){
    	slideshowInstance.actionToggleTitle();
    }
    

    
    public View getView(int position, View convertView, ViewGroup parent) {        	
    	View slideshowView=convertView;
    	if(position>= getCount()){
    		return null;
    	}
    	
    	
    	SlideshowPhoto slideshowPhoto=getItem(position);
    	Log.d(LOG_PREFIX,position + " title:"+slideshowPhoto.getTitle());
    	
    	boolean copyDrawableFromCachedView=false;
    	View cachedView=null;
    	Integer mapKey = new Integer(position);
        if (slideshowView == null) {
        	//let's check the weak references for this View
        	if(mapWeakRefViews.containsKey(mapKey)){
        		//we have a key, but the View may be garbage collected
        		WeakReference<View> weakRefView = mapWeakRefViews.get(mapKey);
        		cachedView = weakRefView.get();
        		if(cachedView==null){
        			//view was cached, but has been deleted.
        			//it will be replaced later in this method, so don't bother deleting it from the hashmap
        		}else if(cachedView.getParent()!=null){
        			//Log.i(LOG_PREFIX,position + " was cached, but had a parent. So close!");
        			copyDrawableFromCachedView=true;
        		}else {
        			Log.d(LOG_PREFIX,position + " returned through weak reference caching. Yeah!");
        			return cachedView;
        		}
        	}
        	
        	//if no cached value, create it from the resource definition
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
        
        slideshowImageView.setTag(LOADING_TAG);
        slideshowImageView.setImageResource(R.drawable.loading);
        
        //OLD METHOD
        //These lines of code are in a separate async task in order to not block the UI thread for approx 300 ms
        //Drawable drawable = slideshowPhoto.getLargePhotoDrawable(rootPhotoFolder);
        //slideshowImageView.setImageDrawable(drawable);
        //new ReadPhotoFromFileTask(slideshowImageView,slideshowPhoto,rootPhotoFolder).execute();
        
       

        //This section applies if we have a cached view, but it cannot be reuse directly since it has a parent
        //let us copy the drawable
        boolean slideShowImageDrawableMissing=true;
        if(copyDrawableFromCachedView==true && cachedView!=null){
        	//reusing the drawable from a cached view
        	ImageView cachedSlideshowImageView = (ImageView)cachedView.findViewById(R.id.slideshow_photo);	
        	String cachedTag = (String)cachedSlideshowImageView.getTag();
        	if(cachedSlideshowImageView!=null && cachedTag!=null && cachedTag.equals(LOADED_TAG)){
        		slideshowImageView.setImageDrawable(cachedSlideshowImageView.getDrawable());
        		slideShowImageDrawableMissing=false;
        		Log.d(LOG_PREFIX,position+" Cached photo drawable reused. Yeah!");
        	}else {
        		Log.i(LOG_PREFIX,position+" Cached Photo is not loaded yet, so could not use cache. Doh!");
        	}
        }
        	
        if(slideShowImageDrawableMissing){
            //NEW METHOD
            //Add to a last-in/first-out queue
        	AsyncQueueableObject queueablePhotoObject=null;
            queueablePhotoObject = new QueueablePhotoObject(slideshowPhoto, slideshowView, rootPhotoFolder,LOADED_TAG, screenWidthPx,screenHeightPx);
            slideshowInstance.addToAsyncReadQueue(queueablePhotoObject);
            Log.d(LOG_PREFIX,position+" is being loaded in a background async task");
        }
        
        
        TextView slideshowTitle = (TextView)slideshowView.findViewById(R.id.slideshow_title);
        slideshowTitle.setText(slideshowPhoto.getTitle());
        
        //Scrolling for the description causes a memory leak
        //ref http://stackoverflow.com/questions/8900212/adding-a-simple-scrollview-to-gallery-causes-a-memory-leak
        //Therefore we use a TextSwitcher which we will use a Timer job to swap between parts of the string
        final TextSwitcher slideshowDescription = (TextSwitcher)slideshowView.findViewById(R.id.slideshow_description);
        Animation outAnim = AnimationUtils.loadAnimation(context,
                R.anim.slide_out_down);
        Animation inAnim = AnimationUtils.loadAnimation(context,
                R.anim.slide_in_up);		
        		
        slideshowDescription.setInAnimation(inAnim);
        slideshowDescription.setOutAnimation(outAnim);
        
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
        
        //add the view to our weak reference caching
        WeakReference<View> weakRefView = new WeakReference<View>(slideshowView);
        mapWeakRefViews.put(mapKey, weakRefView);
        
        
        //DEBUG
        String classLayoutParam=null;
        Object objectLayoutParam = slideshowView.getLayoutParams();
        if(objectLayoutParam!=null){
        	classLayoutParam=objectLayoutParam.getClass().getName();
        }
        Log.d(LOG_PREFIX, "Layout params class="+classLayoutParam+ " value="+objectLayoutParam);
        
        return slideshowView;
    }

	public void onGalleryNewPhoto(View view) {
		slideshowInstance.setUpScrollingOfDescription();
		
	}
}	    