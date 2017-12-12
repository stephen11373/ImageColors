package com.bjzimage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

/*
 * @author Bingjun Zhou
 * Dec 09, 2017 ReadParseUrlThread.java
 * producer of URLs. The class reads a url file one line by one line and then parse the line, and then
 * queue the created java.net.URL object into a blocking queue.
 */
public class ReadParseUrlThread implements Callable<Integer> {
	
	private String urlFilePath;
	//store URL object, capacity is limited, maybe the optional fairness should be considered
    private BlockingQueue<URL> urlQueue;
    
    public ReadParseUrlThread(BlockingQueue<URL> queue, String urlFile) {
	        this.urlQueue = queue;
	        this.urlFilePath = urlFile;
    }

    @Override
	public Integer call() throws IOException, InterruptedException {
		File urlFile = new File(urlFilePath);
		if(! urlFile.exists()) {
			throw new InterruptedException();
		}
    	/*
    	 * We use BufferedReader to read one line at a time until the BlockingQueue is at its capacity to reduce
    	 * memory usage
    	 */
    	try (BufferedReader br = new BufferedReader(new FileReader(urlFile))) {
    		String lineStr;
    		URL url;
    		//Read File Line By Line
    		while ((lineStr = br.readLine()) != null){
    			try {
    				//parse the string
    				url = new URL(lineStr);
    			}catch(MalformedURLException innerE) {
    				continue; //skip the line
    			}
    			//if full, wait
    			urlQueue.put(url);
    		}
    		return 1;
    	}
    }
}
