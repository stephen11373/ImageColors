package com.bjzimage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/*
 * @author Bingjun Zhou
 * Dec 10, 2017 CsvWriterThread.java
 * consumer of the ProcessImageThread. It takes result in string form and write them into a CSV file.
 */
public class CsvWriterThread implements  Callable<Integer> {
    //store BufferedImage, capacity is limited(can be just one). maybe the optional fairness should be considered
    private BlockingQueue<Url3PrevColorPair> resultQueue;
    //the producer - only one producer for one file
    private ArrayList<FutureTask<Integer>> imageProcessingFuture;
    //the output csv file path
    private String csvFilePath;
    
    public CsvWriterThread(String csvFilePath,
    						BlockingQueue<Url3PrevColorPair> input, 
    						ArrayList<FutureTask<Integer>> imageProcFutures) {
    	this.csvFilePath = csvFilePath;
    	this.resultQueue = input;
    	this.imageProcessingFuture = imageProcFutures;
    }

	@Override
	public Integer call() throws IOException, InterruptedException {
		File csvFile = new File(csvFilePath);
		if(! csvFile.exists()) {
			csvFile.createNewFile();
		}
		
		int count = 0;
		
    	try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile, false))){
    		String header = "URL,Prev Color 1 (R G B),Prev Color 2 (R G B),Prev Color 3 (R G B)";
    		writer.append(header);
    		writer.append('\n');
    		
    		//loop forever
			while (true) {
				boolean pComp = true;
				//get the status of upper layer's thread
				for(Future<Integer> f: imageProcessingFuture) {
					pComp = pComp && f.isDone();
				}
				if(pComp) break; //if all related upper layer's threads are done
				String result = getImageFromNet(writer);
				count ++;
				System.out.println("Count = " + count + " " + result);
			} //end of While loop
    		//All the producers are complete
			while (!resultQueue.isEmpty()) {
				String result = getImageFromNet(writer);
				count ++;
				System.out.println("Count = " + count + " " + result);
			} //end of While loop
			writer.flush();
    	}
    	return 1;
    }
	
	private String getImageFromNet(BufferedWriter writer) throws IOException, InterruptedException {
		//take the next object
		Url3PrevColorPair pair = resultQueue.take();
		String result = pair.getUrl();
		List<Entry<String, Integer>> prevColors = pair.getPrev3Colors();
		StringBuffer sb = new StringBuffer();
		for(Entry<String, Integer> en : prevColors) {
			sb.append(",");
			sb.append(en.getKey());
		}
		sb.append('\n');
		result = result + sb.toString();
		prevColors.clear();
		prevColors = null;
		writer.append(result);
		return result;
	}
}
