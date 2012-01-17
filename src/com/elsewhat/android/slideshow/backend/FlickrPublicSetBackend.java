package com.elsewhat.android.slideshow.backend;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.content.Context;
import android.util.Log;

import com.elsewhat.android.slideshow.activities.SlideshowActivity;
import com.elsewhat.android.slideshow.api.SlideshowPhoto;

public class FlickrPublicSetBackend {
	//TODO-FORK: Define your own flickr api key (this is a test one)
	protected String flickrAPIKey="f6ccebf5e7a6427fec99952ad91939e2";
	protected String photoset_id;
	
	public FlickrPublicSetBackend(String photoset_id){
		this.photoset_id=photoset_id;
	}
	
	/**
	 * Retrieve the slideshow photos from the remote source
	 * 
	 * 
	 * @param context
	 * @return List of SlideshowPhoto objects
	 */
	public List<SlideshowPhoto> getSlideshowPhotos(Context context) throws Throwable {
		String flickrURL = "http://api.flickr.com/services/rest/?method=flickr.photosets.getPhotos"
				+ "&api_key="+ flickrAPIKey 
				+ "&photoset_id="+ photoset_id
				+ "&per_page=500"
				+ "&media=photos"
				+ "&extras=description,geo,date_taken,tags";	
		Log.i("FlickrPublicFavoritesBackend", "FlickrAPI url "+ flickrURL);
		
		//String feedResponse = null;
		InputStream responseInputStream=null;
		String exceptionMessage="Could not download photos list";
		try {
			//setup the HTTP request
			DefaultHttpClient httpClient = new DefaultHttpClient();
		    HttpGet request = new HttpGet(flickrURL);
            HttpParams httpParameters = httpClient.getParams();
            HttpConnectionParams.setConnectionTimeout(httpParameters, 60 * 1000);
            HttpConnectionParams.setSoTimeout        (httpParameters, 60 * 1000);

			//perform the HTTP request
            HttpResponse response = httpClient.execute(request);
            int responseCode = response.getStatusLine().getStatusCode();
            Log.i(SlideshowActivity.LOG_PREFIX, responseCode  + " response code for download of " + flickrURL  );
            
            if(responseCode==200){
            	responseInputStream=response.getEntity().getContent();
            	//feedResponse = getStringFromInputStream(response.getEntity().getContent());
            }else {
            	String message=responseCode  + " for feed was not 200. Will not write to file";
            	Log.w(SlideshowActivity.LOG_PREFIX,  message );
            	throw new IOException(exceptionMessage);
            }

			
			ArrayList<SlideshowPhoto> alPhotos = new ArrayList<SlideshowPhoto>(50);
			
			InputSource inputSource = new InputSource(responseInputStream);
			//We do DOM parsing as xmls are fairly small
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(inputSource);
			doc.getDocumentElement().normalize();

			NodeList photoElements = doc.getElementsByTagName("photo");

			//loop through photo elements
			for (int i = 0; i < photoElements.getLength(); i++) {
				Node photoNode = photoElements.item(i);
				SlideshowPhoto slideshowPhoto = new SlideshowPhoto();
				//used for calculating the url
				String flickrPhotoId=null;
				String flickrPhotoSecret=null;
				String flickrFarm=null;
				String flickrServer=null;
				
				//for each element check if it has children for the various attributes we are search for
				if (photoNode instanceof Element){
					Element photoElement = (Element)photoNode; 
					//NodeList photoAttributeElements = photoElement.getChildNodes();
					
					
					if (photoElement.hasAttribute("title")){
						slideshowPhoto.setTitle(photoElement.getAttribute("title"));
					}
					if (photoElement.hasAttribute("description")){
						slideshowPhoto.setTitle(photoElement.getAttribute("description"));
					}
					if (photoElement.hasAttribute("id")){
						flickrPhotoId= photoElement.getAttribute("id");
					}
					if (photoElement.hasAttribute("secret")){
						flickrPhotoSecret= photoElement.getAttribute("secret");
					}
					if (photoElement.hasAttribute("server")){
						flickrServer= photoElement.getAttribute("server");
					}
					if (photoElement.hasAttribute("farm")){
						flickrFarm= photoElement.getAttribute("farm");
					}

					
				}
				
				if(flickrPhotoSecret!= null && flickrPhotoId!=null && flickrServer!=null && flickrFarm!=null){
					String photoUrl = "http://farm"+ flickrFarm + ".staticflickr.com/"+flickrServer+"/"+flickrPhotoId+"_"+flickrPhotoSecret+"_b.jpg";
					Log.d(SlideshowActivity.LOG_PREFIX, "Url for photo" + photoUrl);
					slideshowPhoto.setLargePhoto(photoUrl);
				}
					
				if(slideshowPhoto.getLargePhoto()==null){
					Log.w(SlideshowActivity.LOG_PREFIX, "Slideshow photo not parsed correctly from xml. Is missing essential attribute. Slideshowphoto" +slideshowPhoto);
				}else {
					//add photo to list
					alPhotos.add(slideshowPhoto);
				}
			}
			
			if(alPhotos.size()==0){
				return null;
			}else {
				return alPhotos;
			}
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			Log.w(SlideshowActivity.LOG_PREFIX, "MalformedURLException " + e.getMessage(),e);
			throw new MalformedURLException(exceptionMessage);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.w(SlideshowActivity.LOG_PREFIX, "IOException " + e.getMessage(),e);
			throw new IOException(exceptionMessage);
		}
		
		
	}
}
