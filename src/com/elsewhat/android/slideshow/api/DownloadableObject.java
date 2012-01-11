package com.elsewhat.android.slideshow.api;

public interface DownloadableObject {
	public String getUrlStringForDownload();
	public String getFileName();
	public void setDownloadFailed(Throwable t, String message);
	public boolean isDownloadFailed();
	
}
