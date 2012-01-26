package com.elsewhat.android.slideshow.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

public class FileUtils {

	public static BitmapDrawable readBitmapFromFile(File imageFile) {
		BitmapDrawable result = null;
		// Log.d(SlideshowActivity.LOG_PREFIX, "Loading image from file " +
		// imageFile.getAbsolutePath() + " "+ imageFile.getName());
		result = new BitmapDrawable(imageFile.getAbsolutePath());
		/*
		 * try { //FileInputStream fileStream= new FileInputStream(imageFile);
		 * 
		 * //fileStream.close(); }catch (IOException e) {
		 * Log.w(SlideshowActivity.LOG_PREFIX,
		 * "Could not load file from storage " + imageFile.getAbsolutePath() +
		 * " "+ imageFile.getName(),e);
		 * 
		 * }
		 */
		return result;
	}

	public static BitmapDrawable readBitmapFromResources(Resources resources,int resourcesId,int maxWidth, int maxHeight) throws IOException{
		//BitmapDrawable result = null;
		//result = new BitmapDrawable(resources, inputStream);

		// Decode image size
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		InputStream inputStream = resources.openRawResource(resourcesId);
		BitmapFactory.decodeStream(inputStream, null, options);
		inputStream.close();

		
		int scale = 1;
		//choose the longest side as basis for scaling since user may change orientation
		int maxDimension = Math.max(maxWidth, maxHeight);
		
		if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
			scale = (int) Math.pow(2, (int) Math.round(Math.log(maxDimension
					/ (double) Math.max(options.outHeight, options.outWidth))
					/ Math.log(0.5)));
		}

		if(scale!=1){
			Log.d("fileutils", "Loading photo with scale factor "+scale);
		}
		// Decode with inSampleSize
		BitmapFactory.Options options2 = new BitmapFactory.Options();
		options2.inSampleSize = scale;
		options.inPurgeable = true; // Tell to gc that whether it needs free
		// memory, the Bitmap can be cleared
		inputStream = resources.openRawResource(resourcesId);
		Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null,
				options2);

		return new BitmapDrawable(bitmap);
	}
	
	/**
	 * Read a purgable drawable at a sensible scale factor
	 * 
	 * 
	 * @param imageFile
	 * @param maxWidth
	 * @param maxHeight
	 * @return
	 * @throws IOException
	 */
	public static BitmapDrawable readPurgableBitmapFromFile(File imageFile,
			int maxWidth, int maxHeight) throws IOException {

		FileInputStream fileInputStream = null;
		// Decode image size
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;

		fileInputStream = new FileInputStream(imageFile);
		BitmapFactory.decodeStream(fileInputStream, null, options);
		fileInputStream.close();

		int scale = 1;
		//choose the longest side as basis for scaling since user may change orientation
		int maxDimension = Math.max(maxWidth, maxHeight);
		
		if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
			scale = (int) Math.pow(2, (int) Math.round(Math.log(maxDimension
					/ (double) Math.max(options.outHeight, options.outWidth))
					/ Math.log(0.5)));
		}

		if(scale!=1){
			Log.d("fileutils", "Loading photo with scale factor "+scale);
		}
		// Decode with inSampleSize
		BitmapFactory.Options options2 = new BitmapFactory.Options();
		options2.inSampleSize = scale;
		options.inPurgeable = true; // Tell to gc that whether it needs free
		// memory, the Bitmap can be cleared
		fileInputStream = new FileInputStream(imageFile);
		Bitmap bitmap = BitmapFactory.decodeStream(fileInputStream, null,
				options2);

		return new BitmapDrawable(bitmap);

	}

	public static File writeToFile(File dir, String fileName,
			InputStream inputStream) {

		File file = new File(dir, fileName);
		FileOutputStream f;
		try {
			f = new FileOutputStream(file);

			byte[] buffer = new byte[1024];
			int len1 = 0;
			while ((len1 = inputStream.read(buffer)) > 0) {
				f.write(buffer, 0, len1);
			}
			f.close();
			return file;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/*
	 * public static BitmapDrawable readBitmapFromFile2(File imageFile){
	 * BitmapDrawable result = null; Log.d(SlideshowActivity.LOG_PREFIX,
	 * "Loading image from file " + imageFile.getAbsolutePath() + " "+
	 * imageFile.getName()); try { final FileChannel channel = in.getChannel();
	 * final int fileSize = (int)channel.size(); final byte[] testBytes = new
	 * byte[fileSize]; final ByteBuffer buff = ByteBuffer.allocate(fileSize);
	 * final byte[] buffArray = buff.array(); final int buffBase =
	 * buff.arrayOffset(); // Read from channel into buffer, and batch read from
	 * buffer to byte array long time1 = System.currentTimeMillis();
	 * channel.position(0); channel.read(buff); buff.flip();
	 * buff.get(testBytes); buff. new BitmapDrawable }catch (IOException e) {
	 * Log.w(SlideshowActivity.LOG_PREFIX, "Could not load file from storage " +
	 * imageFile.getAbsolutePath() + " "+ imageFile.getName(),e);
	 * 
	 * } return result; }
	 */

}
