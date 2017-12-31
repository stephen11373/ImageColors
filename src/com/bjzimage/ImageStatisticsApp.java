package com.bjzimage;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
//import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/*
 * @author Bingjun Zhou
 * Dec 11, 2017 ImageStatisticsApp.java
 * This class is the entry point of the whole application. Usage:
 * 		ArrayList<String> urlInputFiles = new ArrayList<>();  //stores all url input file paths
 *		ArrayList<String> csvOutputFiles = new ArrayList<>(); //stores all csv output file paths
 * Usually the input and output are of one-one relationship. It can be changed with slightly modification but
 * relative rules have to be defined (mapping = ?). 
 * The following variables can be adjusted according to the system resources and networking speed.
 * 		//the capacity of the blocking queue between URL file Reader and Image fetches
 *		int capacityOfUrlBlockingQueue = 5; 
 *		
 *		//the capacity of the blocking queue between Image fetches and Image Process Tasks
 *		int capacityOfImageBlockingQueue = 8;
 *		
 *		//the capacity of the blocking queue between Image Process tasks and CSV writers
 *		int capacityOfResultBlockingQueu = 8;
 *		
 *		//the number of Image Fetch Threads
 *		int numberOfImageReaders = 2;
 *		
 *		//the number of Image Process Threads
 *		int numberOfImageProcessers = 3;
 *  Command Line:
 *  java -cp [...] com.bjzimage.ImageStatisticsApp urlInputFilePath csvOutputFilePath
 */
public class ImageStatisticsApp{
	
	//executor service thread pool for the reading url files
	private ExecutorService executorServReadUrl = null;
	//URL objects queue
	private BlockingQueue<URL> urlQueue = null;
	private FutureTask<Integer> futReaderUrl = null;
	
	//executor service thread pool for the reading images
	private ExecutorService executorServDwnLdImage = null;
	//the blocking queue between Image Fetch and Image Processors
	private BlockingQueue<UrlImagePair> imageQueue = null;
	//image readers accross network
	private ArrayList<FutureTask<Integer>> imageReaderFutures = null;
	
	//executor service thread pool for the processing images
	private ExecutorService executorServProcImage = null;
	//the blocking queue between image processors and CSV writer
	private BlockingQueue<Url3PrevColorPair> resultQueue = null;
	//image process threads
	private ArrayList<FutureTask<Integer>> imageProcFutures = null;
	
	//executor service thread pool for the writing csv files
	private ExecutorService executorServCsvWriter = null;
	//csv writer threads
	private FutureTask<Integer> futureWriteCsv = null;
	
	
	public ImageStatisticsApp() {
	}
	
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		//one input URL file, can be changed to more than one
		//String urlfilePath = "/home/hadoop/eclipse-workspace/BjZhPayImage/src/com/bjzimage/urls-PEX.txt";
		//one output file, can be changed to more than one
		//String csvFilePath = "/home/hadoop/prevalentColors.csv";
		ArrayList<String> urlInputFiles = new ArrayList<>();  //all url input file paths
		ArrayList<String> csvOutputFiles = new ArrayList<>(); //all csv output file paths
		urlInputFiles.add(args[0]);
		csvOutputFiles.add(args[1]);
		System.out.println("The input URL file = " + args[0]);
		System.out.println("The output CSV file = " + args[1]);
		
		//the capacity of the blocking queue between URL file Reader and Image fetches
		int capacityOfUrlBlockingQueue = 30; 
		
		//the capacity of the blocking queue between Image fetches and Image Process Tasks
		int capacityOfImageBlockingQueue = 40;
		
		//the capacity of the blocking queue between Image Process tasks and CSV writers
		int capacityOfResultBlockingQueu = 40;
		
		//the number of Image Fetch Threads
		int numberOfImageReaders = 2;
		
		//the number of Image Process Threads
		int numberOfImageProcessers = 2;
		
		//one url input file - one csv output file. It can be changed with slight modification
		for(int i = 0; i < urlInputFiles.size(); i++) {
			ImageStatisticsApp execObj = new ImageStatisticsApp();
			execObj.execute(urlInputFiles.get(i), csvOutputFiles.get(i),
							capacityOfUrlBlockingQueue,
							capacityOfImageBlockingQueue,
							capacityOfResultBlockingQueu,
							numberOfImageReaders,
							numberOfImageProcessers);
		}
	}

	public void execute(String urlfilePath, 
						String csvFilePath,
						int capacityOfUrlBlockingQueue,
						int capacityOfImageBlockingQueue,
						int capacityOfResultBlockingQueu,
						int numberOfImageReaders,
						int numberOfImageProcessers) throws InterruptedException, ExecutionException {
		try {
			//one url file one read thread
			//capacityOfUrlBlockingQueue = capacity of the Blocking Queue, changeable
			populateURLReadersAndExec(urlfilePath, capacityOfUrlBlockingQueue);
			
			//numberOfImageReaders threads for image fetching for each url file, 
			//capacityOfImageBlockingQueue = capacity of the Blocking Queue, changeable.
			populateImageReadersAndExec(numberOfImageReaders, capacityOfImageBlockingQueue); 
			//we have two threads putting and three threads taking
			
			//The image processing is a heavy calculation task so we use Thread Pool of 3, 
			//depending the system resources
			//numberOfImageProcessers threads totally for image processing 
			//capacityOfResultBlockingQueu = capacity of the Blocking Queue, changeable.
			populateImageProcessesAndExec(numberOfImageProcessers, capacityOfResultBlockingQueu);
			//we have 3 threads putting and 1 threading taking
			
			//populate the CSV Write thread pool
			//The number of threads is usually the same as ReadParseUrlThread pool
			//depending the system resources
			populateCsvWritersAndExec(csvFilePath);
			
			//wait until the read URL thread is done
			//Integer val = futReaderUrl.get();
			//System.out.println("Integer value from the URL reading thread = " + val);
			nicelyShutDown(capacityOfUrlBlockingQueue, capacityOfImageBlockingQueue, capacityOfResultBlockingQueu);
		} catch (Exception ex) {
			ex.printStackTrace();
			immediatelyShutDown();
		}
	}
	
	private void nicelyShutDown(int capacityOfUrlBlockingQueue,
								int capacityOfImageBlockingQueue,
								int capacityOfResultBlockingQueue) throws InterruptedException, ExecutionException {
		//wait until the read URL thread is done
		if(futReaderUrl != null) {
			Integer val = futReaderUrl.get();
			System.out.println("Integer value from the URL reading thread = " + val);
			futReaderUrl.cancel(true);
			executorServReadUrl.shutdown();
			executorServReadUrl.awaitTermination(20, TimeUnit.SECONDS);
		}
		if(urlQueue != null) {
			while(!urlQueue.isEmpty()) {
				Thread.sleep(capacityOfUrlBlockingQueue*500); //sleep for several seconds
			}
		}
		Thread.sleep(20000); //sleep for 20 seconds
		if(imageReaderFutures != null) {
			for(FutureTask<Integer> task: imageReaderFutures) {
				task.cancel(true); //interrupt it if waiting
			}
		}
		if(executorServDwnLdImage != null) {
			executorServDwnLdImage.shutdown();
			executorServDwnLdImage.awaitTermination(40, TimeUnit.SECONDS);
		}
		while(! imageQueue.isEmpty()) {
			Thread.sleep(capacityOfImageBlockingQueue*700); //sleep for several seconds
		}
		Thread.sleep(20000); //sleep for 10 seconds
		if(imageProcFutures != null) {
			for(FutureTask<Integer> task: imageProcFutures) {
				task.cancel(true); //interrupt it if waiting
			}
		}
		if(executorServProcImage != null) {
			executorServProcImage.shutdown();
			executorServProcImage.awaitTermination(30, TimeUnit.SECONDS);
		}
		while(! resultQueue.isEmpty()) {
			Thread.sleep(capacityOfResultBlockingQueue*500); //sleep for several seconds
		}
		Thread.sleep(10000); //sleep for 10 seconds
		if(futureWriteCsv != null)
				futureWriteCsv.cancel(true);  //interrupt it if waiting
		if(executorServCsvWriter != null) {
			executorServCsvWriter.shutdown();
			executorServCsvWriter.awaitTermination(20, TimeUnit.SECONDS);
		}
	}
	
	/*
	 * It immediately shutdown all thread pools, It is intended to be called in case of any unrecoverable error.
	 */
	private void immediatelyShutDown() throws InterruptedException, ExecutionException {
		//wait until the read URL thread is done
		if(futReaderUrl != null)
			futReaderUrl.cancel(true);
		if(executorServReadUrl != null) {
			executorServReadUrl.shutdown();
			executorServReadUrl.awaitTermination(20, TimeUnit.SECONDS);
		}
		//Thread.sleep(capacityOfUrlBlockingQueue*1000); //sleep for 30 seconds
		if(imageReaderFutures != null) {
			for(FutureTask<Integer> task: imageReaderFutures) {
				task.cancel(true); //interrupt it if waiting
			}
		}
		if(executorServDwnLdImage != null) {
			executorServDwnLdImage.shutdown();
			executorServDwnLdImage.awaitTermination(40, TimeUnit.SECONDS);
		}
		//Thread.sleep(capacityOfImageBlockingQueue*3000); //sleep for one minute
		if(imageProcFutures != null) {
			for(FutureTask<Integer> task: imageProcFutures) {
				task.cancel(true); //interrupt it if waiting
			}
		}
		if(executorServProcImage != null) {
			executorServProcImage.shutdown();
			executorServProcImage.awaitTermination(30, TimeUnit.SECONDS);
		}
		//Thread.sleep(capacityOfResultBlockingQueue*3000); //sleep for 30 seconds
		if(futureWriteCsv != null)
			futureWriteCsv.cancel(true);  //interrupt it if waiting
		if(executorServCsvWriter != null) {
			executorServCsvWriter.shutdown();
			executorServCsvWriter.awaitTermination(20, TimeUnit.SECONDS);
		}
	}
	
	/*
	 * populate the URL reader thread pool. The Thread Pool will be 1.
	 * Usually the number of threads equals to the number of url input file.
	 */
	private void populateURLReadersAndExec(String urlFile, int capacityOfBlockingQueue) {
		executorServReadUrl = Executors.newFixedThreadPool(1);
		urlQueue = new ArrayBlockingQueue<>(capacityOfBlockingQueue);
		futReaderUrl = new FutureTask<Integer>(new ReadParseUrlThread(urlQueue, urlFile));
		executorServReadUrl.execute(futReaderUrl);
	}
	
	/*
	 * 	populate the BufferedImage reader thread pool. 
	 */
	private void populateImageReadersAndExec(int numOfThreads, int capacityOfBlockingQueue) {
		executorServDwnLdImage = Executors.newFixedThreadPool(numOfThreads); 
		imageReaderFutures = new ArrayList<FutureTask<Integer>>();
		imageQueue = new ArrayBlockingQueue<UrlImagePair> (capacityOfBlockingQueue);
		for(int i = 0; i < numOfThreads; i++) {
			FutureTask<Integer> futureReadImage = new FutureTask<>(new ReadUrlImageThread(imageQueue, urlQueue, futReaderUrl));
			imageReaderFutures.add(futureReadImage);
		}
		for(FutureTask<Integer> thread: imageReaderFutures) {
			executorServDwnLdImage.execute(thread);
		}
	}
	
	/*
	 * 	populate the Image processing thread pool. 
	 */
	private void populateImageProcessesAndExec(int numOfThreads, int capacityOfBlockingQueue) {
		resultQueue = new ArrayBlockingQueue<Url3PrevColorPair>(capacityOfBlockingQueue);
		//populate the Process Image thread pool
		executorServProcImage = Executors.newFixedThreadPool(numOfThreads); 
		imageProcFutures = new ArrayList<FutureTask<Integer>> ();
		for(int i = 0; i < numOfThreads; i++) {
			FutureTask<Integer> futureProcImage = new FutureTask<>(new ProcessImageThread(imageQueue, imageReaderFutures, resultQueue));
			imageProcFutures.add(futureProcImage);
		}
		for(FutureTask<Integer> thread: imageProcFutures) {
			executorServProcImage.execute(thread);
		}
	}
	
	/*
	 * 	populate he CSV Write thread pool.
	 *  The number of threads is usually the same as ReadParseUrlThread pool = 1
	 */
	private void populateCsvWritersAndExec(String csvFilePath) {
		executorServCsvWriter = Executors.newFixedThreadPool(1); 
		futureWriteCsv = new FutureTask<Integer>(new CsvWriterThread(csvFilePath, resultQueue, imageProcFutures));
		executorServCsvWriter.execute(futureWriteCsv);
	}
}

