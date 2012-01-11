package com.elsewhat.android.slideshow.activities;

import java.io.File;

import com.elsewhat.android.slideshow.R;
import com.elsewhat.android.slideshow.api.Analytics;
import com.elsewhat.android.slideshow.api.AndroidUtils;
import com.elsewhat.android.slideshow.api.DeletablePreference;
import com.elsewhat.android.slideshow.api.ReadOnlyPreference;


import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.widget.Toast;


public class SlideshowPreferences extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	public static final String PREFS_NAME = "SlideshowPreferences";

	public static final String KEY_DO_ANALYTICS = "DoAnalytics";
	public static final String KEY_DISPLAY_TIME = "DisplayTime";
	public static final String KEY_DO_TRANSITION = "DoTransition";
	public static final String KEY_TRANSITION_TYPE = "TransitonType";
	
	public static final String KEY_DISPLAY_TITLE = "DisplayTitle";
	public static final String KEY_DO_DOWNLOAD_ON_3G = "Do3G";
	public static final String KEY_DO_DELETE_CACHE = "DoDeleteCache";
	
	public static final String TRANSITION_TYPE_SLIDE_IN="SLIDE_IN";
	public static final String TRANSITION_TYPE_FADE_IN="FADE_IN";


	public static final boolean DEFAULT_VALUE_DO_ANALYTICS = true;
	public static final boolean DEFAULT_VALUE_DOWNLOAD_ON_3G = false;
	public static final boolean DEFAULT_VALUE_DO_TRANSITION = true;
	public static final boolean DEFAULT_VALUE_DO_TRANSITION_GOOGLETV = false;
	public static final String DEFAULT_VALUE_TRANSITION_TYPE = TRANSITION_TYPE_FADE_IN;
	public static final String DEFAULT_VALUE_TRANSITION_TYPE_GOOGLETV = TRANSITION_TYPE_FADE_IN;
	public static final boolean DEFAULT_VALUE_DISPLAY_TITLE = true;
	public static final int DEFAULT_VALUE_DISPLAY_TIME_INT = 11;
	public static final String DEFAULT_VALUE_DISPLAY_TIME = DEFAULT_VALUE_DISPLAY_TIME_INT+"";
	
	//TODO: Make sure this matches the package name of the app!
	public static final String CACHE_DIRECTORY="/Android/data/com.elsewhat.android.slideshow.premium/files/";

	protected EditTextPreference editDisplayTime;
	protected CheckBoxPreference cbAnalytics,cbDownloadOn3G, cbDoTransition,cbDisplayPhotoTitle;
	protected ListPreference lpTransisitonType;
	protected DeletablePreference dDeleteCache;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setPreferenceScreen(createPreferenceHierarchy());
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);

		setContentView(R.layout.activity_preferences);

	}

	private PreferenceScreen createPreferenceHierarchy() {
		// Root
		PreferenceScreen root = getPreferenceManager().createPreferenceScreen(
				this);
		root.getPreferenceManager().setSharedPreferencesName(PREFS_NAME);

		PreferenceCategory slideshowCat = new PreferenceCategory(this);
		
		/* No need for this since this is the premium version
		ReadOnlyPreference premiumPreference = new ReadOnlyPreference(this);
		premiumPreference.setPersistent(false);
		premiumPreference.setTitle(R.string.pref_premium_title);
		premiumPreference.setSummary(R.string.pref_premium_summary);
		premiumPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				try {
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:com.elsewhat.android.slideshow.premium")));
				}catch (ActivityNotFoundException e) {
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://market.android.com/details?id=com.elsewhat.android.slideshow.premium")));
				}
				return true;
			}
		});
		root.addPreference(premiumPreference);
		*/
		
		slideshowCat.setTitle(R.string.pref_category_slideshow);
		root.addPreference(slideshowCat);
		
		
		
		editDisplayTime = new EditTextPreference(this);
		editDisplayTime.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
		editDisplayTime.getEditText().setKeyListener(DigitsKeyListener
				.getInstance(false, false));
		editDisplayTime.setDefaultValue(DEFAULT_VALUE_DISPLAY_TIME);
		editDisplayTime.setKey(KEY_DISPLAY_TIME);
		editDisplayTime.setTitle(R.string.pref_displaytime_title);
		//editDisplayTime.setSummary(R.string.pref_displaytime_summary);
		editDisplayTime.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				try {
					int number = Integer.parseInt(newValue.toString());
					if (number < 5 || number>120){          				
						notifyUser("Number of seconds must be between 5 and 120");
						return false;
					}
					return true;
				} catch (NumberFormatException e) {
					notifyUser("Please enter a valid number for display time (minimum 5, maximum 120)");
					return false;
				}
			}
		});
		slideshowCat.addPreference(editDisplayTime);
		
		cbDisplayPhotoTitle = new CheckBoxPreference(this);
		cbDisplayPhotoTitle.setKey(KEY_DISPLAY_TITLE);
		cbDisplayPhotoTitle.setDefaultValue(new Boolean(DEFAULT_VALUE_DISPLAY_TITLE));
		cbDisplayPhotoTitle.setTitle(R.string.pref_phototitle_title);
		cbDisplayPhotoTitle.setSummary(R.string.pref_phototitle_summary);
		slideshowCat.addPreference(cbDisplayPhotoTitle);
		
		/*
		cbDoTransition = new CheckBoxPreference(this);
		cbDoTransition.setKey(KEY_DO_TRANSITION);
		if(AndroidUtils.isGoogleTV(getApplicationContext())){
			cbDoTransition.setDefaultValue(new Boolean(DEFAULT_VALUE_DO_TRANSITION_GOOGLETV));
		}else {
			cbDoTransition.setDefaultValue(new Boolean(DEFAULT_VALUE_DO_TRANSITION));
		}
		cbDoTransition.setTitle(R.string.pref_transition_title);
		cbDoTransition.setSummary(R.string.pref_transition_summary);
		slideshowCat.addPreference(cbDoTransition);
		*/

		lpTransisitonType = new ListPreference(this);
		lpTransisitonType.setKey(KEY_TRANSITION_TYPE);
		if(AndroidUtils.isGoogleTV(getApplicationContext())){
			lpTransisitonType.setDefaultValue(DEFAULT_VALUE_TRANSITION_TYPE);
		}else {
			lpTransisitonType.setDefaultValue(DEFAULT_VALUE_TRANSITION_TYPE_GOOGLETV);
		}
		lpTransisitonType.setTitle(R.string.pref_transition_title);
		lpTransisitonType.setDialogTitle(R.string.pref_transition_dialogtitle);
		lpTransisitonType.setEntries(R.array.pref_transitiontypes_text);
		lpTransisitonType.setEntryValues(R.array.pref_transitiontypes_values);
		slideshowCat.addPreference(lpTransisitonType);
		 
		
/*
		editTextUser = new EditTextPreference(this);
		editTextUser.setKey(KEY_REDDIT_USERNAME);
		editTextUser.setDialogTitle(R.string.lblUsernameDialogTitle);
		editTextUser.setTitle(R.string.lblUsernameTitle);
		editTextUser.setSummary(R.string.lblUsernameSummaryBlank);
		userCat.addPreference(editTextUser);
*/
		
		PreferenceCategory otherCat = new PreferenceCategory(this);
		otherCat.setTitle(R.string.pref_category_other);
		root.addPreference(otherCat);

		
		
		cbAnalytics = new CheckBoxPreference(this);
		cbAnalytics.setDefaultValue(new Boolean(DEFAULT_VALUE_DO_ANALYTICS));
		cbAnalytics.setKey(KEY_DO_ANALYTICS);
		cbAnalytics.setTitle(R.string.pref_analytics_title);
		cbAnalytics.setSummary(R.string.pref_analytics_summary);
		otherCat.addPreference(cbAnalytics);
		
		
		//hide this option from non-phone devices such as GoogleTV
		if (AndroidUtils.hasTelephony(this)==true){
			cbDownloadOn3G = new CheckBoxPreference(this);
			cbDownloadOn3G.setDefaultValue(new Boolean(DEFAULT_VALUE_DOWNLOAD_ON_3G));
			cbDownloadOn3G.setKey(KEY_DO_DOWNLOAD_ON_3G);
			cbDownloadOn3G.setTitle(R.string.pref_allow3g_title);
			cbDownloadOn3G.setSummary(R.string.pref_allow3g_summary);
			otherCat.addPreference(cbDownloadOn3G);
		}
		
		DeletablePreference dDeleteCache = new DeletablePreference(this);
		dDeleteCache.setDialogTitle(R.string.pref_deletecache_dialogtitle);
		dDeleteCache.setTitle(R.string.pref_deletecache_title);
		dDeleteCache.setSummary(R.string.pref_deletecache_summary);
		dDeleteCache.setDialogIcon(android.R.drawable.ic_delete);
		dDeleteCache.setPersistent(true);
		dDeleteCache.setKey(KEY_DO_DELETE_CACHE);
		dDeleteCache.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Analytics.trackEvent(getApplicationContext(), "actions", "deletecache", "true");
				String rootPath =  Environment.getExternalStorageDirectory()+ SlideshowPreferences.CACHE_DIRECTORY;
				
				File rootCacheDir= new File (rootPath);
				
			    if (rootCacheDir.isDirectory()) {
			        File[] cachedFiles = rootCacheDir.listFiles();
			        notifyUser("Deleting " + cachedFiles.length + " cached photos from " + rootPath);
			        for (int i=0; i<cachedFiles.length; i++) {
			        	cachedFiles[i].delete();
			        }
			        rootCacheDir.delete();
			    }

			   
				return true;
			}
		});
		otherCat.addPreference(dDeleteCache);
		
		ReadOnlyPreference aboutPreference = new ReadOnlyPreference(this);
		aboutPreference.setPersistent(false);
		aboutPreference.setTitle(R.string.pref_about_title);
		aboutPreference.setSummary(R.string.pref_about_summary);
		aboutPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				try {
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pub:\"Dagfinn Parnas\"")));
				}catch (ActivityNotFoundException e) {
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://market.android.com/developer?pub=Dagfinn+Parnas")));
				}
				
				return true;
			}
		});
		otherCat.addPreference(aboutPreference);

		updateSummaryBasedOnValue();

		return root;

	}

	private void updateSummaryBasedOnValue() {
		if (editDisplayTime.getText() != null) {
			editDisplayTime.setSummary(editDisplayTime.getText() + " seconds");
		} 
		lpTransisitonType.setSummary("Transition " + lpTransisitonType.getEntry());

	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// SharedPreferences settings =
		// getSharedPreferences(RedditTVPreferences.PREFS_NAME, 0);
		// Need to handle conversion due to lack of api support for int in
		// ListPreferences
		// String updatedValue= settings.getString(key,null);
		// Log.d("RedditTV", key + " key was updated in sharedPreferences");
		updateSummaryBasedOnValue();

	}

	public static int getDisplayTimeMS(Context c){
		SharedPreferences settings = c.getSharedPreferences(SlideshowPreferences.PREFS_NAME, MODE_WORLD_READABLE);
		String strDisplayTimeSeconds=settings.getString(SlideshowPreferences.KEY_DISPLAY_TIME, SlideshowPreferences.DEFAULT_VALUE_DISPLAY_TIME);
		try {
			int nrSeconds = Integer.parseInt(strDisplayTimeSeconds);
			return nrSeconds*1000;
			
		}catch (NumberFormatException e) {
			return SlideshowPreferences.DEFAULT_VALUE_DISPLAY_TIME_INT * 1000;
		}
	}
	
	public static boolean doDownloadOn3G(Context c){
		SharedPreferences settings = c.getSharedPreferences(SlideshowPreferences.PREFS_NAME, MODE_WORLD_READABLE);
		return settings.getBoolean(SlideshowPreferences.KEY_DO_DOWNLOAD_ON_3G, SlideshowPreferences.DEFAULT_VALUE_DOWNLOAD_ON_3G);
	}
	
	public static boolean doDisplayPhotoTitle(Context c){
		SharedPreferences settings = c.getSharedPreferences(SlideshowPreferences.PREFS_NAME, MODE_WORLD_READABLE);
		return settings.getBoolean(SlideshowPreferences.KEY_DISPLAY_TITLE, SlideshowPreferences.DEFAULT_VALUE_DISPLAY_TITLE);
	}
	
	
	/**
	 * Find out if we should use a custom transition for the gallery.
	 * If yes, we will use custom animations in the Gallery
	 * 
	 * @param c
	 * @return
	 */
	public static boolean doCustomTransition(Context c){
		SharedPreferences settings = c.getSharedPreferences(SlideshowPreferences.PREFS_NAME, MODE_WORLD_READABLE);
		String defaultValue = DEFAULT_VALUE_TRANSITION_TYPE;
		if(AndroidUtils.isGoogleTV(c)){
			defaultValue=DEFAULT_VALUE_TRANSITION_TYPE_GOOGLETV;
		}
		String transitionType= settings.getString(SlideshowPreferences.KEY_TRANSITION_TYPE, defaultValue);
		
		if(!TRANSITION_TYPE_SLIDE_IN.equals(transitionType)){
			return true;
		}else {
			return false;
		}
	}


	
	
	private void notifyUser(CharSequence msg) {
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}

}