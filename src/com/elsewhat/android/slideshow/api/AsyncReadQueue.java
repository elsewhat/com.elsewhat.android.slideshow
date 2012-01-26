package com.elsewhat.android.slideshow.api;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.Iterator;
import java.util.Stack;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Class which implements a Last-In/First-Out queue for operations that require background processing and an update to the UI thread
 * 
 * My usage is to read large photos from the file system
 * 
 * @author dagfinn.parnas
 */
public class AsyncReadQueue<T> {	
	protected static final String LOG_PREFIX="Slideshow PhotoIOQueue";
	//if the tasks can be processed in parallel (for example http), increase this to 2 or more
	protected int numberOfThreads=1;
	//that workers
	protected ArrayList<ReaderTask>  readerTasks;
	
	Context context;
	//stack is synchronized
	Stack<AsyncQueueableObject> queuedObjects;
	AsyncQueueListener listener;

	
	/**
	 * Constructor 
	 * @param context
	 * @param listener The listener which will be notified when a task has completed
	 */
	public AsyncReadQueue(Context context,AsyncQueueListener listener) {
		super();
		this.context = context;
		this.listener=listener;
		
		//lets make sure the List is synchronized
		queuedObjects= new Stack<AsyncQueueableObject>();
	}
	
	/**
	 * Listener which is notified when the read is finished is complete or retrieved error
	 */
	public interface AsyncQueueListener {
		/*These methods should be synchronized when implemented*/
		void onAsyncReadComplete (AsyncQueueableObject queueableObject);
	}
	
	
	
	/**
	 * Add an object to the queue. 
	 * Will trigger the worker tasks to start if they are not running 
	 *  
	 * @param queueObject
	 */
	public void add(AsyncQueueableObject queueObject){
		queuedObjects.push(queueObject);
		
		if(!hasRunningTasks()){
			if(queuedObjects.size()>1){
				Log.d(LOG_PREFIX, "Added new queued object, queue size="+queuedObjects.size()+". New tasks triggered"+ queueObject );
			}
			
			processQueue();
		}else {
			Log.d(LOG_PREFIX, "Added new queued object, queue size="+queuedObjects.size()+". Processed by already running tasks "+ queueObject );
		}

	}
	
	protected void processQueue(){
		//if this is called, we assume there hasRunningTasks()==false has been called first
		//Log.i(LOG_PREFIX, "Starting new reader tasks to process queue" );
		readerTasks= new ArrayList<ReaderTask>(numberOfThreads);
		for (int i = 0; i < numberOfThreads; i++) {
			ReaderTask readerTask = new ReaderTask(i);
			readerTasks.add(readerTask);
			readerTask.execute();	
		}
	}
	
	protected boolean hasRunningTasks(){
		if(readerTasks==null || readerTasks.size()==0){
			return false;
		}
		boolean hasActiveTask= false;
		for (Iterator<ReaderTask> iterator = readerTasks.iterator(); iterator.hasNext();) {
			ReaderTask readerTask = iterator.next();
			if(readerTask.isFinished()==false){
				hasActiveTask=true;
			}
		}
		if(hasActiveTask){
			return true;
		}else {
			return false;
		}
	}
	
	/**
	 * Stops the ongoing tasks. Any queued objects are removed.
	 * To start it again, a new call to add method must be made
	 * 
	 */
	public void stop(){
		for (Iterator<ReaderTask> iterator = readerTasks.iterator(); iterator.hasNext();) {
			ReaderTask readerTask = iterator.next();
			if(readerTask.isFinished()==false){
				Log.i(LOG_PREFIX, "Stopping ongoing async reader task");
				readerTask.cancel(true);
			}
		}
		queuedObjects.clear();
	
	}
	
	
    /**
     * AsyncTask which represent the worker threads 
     * 
     */
    public class ReaderTask extends AsyncTask<Void, AsyncQueueableObject, Void> {
    	boolean hasError=false;


		Throwable throwable;
    	String userErrorMsg;
    	int threadId;
    	boolean isFinished=false;
    	T result;
    	
    	public ReaderTask(int threadId){
    		this.threadId=threadId;
    		isFinished=false;
    	}
    	
		@Override
		protected Void doInBackground(Void... arg0) {
			//loop which continues until the queue/stack is empty
			while(!queuedObjects.isEmpty()){
				if(isCancelled()){
					Log.d(LOG_PREFIX,"Async task was cancelled");
					return null;
				}
				AsyncQueueableObject asyncObject=null; 
				try {
					asyncObject= queuedObjects.pop();
				}catch (EmptyStackException e) {
					//can happen if we have more than one running ReaderTask
					return null;
				}
				if(asyncObject!=null){
					//process the async operation
					//this is the time consuming task
					asyncObject.performOperation();
					
					//publish progress will cause the handleOperationResult to be run on the UI thread
					publishProgress(asyncObject);				
				}
	
			}
			isFinished=true;
			return null;
		}
		
		
		/** 
		 * Called when one of the queue objects have performmed the operation
		 * 
		 * @see android.os.AsyncTask#onProgressUpdate(Progress[])
		 */
		@Override
		protected void onProgressUpdate(AsyncQueueableObject... asyncObjects) {
			if(asyncObjects.length==1){
				AsyncQueueableObject asyncObject = asyncObjects[0];
				asyncObject.handleOperationResult();
				listener.onAsyncReadComplete(asyncObject);		
			}else {
				Log.w(LOG_PREFIX, "Unexpected number of DownloadableObject in onProgressUpdate:"+asyncObjects.length );
			}
		}

		protected boolean isFinished(){
			return isFinished;
		}

		@Override
		protected void onPostExecute(Void result) {
			//isFinished=true;
			//Log.i(LOG_PREFIX, "Async reader task " + threadId + " completed" );
			
			//handle a case where a new queued object has been added at the same time we are finished this task
			if(queuedObjects!=null && queuedObjects.size()>0 && hasRunningTasks()==false){
				//Log.w(LOG_PREFIX, "Queue not empty at end of run. Restarting async reader tasks"  );
				processQueue();
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
