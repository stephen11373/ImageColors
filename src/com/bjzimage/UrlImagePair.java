package com.bjzimage;

import java.awt.image.BufferedImage;
import java.net.URL;

/*
 * @author Bingjun Zhou
 * Dec 10, 2017 UrlImagePair.java
 * This is just a class of an association of two entries.
 */
public class UrlImagePair {
	private BufferedImage bufImage;
	private URL url;
	
	public UrlImagePair(URL u, BufferedImage im) {
		this.bufImage = im;
		this.url = u;
	}

	public BufferedImage getBufImage() {
		return bufImage;
	}

	public void setBufImage(BufferedImage bufImage) {
		this.bufImage = bufImage;
	}

	public URL getUrl() {
		return url;
	}

	public void setUrl(URL url) {
		this.url = url;
	}

}
