/**
 * 
 */
package com.acertainbookstore.client.workloads;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

/**
 * 
 * CertainWorkload class runs the workloads by different workers concurrently.
 * It configures the environment for the workers using WorkloadConfiguration
 * objects and reports the metrics
 * 
 */
public class CertainWorkload {

	private static int SIZE_SET = 10;
    private static DecimalFormat df = new DecimalFormat("0.00");

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		int numConcurrentWorkloadThreads = 25;
		String serverAddress = "http://localhost:8081";
		boolean localTest = false;
		List<WorkerRunResult> workerRunResults = new ArrayList<WorkerRunResult>();
		List<Future<WorkerRunResult>> runResults = new ArrayList<Future<WorkerRunResult>>();

		// Initialize the RPC interfaces if its not a localTest, the variable is
		// overriden if the property is set
		String localTestProperty = System
				.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
		localTest = (localTestProperty != null) ? Boolean
				.parseBoolean(localTestProperty) : localTest;

		BookStore bookStore = null;
		StockManager stockManager = null;
		if (localTest) {
			CertainBookStore store = new CertainBookStore();
			bookStore = store;
			stockManager = store;
		} else {
			stockManager = new StockManagerHTTPProxy(serverAddress + "/stock");
			bookStore = new BookStoreHTTPProxy(serverAddress);
		}

		// Generate data in the bookstore before running the workload
		initializeBookStoreData(bookStore, stockManager);

		ExecutorService exec = Executors
				.newFixedThreadPool(numConcurrentWorkloadThreads);

		for (int i = 0; i < numConcurrentWorkloadThreads; i++) {
			WorkloadConfiguration config = new WorkloadConfiguration(bookStore,
					stockManager);
			Worker workerTask = new Worker(config);
			// Keep the futures to wait for the result from the thread
			runResults.add(exec.submit(workerTask));
		}

		// Get the results from the threads using the futures returned
		for (Future<WorkerRunResult> futureRunResult : runResults) {
			WorkerRunResult runResult = futureRunResult.get(); // blocking call
			workerRunResults.add(runResult);
		}

		exec.shutdownNow(); // shutdown the executor

		// Finished initialization, stop the clients if not localTest
		if (!localTest) {
			((BookStoreHTTPProxy) bookStore).stop();
			((StockManagerHTTPProxy) stockManager).stop();
		}

		reportMetric(workerRunResults);
	}

	/**
	 * Computes the metrics and prints them
	 * 
	 * @param workerRunResults
	 */
	public static void reportMetric(List<WorkerRunResult> workerRunResults) {
        long elapsedTotal = 0;
        int successfulRuns = 0;
        int totalRuns = 0;
        int clientInteractions = 0;
        int failedInteractions = 0;
        for (WorkerRunResult result : workerRunResults) {
            elapsedTotal = elapsedTotal + result.getElapsedTimeInNanoSecs();
            successfulRuns = successfulRuns + result.getSuccessfulInteractions();
            totalRuns = totalRuns + result.getTotalRuns();
            clientInteractions = clientInteractions + result.getSuccessfulFrequentBookStoreInteractionRuns();
            failedInteractions = failedInteractions + (result.getTotalRuns() - result.getSuccessfulInteractions());
        }
        long avgLatency = (elapsedTotal/successfulRuns)/1000000;
        int throughput = (int) ((int) (double)successfulRuns/((double)elapsedTotal/1000000000));
        System.out.println("Average latency: " + avgLatency + "ms");
        System.out.println("Throughput: " + throughput + "txs/s");
        double percentageCustomer = (100/(double)successfulRuns)*clientInteractions;
        double percentageFailed = (100/(double)totalRuns)*failedInteractions;
        System.out.println("Out of which " + df.format(percentageCustomer) + "% were customer interactions");
        System.out.println("And " + df.format(percentageFailed) + "% failed");
	}

	/**
	 * Generate the data in bookstore before the workload interactions are run
	 * 
	 * Ignores the serverAddress if its a localTest
	 * 
	 */
	public static void initializeBookStoreData(BookStore bookStore,
			StockManager stockManager) throws BookStoreException {

		stockManager.removeAllBooks();
		BookSetGenerator bookSetGenerator = new BookSetGenerator();
		// TODO: You should initialize data for your bookstore here
		Set<StockBook> booksToAdd = bookSetGenerator.nextSetOfStockBooks(SIZE_SET);
		stockManager.addBooks(booksToAdd);
		bookStore.getBooks(booksToAdd.stream().map(Book::getISBN).collect(Collectors.toSet()));
	}
}
