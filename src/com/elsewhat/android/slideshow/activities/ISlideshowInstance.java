package com.elsewhat.android.slideshow.activities;

import com.elsewhat.android.slideshow.api.AsyncQueueableObject;

public interface ISlideshowInstance {

	public void actionToggleTitle();
	public void addToAsyncReadQueue(AsyncQueueableObject asyncObject);
	public void setUpScrollingOfDescription();
	public int getScreenWidth();
	public int getScreenHeight();
}
