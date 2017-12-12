/**
 * 
 */
package com.bjzimage;

import java.util.List;
import java.util.Map.Entry;

/**
 * @author hadoop
 * Dec 10, 2017 Url3PrevColorPair.java
 * This is just a class of url and its 3 most prevalent color strings
 */
public class Url3PrevColorPair {

	private String url;
	private List<Entry<String, Integer>> prev3Colors;
	
	public Url3PrevColorPair(String ul, List<Entry<String, Integer>> colors) {
		this.url = ul;
		this.prev3Colors = colors;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public List<Entry<String, Integer>> getPrev3Colors() {
		return prev3Colors;
	}

	public void setPrev3Colors(List<Entry<String, Integer>> prev3Colors) {
		this.prev3Colors = prev3Colors;
	}

}
