package com.elsewhat.android.slideshow.api;

public interface AsyncQueueableObject {
	/**
	 * 
	 * Perform the operation in a background thread
	 * @return
	 */
	public void performOperation();
	
	/**
	 * Handle the result in the UI thread
	 * 
	 * @param result
	 */
	public void handleOperationResult();


}
