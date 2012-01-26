package com.elsewhat.android.slideshow.api;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import com.elsewhat.android.slideshow.R;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;


/**
 * Queueable object that cause a heavy file IO read load (300-2000 ms)
 * 
 * Will use WeakReferences in order to make sure objects are still needed when it is ready for processing
 * 
 * @author dagfinn.parnas
 *
 */
public class QueueablePhotoObject implements AsyncQueueableObject {
	protected SlideshowPhoto slideshowPhoto;
	protected File rootFolder;
	protected WeakReference<View> weakRefslideshowView;
	protected ImageView myImageView;
	protected boolean wasGarbageCollected = false;
	protected boolean wasOutOfMemory = false;
	protected Drawable result;
	protected String tagOnCompletion=null;
	protected int maxWidth;
	protected int maxHeight;

	/**
	 * Constructor The View is stored as a WeakReference
	 * 
	 * @param slideshowPhoto
	 * @param imageView
	 */
	public QueueablePhotoObject(SlideshowPhoto slideshowPhoto,
			View slideshowView, File rootFolder, String tagOnCompletion, int maxWidth, int maxHeight) {
		this.slideshowPhoto = slideshowPhoto;
		this.rootFolder = rootFolder;
		this.weakRefslideshowView = new WeakReference<View>(slideshowView);
		this.tagOnCompletion=tagOnCompletion;
		this.maxWidth=maxWidth;
		this.maxHeight=maxHeight;
	}

	/**
	 * Perform the operation of reading the photo from file in the background
	 * 
	 */
	@Override
	public void performOperation() {
		View slideshowView = weakRefslideshowView.get();
		if (slideshowView == null) {
			wasGarbageCollected = true;
			return;
		} else {
			try {
				result= slideshowPhoto.getLargePhotoDrawable(rootFolder,maxWidth,maxHeight);
				return;
			} catch (OutOfMemoryError e) {
				Log.i("QueueablePhotoObject",
				"Out of memory while getting drawable");
				wasOutOfMemory = true;
				return;
			} catch (IOException e2) {
				Log.i("QueueablePhotoObject",
				"IOException file reading photo "+ slideshowPhoto,e2);
				wasOutOfMemory = true;
			}
		}
	}	
	
	/**
	 * Handle operation results will be run on the UI thread 
	 * and will be responsible for setting the read drawable to the ImageView drawable 
	 * 
	 */
	@Override
	public void handleOperationResult() {
		View slideshowView = weakRefslideshowView.get();
		
		
		if(wasOutOfMemory){
			return;
		}else if(wasGarbageCollected){
			return;
		}else if (slideshowView == null) {
			Log
					.d("QueueablePhotoObject",
							"Drawable loaded, but imageview has been garbage collected since read started");
			return;
		} else {
			ImageView imageView = (ImageView)slideshowView.findViewById(R.id.slideshow_photo);	
			imageView.setImageDrawable(result);
			if(tagOnCompletion!=null){
				imageView.setTag(tagOnCompletion);
			}
			imageView.requestLayout();
			return;
		}
	}

	

	
	
	public String toString(){
		if(slideshowPhoto!=null){
			return "QueueablePhotoObject:"+slideshowPhoto.getTitle();
		}else {
			return super.toString();
		}
	}

}
