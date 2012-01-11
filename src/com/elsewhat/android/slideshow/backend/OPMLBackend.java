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
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

import android.content.Context;
import android.util.Log;

import com.elsewhat.android.slideshow.activities.SlideshowActivity;
import com.elsewhat.android.slideshow.api.SlideshowBackend;
import com.elsewhat.android.slideshow.api.SlideshowPhoto;

/**
 * Backend specific for StuckInCustoms
 * 
 * 
 * @author dagfinn.parnas
 *
 */
public class OPMLBackend implements SlideshowBackend {
	
	
	public OPMLBackend(){
		
	}
	
	/**
	 * Retrieve the slideshow photos from the remote source
	 * 
	 * 
	 * @param context
	 * @return List of SlideshowPhoto objects
	 */
	public List<SlideshowPhoto> getSlideshowPhotos(Context context) throws Throwable {
		String feedURL = "http://dl.dropbox.com/u/4379928/Slideshow/elsewhat_slideshow.xml";	
		
		//String feedResponse = null;
		InputStream responseInputStream=null;
		String exceptionMessage="Could not download photos list";
		try {
			//setup the HTTP request
			DefaultHttpClient httpClient = new DefaultHttpClient();
		    HttpGet request = new HttpGet(feedURL);
            HttpParams httpParameters = httpClient.getParams();
            HttpConnectionParams.setConnectionTimeout(httpParameters, 60 * 1000);
            HttpConnectionParams.setSoTimeout        (httpParameters, 60 * 1000);

			//perform the HTTP request
            HttpResponse response = httpClient.execute(request);
            int responseCode = response.getStatusLine().getStatusCode();
            Log.i(SlideshowActivity.LOG_PREFIX, responseCode  + " response code for download of " + feedURL  );
            
            if(responseCode==200){
            	responseInputStream=response.getEntity().getContent();
            	//feedResponse = getStringFromInputStream(response.getEntity().getContent());
            }else {
            	String message=responseCode  + " for feed was not 200. Will not write to file";
            	Log.w(SlideshowActivity.LOG_PREFIX,  message );
            	throw new IOException(exceptionMessage);
            }

			
			ArrayList<SlideshowPhoto> alSmugMugPhotos = new ArrayList<SlideshowPhoto>(50);
			
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
				
				//for each element check if it has children for the various attributes we are search for
				if (photoNode.hasChildNodes() && photoNode instanceof Element){
					Element photoElement = (Element)photoNode; 
					NodeList photoAttributeElements = photoElement.getChildNodes();
					
					//promtion elements might contain link to premium version or similar
					if (photoElement.hasAttribute("promotion")){
						slideshowPhoto.setPromotion(true);
					}
					
					for (int j = 0; j < photoAttributeElements.getLength(); j++) {
						Node attributeElement = photoAttributeElements.item(j);
						String attributeName = attributeElement.getNodeName();
						String attributeValue = null;
						if(attributeElement.hasChildNodes()){
							Node childNode = attributeElement.getFirstChild();
							if(childNode instanceof Text){
								attributeValue = ((Text)childNode).getData();
							}
						}
						attributeValue = (attributeValue!=null)?attributeValue.trim():null;
					
						
						if(attributeName!=null && attributeValue!=null){
							//why isn't there switch with strings yet?
							if(attributeName.equalsIgnoreCase("title")){
								slideshowPhoto.setTitle(attributeValue);
							}else if(attributeName.equalsIgnoreCase("description")){
								slideshowPhoto.setDescription(attributeValue);
							}else if(attributeName.equalsIgnoreCase("thumbnail")){
								slideshowPhoto.setThumbnail(attributeValue);
							}else if(attributeName.equalsIgnoreCase("url")){
								slideshowPhoto.setLargePhoto(attributeValue);
							}else if(attributeName.equalsIgnoreCase("small300")){
								slideshowPhoto.setSmallPhoto(attributeValue);
							}/*else {
								Log.w(SlideshowActivity.LOG_PREFIX, "Unknown attribute found in XML " + attributeName + " with value "+ attributeValue);
							}*/
						}
					}
					
					if(slideshowPhoto.getLargePhoto()==null){
						Log.w(SlideshowActivity.LOG_PREFIX, "Slideshow photo not parsed correctly from xml. Is missing essential attribute. Slideshowphoto" +slideshowPhoto);
					}else {
						//add photo to list
						alSmugMugPhotos.add(slideshowPhoto);
					}
					
				}
			}
			
			if(alSmugMugPhotos.size()==0){
				return null;
			}else {
				return alSmugMugPhotos;
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
