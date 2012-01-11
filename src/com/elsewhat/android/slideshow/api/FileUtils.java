package com.elsewhat.android.slideshow.api;

import java.io.File;
import java.io.InputStream;

import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;

public class FileUtils {

	public static BitmapDrawable readBitmapFromFile(File imageFile){
		BitmapDrawable result = null;
		//Log.d(SlideshowActivity.LOG_PREFIX, "Loading image from file " + imageFile.getAbsolutePath() + " "+ imageFile.getName());
		result= new BitmapDrawable(imageFile.getAbsolutePath());
		/*try {
			//FileInputStream fileStream= new FileInputStream(imageFile);
			
			//fileStream.close();
		}catch (IOException e) {
			Log.w(SlideshowActivity.LOG_PREFIX, "Could not load file from storage " + imageFile.getAbsolutePath() + " "+ imageFile.getName(),e);
			
		}*/
		return result;
	}
	
	public static BitmapDrawable readBitmapFromInputStream(Resources resources,InputStream inputStream){
		BitmapDrawable result = null;
		result= new BitmapDrawable(resources,inputStream);

		return result;
	}	

	
	/*
	public static BitmapDrawable readBitmapFromFile2(File imageFile){
		BitmapDrawable result = null;
		Log.d(SlideshowActivity.LOG_PREFIX, "Loading image from file " + imageFile.getAbsolutePath() + " "+ imageFile.getName());
		try {
		     final FileChannel channel = in.getChannel(); 
		     final int fileSize = (int)channel.size(); 
		     final byte[] testBytes = new byte[fileSize]; 
		     final ByteBuffer buff = ByteBuffer.allocate(fileSize); 
		     final byte[] buffArray = buff.array(); 
		     final int buffBase = buff.arrayOffset(); 
		     // Read from channel into buffer, and batch read from buffer to  byte array 
		     long time1 = System.currentTimeMillis(); 
		     channel.position(0); 
		     channel.read(buff); 
		     buff.flip(); 
		     buff.get(testBytes); 
			 buff.
			 new BitmapDrawable
		}catch (IOException e) {
			Log.w(SlideshowActivity.LOG_PREFIX, "Could not load file from storage " + imageFile.getAbsolutePath() + " "+ imageFile.getName(),e);
			
		}
		return result;
	}*/

	
}
