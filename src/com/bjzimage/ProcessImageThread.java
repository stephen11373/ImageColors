package com.bjzimage;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/*
 * @author Bingjun Zhou
 * Dec 09, 2017 ProcessImageThread.java
 * consumer of the ReadUrlImageThread. It takes UrlImagePair object and then 
 * calculate the most 3 prevalent colors by using a HashMap and sorting.
 */
public class ProcessImageThread implements Callable<Integer> {
    //store BufferedImage, capacity is limited(can be just one). maybe the optional fairness should be considered
    private BlockingQueue<UrlImagePair> imageQueue;
    //the producer - only one producer for one file
    private ArrayList<FutureTask<Integer>> imageReaderFuture;
    //store Url3PrevColorPair objects which represent (url, color1, color 2, color 3)
    private BlockingQueue<Url3PrevColorPair> resultQueue;
    
    //a common variable used in different methods
    private UrlImagePair pair;
    
    public ProcessImageThread(BlockingQueue<UrlImagePair> input, 
    						ArrayList<FutureTask<Integer>> imageReaderFutures,
    						BlockingQueue<Url3PrevColorPair> result) {
    	this.imageQueue = input;
    	this.imageReaderFuture = imageReaderFutures;
    	this.resultQueue = result;
    }

	@Override
	public Integer call() throws InterruptedException  {
		UrlImagePair pair;
		//BufferedImage bufImage = null;
		while (true) {
			boolean pComp = true;
			//get the status of upper layer's thread
			for(Future<Integer> f: imageReaderFuture) {
				pComp = pComp && f.isDone();
			}
			if(pComp) break; //if all related upper layer's threads are done
			
			this.takeAndProcess();
		}
		while(!imageReaderFuture.isEmpty()) {
			this.takeAndProcess();
		}
		return 1;
	}
	
	private void takeAndProcess() throws InterruptedException {
        pair = this.imageQueue.take();
        HashMap<String, Integer> hm = ProcessImageThread.calculateRgbCount(pair.getBufImage());
        pair.setBufImage(null);
        URL url = pair.getUrl();
        pair.setUrl(null);
        pair = null;
        List<Map.Entry<String, Integer>> list = ProcessImageThread.get3PrevalentColor(hm);
        hm.clear(); //clear the map
        hm = null;
        resultQueue.put(new Url3PrevColorPair(url.toString(), list));
	}
	
	/*
	 * It calculates the count of each RGB scheme in a buffered image and store the result in a HashMap
	 * object and return it. The key String is in the form of "R G B" to save memory
	 */
    private static HashMap<String, Integer> calculateRgbCount(BufferedImage image){
    	HashMap<String, Integer> hm = new HashMap<>();
        int w = image.getWidth();
        int h = image.getHeight();
        int rgbPixel;
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
            	rgbPixel = image.getRGB(j, i);
                String rgbStr = convertToRgb(rgbPixel);
                if (! hm.containsKey(rgbStr)) {   //first time
                    hm.put(rgbStr, 1);
                } else {
                    hm.put(rgbStr, hm.get(rgbStr) + 1);
                }
            }
        }
        return hm;

    }
    
    private static String convertToRgb(int pixel) {
    	//int alpha = (pixel >> 24) & 0xff;
    	int red = (pixel >> 16) & 0xff;
    	int green = (pixel >> 8) & 0xff;
    	int blue = (pixel) & 0xff;
    	return red + " " + green + " " + blue;
    }
    
    /*
     * it returns the 3 most prevalent colors. First it sort the input in the descending order. Then it 
     * picks the top 3 ones.
     */
    private static List<Map.Entry<String, Integer>> get3PrevalentColor(HashMap<String, Integer> hm) {
    	/*
    	List<Map.Entry<String, Integer>> list = hm.entrySet().stream().sorted((n1, n2) -> 
    		{ return Integer.compare(n2.getValue(), n1.getValue()); //Descending
    	}).collect(Collectors.toList());
    	*/
    	//ArrayList is better than LinkedList since Collections sort uses Merged Sort
    	ArrayList<Map.Entry<String, Integer>> listView = 
    			Collections.list(Collections.enumeration(hm.entrySet()));
    	//LinkedList listView = new LinkedList(hm.entrySet());
        Collections.sort(listView, new Comparator<Map.Entry<String, Integer>>() {
              public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return Integer.compare(o2.getValue(), o1.getValue()); //descending order
              }
        });    
        List<Map.Entry<String, Integer>> mostPrevalentList = new ArrayList<Map.Entry<String, Integer>>();
        if(listView.size() > 0) mostPrevalentList.add(listView.get(0));
        if(listView.size() > 1) mostPrevalentList.add(listView.get(1));
        if(listView.size() > 2) mostPrevalentList.add(listView.get(2));
        return mostPrevalentList;        
    }    
	
}
