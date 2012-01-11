package com.elsewhat.android.slideshow.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Class which is used to download a queue of DownloadableObjects to the file system
 * 
 * 
 * @author dagfinn.parnas
 *
 */
public class FileDownloader {
	protected static final String LOG_PREFIX="Slideshow FileDownloader";
	protected int numberOfThreads=2;
	protected static final int connectionTimeOutSec=20;
	protected static final int socketTimeoutSec=20;
	protected ArrayList<FileDownloaderTask>  downloaderTasks;
	Context context;
	File rootDirectory;
	//this list must be synchronized! See constructor
	List<DownloadableObject> downloadableObjects;
	FileDownloaderListener listener;
	
	private ArrayList<String> arDownloadSize;
	
	public FileDownloader(Context context,FileDownloaderListener listener, File rootDirectory,
			List<DownloadableObject> downloadableObjects) {
		super();
		this.context = context;
		this.listener=listener;
		this.rootDirectory = rootDirectory;
		
		//lets make sure the List is synchronized
		this.downloadableObjects = Collections.synchronizedList(downloadableObjects);
		arDownloadSize=new ArrayList<String>(300);
	}
	
	/**
	 * Listener which is notified when the download is complete or retrieved error
	 */
	public interface FileDownloaderListener {
		/*These methods should be synchronized when implemented*/
		void onDownloadCompleted (DownloadableObject downloadableObject);
		void onAllDownloadsCompleted ();
		void onAllDownloadsFailed (String message);
		void onDownloadError (DownloadableObject downloadableObject);
	}
	
	/**
	 * Start the download of the files to the file system
	 * 
	 */
	public void execute(){
		if(!rootDirectory.exists()){
			boolean result =rootDirectory.mkdirs();
			if(result==false){
				String userErrorMsg="Unable to download photos\nFailed to create directory at " +rootDirectory.getAbsolutePath();
				listener.onAllDownloadsFailed(userErrorMsg);
				Log.i("FileDownloader", userErrorMsg);
				return;
			}
		}
		
		downloaderTasks= new ArrayList<FileDownloaderTask>(numberOfThreads);
		for (int i = 0; i < numberOfThreads; i++) {
			FileDownloaderTask downloaderTask = new FileDownloaderTask(i);
			downloaderTasks.add(downloaderTask);
			//start downloading untill the list is empty
			downloaderTask.execute();	
		}
	}
	
	/**
	 * Stops the ongoing downloads.
	 * If execute() is called after stop() is called it will continue where it was stopped
	 * 
	 */
	public void stop(){
		
		
		for (Iterator<FileDownloaderTask> iterator = downloaderTasks.iterator(); iterator.hasNext();) {
			FileDownloaderTask downloaderTask = iterator.next();
			if(downloaderTask.isFinished()==false){
				Log.i(LOG_PREFIX, "Stopping ongoing download task");
				downloaderTask.cancel(true);
			}
		}
		
	
	}
	
	/**
	 * If there are remaining downloads (used if it is temporarily stopped)
	 * 
	 * @return
	 */
	public boolean hasRemainingDownloads(){
		if(downloadableObjects==null || downloadableObjects.size()==0){
			return false;
		}else {
			return true;
		}
	}
	
	
    /**
     * Asynch task representing a thread which downloads 
     * 
     */
    public class FileDownloaderTask extends AsyncTask<Void, DownloadableObject, Void> {
    	boolean hasError=false;


		Throwable throwable;
    	String userErrorMsg;
    	int threadId;
    	boolean isFinished=false;
    	
    	public FileDownloaderTask(int threadId){
    		this.threadId=threadId;
    	}
    	
		@Override
		protected Void doInBackground(Void... arg0) {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			
			

			while(!downloadableObjects.isEmpty()){
				if(isCancelled()){
					Log.w(LOG_PREFIX,"Async task was cancelled");
					return null;
				}
				try {
					DownloadableObject downloadableObject = downloadableObjects.remove(0);
					String fileUrl = downloadableObject.getUrlStringForDownload();
					String fileName = downloadableObject.getFileName();
		            
					//setup the HTTP request
				    HttpGet request = new HttpGet(fileUrl);
		            HttpParams httpParameters = httpClient.getParams();
		            HttpConnectionParams.setConnectionTimeout(httpParameters, connectionTimeOutSec * 1000);
		            HttpConnectionParams.setSoTimeout        (httpParameters, socketTimeoutSec * 1000);

					try {
						//perform the HTTP request
			            HttpResponse response = httpClient.execute(request);
			            int responseCode = response.getStatusLine().getStatusCode();
			            //Log.i(LOG_PREFIX, responseCode  + " response code for download of " + fileUrl  );
			            
			            if(responseCode!=200){
			            	String message=responseCode  + " was not 200. Will not write to file";
			            	Log.w(LOG_PREFIX,  message );
			            	downloadableObject.setDownloadFailed(null, message);
			            	
			            }else {
				            InputStream is = response.getEntity().getContent();
							
				          //write to file
				            File file = new File(rootDirectory,fileName);
							FileOutputStream f= new FileOutputStream(file);
				            
				            byte[] buffer = new byte[1024];
				            int len1 = 0;
				            while ((len1 = is.read(buffer)) > 0) {                          
				                f.write(buffer, 0, len1);               
				            }       
				            f.close();
				            
				            arDownloadSize.add(file.length()/(1024L) + "KB "+ fileUrl);
				            Log.d(LOG_PREFIX, file.length()/(1024L) + "KB "+ fileUrl );
				            Log.i(LOG_PREFIX, file.getPath()+ " " +file.getName() + " has been written to the file system" );
				            
				            //this object is now finished, alert the listener
				            publishProgress(downloadableObject);
			            }

			            
					} catch (FileNotFoundException e) {
						downloadableObject.setDownloadFailed(e, "File " + fileName+ " could not be found. Root folder "+rootDirectory );
						publishProgress(downloadableObject);
						Log.w(LOG_PREFIX, e);
					} catch (IOException e) {
						downloadableObject.setDownloadFailed(e, "File " + fileName+ " failed to download due to IOException ");
						publishProgress(downloadableObject);
						Log.w(LOG_PREFIX, e);
					}
	
				}catch (IndexOutOfBoundsException e) {
					Log.w(LOG_PREFIX, " Got IndexOutOfBoundsException most likely due to synchronization issues");
				}
					
				
			}
			
			
			return null;
		}
		
		
		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onProgressUpdate(Progress[])
		 */
		@Override
		protected void onProgressUpdate(DownloadableObject... values) {
			if(values.length==1){
				DownloadableObject downloadedObject = values[0];
				if(downloadedObject.isDownloadFailed()){
					listener.onDownloadError(downloadedObject);
				}else {
					listener.onDownloadCompleted(downloadedObject);
				}
				
			}else {
				Log.w(LOG_PREFIX, "Unexpected number of DownloadableObject in onProgressUpdate:"+values.length );
			}

			super.onProgressUpdate(values);
		}

		protected boolean isFinished(){
			return isFinished;
		}

		@Override
		protected void onPostExecute(Void result) {
			isFinished=true;
		
			//TEMP
			Log.w(LOG_PREFIX, "Printing out stats for the " + arDownloadSize.size() + " downloads");
			for (Iterator<String> iterator = arDownloadSize.iterator(); iterator.hasNext();) {
				Log.w(LOG_PREFIX, iterator.next());
			}
			//TEMP
			
			if(threadId==0){//only report this from the first thread
				listener.onAllDownloadsCompleted();
			}
			
		}
		
    	/* (non-Javadoc)
		 * @see android.os.AsyncTask#onCancelled()
		 */
		@Override
		protected void onCancelled() {
			Log.i(LOG_PREFIX, "Download task stopped");
			super.onCancelled();
		}
    }
	
	
	
	

	
}
