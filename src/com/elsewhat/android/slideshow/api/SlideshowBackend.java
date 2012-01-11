package com.elsewhat.android.slideshow.api;

import java.util.List;

import android.content.Context;

public interface SlideshowBackend {
	public List<SlideshowPhoto> getSlideshowPhotos(Context context) throws Throwable;
}
