package com.elsewhat.android.slideshow.backend;

import java.util.List;

import com.elsewhat.android.slideshow.api.SlideshowPhoto;

import android.content.Context;



/**
 * NOT FUNCTIONAL
 * 
 * A start for a backend bacsed on smug mug
 * 
 * @author dagfinn.parnas
 *
 */
public class SmugMugRecentBackend {
	String smugMugUser;
	public SmugMugRecentBackend(String smugMugUser){
		this.smugMugUser=smugMugUser;
	}
	/**
	 * Method for retrieving photos from Smug mug api
	 * NOT IN USE
	 * 
	 * @param context
	 * @param nickname
	 * @return
	 */
	public List<SlideshowPhoto> getSlideshowPhotos(Context context) throws Throwable {
		//String feedURL = "http://api.smugmug.com/hack/feed.mg?Type=nicknameRecent&Data="
		//		+ smugMugUser + "&format=atom03&Size=X2Large";

		/* TODO: Implement
		try {
			String feedResponse = Util.getResponseFromUrl(context, feedURL);
			Pattern regexPattern = Pattern.compile("<id>(.*?)</id>");
			Matcher regexMatcher = regexPattern.matcher(feedResponse);
			ArrayList<String> alPhotoUrls = new ArrayList<String>(50);
			while(regexMatcher.find()){
				//String fullString = regexMatcher.group(0);
				String url =regexMatcher.group(1);
				
				if(!url.endsWith("/")){
					alPhotoUrls.add(url);
				}
				
			}
			
			if(alPhotoUrls.size()==0){
				return null;
			}else {
				return alPhotoUrls.toArray(new String[alPhotoUrls.size()]);
			}
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}*/
		return null;
		
		
	}
	
	
}
