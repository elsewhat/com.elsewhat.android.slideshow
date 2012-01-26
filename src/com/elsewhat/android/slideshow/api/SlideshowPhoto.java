package com.elsewhat.android.slideshow.api;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.elsewhat.android.slideshow.activities.SlideshowActivity;

import android.graphics.drawable.Drawable;
import android.util.Log;

public class SlideshowPhoto implements DownloadableObject{
	protected String title;
	protected String description;
	protected String thumbnail;
	protected String smallPhoto;
	protected String largePhoto;
	protected boolean isPromotion=false;
	protected String fileType=".jpg";
	


	private boolean downloadFailed=false;
	
	public SlideshowPhoto(){
		
	}
	
	public SlideshowPhoto(String title, String description, String thumbnail,
			String smallPhoto, String largePhoto) {
		this.title = title;
		this.description = description;
		this.thumbnail = thumbnail;
		this.smallPhoto = smallPhoto;
		this.largePhoto = largePhoto;
	}
	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * @return the thumbnail
	 */
	public String getThumbnail() {
		return thumbnail;
	}
	/**
	 * @param thumbnail the thumbnail to set
	 */
	public void setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
	}
	/**
	 * @return the smallPhoto
	 */
	public String getSmallPhoto() {
		return smallPhoto;
	}
	/**
	 * @param smallPhoto the smallPhoto to set
	 */
	public void setSmallPhoto(String smallPhoto) {
		this.smallPhoto = smallPhoto;
	}
	/**
	 * @return the largePhoto
	 */
	public String getLargePhoto() {
		return largePhoto;
	}
	/**
	 * @param largePhoto the largePhoto to set
	 */
	public void setLargePhoto(String largePhoto) {
		this.largePhoto = largePhoto;
	}
	
	/*public Drawable getLargePhotoDrawable(File folder){
		long startTime = System.currentTimeMillis(); 
		//Drawable retDrawable=  FileUtils.readBitmapFromFile(new File(folder,getFileName()));
		Drawable retDrawable=  FileUtils.readPurgableBitmapFromFile(new File(folder,getFileName()));
		long endTime = System.currentTimeMillis();
		Log.d(SlideshowActivity.LOG_PREFIX, "File IO used " + (endTime - startTime ) + " ms");
		return retDrawable;
	}*/
	
	public Drawable getLargePhotoDrawable(File folder, int maxWidth, int maxHeight)throws IOException{
		long startTime = System.currentTimeMillis(); 
		//Drawable retDrawable=  FileUtils.readBitmapFromFile(new File(folder,getFileName()));


		Drawable retDrawable=  FileUtils.readPurgableBitmapFromFile(new File(folder,getFileName()), maxWidth,maxHeight);
		long endTime = System.currentTimeMillis();
		Log.d(SlideshowActivity.LOG_PREFIX, "File IO used " + (endTime - startTime ) + " ms");
		return retDrawable;
	}
	
	
	
	
	public boolean isCacheExisting(File folder){
		File photoFile = new File(folder, getFileName());
		if(photoFile.exists()){
			return true;
		}else {
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see com.elsewhat.smugmug.api.DownloadableObject#getFileName()
	 */
	@Override
	public String getFileName() {
		String urlString = getUrlStringForDownload();
		//let's do a MD5 hash and return that
	    try {
	        // Create MD5 Hash
	        MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
	        digest.update(urlString.getBytes());
	        byte messageDigest[] = digest.digest();
	        
	        // Create Hex String
	        StringBuffer hexString = new StringBuffer();
	        for (int i=0; i<messageDigest.length; i++)
	            hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
	        return hexString.toString()+fileType;
	        
	    } catch (NoSuchAlgorithmException e) {
	    	//TODO: This should really  be handled better
	        return "error in filehash";
	    }
	}

	/* (non-Javadoc)
	 * @see com.elsewhat.smugmug.api.DownloadableObject#getUrlStringForDownload()
	 */
	@Override
	public String getUrlStringForDownload() {
		return getLargePhoto();
	}

	/* (non-Javadoc)
	 * @see com.elsewhat.smugmug.api.DownloadableObject#isDownloadFailed()
	 */
	@Override
	public boolean isDownloadFailed() {
		return downloadFailed;
	}

	/* (non-Javadoc)
	 * @see com.elsewhat.smugmug.api.DownloadableObject#setDownloadFailed(java.lang.Throwable, java.lang.String)
	 */
	@Override
	public void setDownloadFailed(Throwable t, String message) {
		downloadFailed=true;		
	}
	
	/**
	 * Indicates that this is a promotional photo, that may be handled differently
	 * @return the isPromotion
	 */
	public boolean isPromotion() {
		return isPromotion;
	}

	/**
	 * @param isPromotion the isPromotion to set
	 */
	public void setPromotion(boolean isPromotion) {
		this.isPromotion = isPromotion;
	}
	
	public String toString(){
		return getTitle()+ " - " + getLargePhoto();
	}
	
}
