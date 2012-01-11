package com.elsewhat.android.slideshow.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.DialogPreference;
import android.util.AttributeSet;



public class DeletablePreference extends DialogPreference {
	OnPreferenceChangeListener mOnChangeListener;
	
	public DeletablePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}
	
	public DeletablePreference(Context context) {
		super(context, null);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see android.preference.DialogPreference#onDialogClosed(boolean)
	 */
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		if(positiveResult && mOnChangeListener!=null){
			 mOnChangeListener.onPreferenceChange(this, "OK");
			
		}
		
		if(positiveResult &&isPersistent()){
			SharedPreferences preferences =getSharedPreferences();
			Editor editor =preferences.edit();
			//we want a unique value so that it triggers any SharedPreferenceListeners
			editor.putString(getKey(), System.currentTimeMillis()+"");
			editor.commit();
		}
	}

	/* (non-Javadoc)
	 * @see android.preference.Preference#setOnPreferenceChangeListener(android.preference.Preference.OnPreferenceChangeListener)
	 */
	@Override
	public void setOnPreferenceChangeListener(
			OnPreferenceChangeListener onPreferenceChangeListener) {
		mOnChangeListener = onPreferenceChangeListener;
		//super.setOnPreferenceChangeListener(onPreferenceChangeListener);
	}
	
	

}
