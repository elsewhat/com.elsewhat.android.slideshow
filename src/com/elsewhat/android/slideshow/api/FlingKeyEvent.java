package com.elsewhat.android.slideshow.api;

import android.view.KeyEvent;

/**
 * Dummy keyevent we use to identify source when we are faking a KeyEvent in onFling of Gallery
 * 
 * @author dagfinn.parnas
 *
 */
public class FlingKeyEvent extends KeyEvent {

	public FlingKeyEvent(int action, int code) {
		super(action, code);
	}

}
