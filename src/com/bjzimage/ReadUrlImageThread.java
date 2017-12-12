package com.bjzimage;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;

/*
 * @author Bingjun Zhou
 * Dec 09, 2017 ReadUrlImageThread.java
 * consumer of the ReadParseUrlThread. It takes URL object and then read the image from the url and 
 * queue the image into another blocking queue, which is the producer of the ImageProcessing Thread
 */
public class ReadUrlImageThread implements Callable<Integer> {
	//store URL object, capacity is limited, maybe the optional fairness should be considered
    private BlockingQueue<URL> urlQueue;
    //store BufferedImage, capacity is limited(can be just one). maybe the optional fairness should be considered
    private BlockingQueue<UrlImagePair> imageQueue;
    //the producer - only one producer for one file. Used for checking done or not
    private Future<Integer> producerFuture;
    
    public ReadUrlImageThread(BlockingQueue<UrlImagePair> input, 
    		                  BlockingQueue<URL> urlInput,
    		                  Future<Integer> producer) {
    	this.imageQueue = input;
    	this.urlQueue = urlInput;
    	this.producerFuture = producer;
    }

	@Override
	public Integer call() throws InterruptedException, javax.imageio.IIOException,java.net.UnknownHostException
	{
		BufferedImage bufImage = null;
		URL url = null;
		while (!this.producerFuture.isDone()) {
			try {
				url = urlQueue.take();
		        bufImage = ImageIO.read(url); //maybe add time out
		        this.imageQueue.put(new UrlImagePair(url, bufImage));
		    } catch (IOException e) { //we need to go to the next URL
		        e.printStackTrace();
		        continue;
		    }
		}
		//the last turn of consuming -- the URL string reader is completed
		while (!urlQueue.isEmpty()) {
			try {
				url = urlQueue.take();
		        bufImage = ImageIO.read(url); //maybe add time out
		        this.imageQueue.put(new UrlImagePair(url, bufImage));
		    } catch (IOException e) { //we need to go to the next URL
		        e.printStackTrace();
		        continue;
		    }
		}
		return 1;
	}
}
