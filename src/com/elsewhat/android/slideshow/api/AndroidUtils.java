package com.elsewhat.android.slideshow.api;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class AndroidUtils {
	private static int sdkVersion;
	 static 
	 {
	    try {
	      sdkVersion = Integer.parseInt(android.os.Build.VERSION.SDK);
	    } catch (Exception ex) {
	    }
	  }

	  /** Device support the froyo (Android 2.2) APIs */
	  public static boolean isAndroid22() {
	    return sdkVersion >= 8;
	  }

	  /** Device supports the Honeycomb (Android 3.0) APIs */
	  public static boolean isAndroid30() {
	    return sdkVersion >= 11;
	  }
	  
	  /**
	   * Test if this device is a Google TV.
	   * 
	   * See 32:00 in "Google I/O 2011: Building Android Apps for Google TV"
	   * http://www.youtube.com/watch?v=CxLL-sR6XfM
	   * 
	   * @return true if google tv
	   */
	  public static boolean isGoogleTV(Context context) {
	      final PackageManager pm = context.getPackageManager();
	      return pm.hasSystemFeature("com.google.android.tv");
	  }
	  
	  public static boolean hasTelephony(Context context){
		  PackageManager pm = context.getPackageManager();
		  return pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
	  }
	  
	  public static boolean isConnectedToWifi(Context context){
		  ConnectivityManager connec = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		  NetworkInfo wifi = connec.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		   
		  if (wifi!=null && wifi.isConnected()) {
		        return true;
		  } else {
			  return false;
		  }
	
	  }
	  
	  public static boolean isConnectedToWired(Context context){
		  ConnectivityManager connec = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		  //ConnectivityManager.TYPE_ETHERNET == 9 , but only from API 13
		  
		  if(sdkVersion<13){
			  return false;
		  }
		  NetworkInfo ethernet = connec.getNetworkInfo(9);
		   
		  if (ethernet!=null && ethernet.isConnected()) {
		        return true;
		  } else {
			  return false;
		  }
	
	  }
	  
	  public static boolean isConnectedRoaming(Context context){
		  ConnectivityManager connec = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		  NetworkInfo mobile = connec.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		   
		  if (mobile!=null && mobile.isConnected() && mobile.isRoaming()){
		        return true;
		  } else {
			  return false;
		  }
	  }
	  
	  
}
