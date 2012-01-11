package com.elsewhat.android.slideshow.api;

import java.io.File;

import android.content.Context;

/**
 * Representing photos that are cached for which we only have the filename
 * This will only occur when the photos list has failed download
 * 
 * 
 * @author dagfinn.parnas
 *
 */
public class SlideshowPhotoCached extends SlideshowPhoto {
	protected int largePhotoDrawableId;
	protected Context context;
	protected String fileName;
	
	public SlideshowPhotoCached(Context context,File file){
		super("","",null,null,"dummy url");
		this.context=context;
		fileName=file.getName();
	}

	@Override
	public String getFileName() {
		return fileName;
	}
	
}
