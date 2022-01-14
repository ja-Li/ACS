/**
 * 
 */
package com.acertainbookstore.client.workloads;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreException;

/**
 * 
 * Worker represents the workload runner which runs the workloads with
 * parameters using WorkloadConfiguration and then reports the results
 * 
 */
public class Worker implements Callable<WorkerRunResult> {
    private WorkloadConfiguration configuration = null;
    private int numSuccessfulFrequentBookStoreInteraction = 0;
    private int numTotalFrequentBookStoreInteraction = 0;

    public Worker(WorkloadConfiguration config) {
	configuration = config;
    }

    /**
     * Run the appropriate interaction while trying to maintain the configured
     * distributions
     * 
     * Updates the counts of total runs and successful runs for customer
     * interaction
     * 
     * @param chooseInteraction
     * @return
     */
    private boolean runInteraction(float chooseInteraction) {
	try {
	    float percentRareStockManagerInteraction = configuration.getPercentRareStockManagerInteraction();
	    float percentFrequentStockManagerInteraction = configuration.getPercentFrequentStockManagerInteraction();

	    if (chooseInteraction < percentRareStockManagerInteraction) {
		runRareStockManagerInteraction();
	    } else if (chooseInteraction < percentRareStockManagerInteraction
		    + percentFrequentStockManagerInteraction) {
		runFrequentStockManagerInteraction();
	    } else {
		numTotalFrequentBookStoreInteraction++;
		runFrequentBookStoreInteraction();
		numSuccessfulFrequentBookStoreInteraction++;
	    }
	} catch (BookStoreException ex) {
	    return false;
	}
	return true;
    }

    /**
     * Run the workloads trying to respect the distributions of the interactions
     * and return result in the end
     */
    public WorkerRunResult call() throws Exception {
	int count = 1;
	long startTimeInNanoSecs = 0;
	long endTimeInNanoSecs = 0;
	int successfulInteractions = 0;
	long timeForRunsInNanoSecs = 0;

	Random rand = new Random();
	float chooseInteraction;

	// Perform the warmup runs
	while (count++ <= configuration.getWarmUpRuns()) {
	    chooseInteraction = rand.nextFloat() * 100f;
	    runInteraction(chooseInteraction);
	}

	count = 1;
	numTotalFrequentBookStoreInteraction = 0;
	numSuccessfulFrequentBookStoreInteraction = 0;

	// Perform the actual runs
	startTimeInNanoSecs = System.nanoTime();
	while (count++ <= configuration.getNumActualRuns()) {
	    chooseInteraction = rand.nextFloat() * 100f;
	    if (runInteraction(chooseInteraction)) {
		successfulInteractions++;
	    }
	}
	endTimeInNanoSecs = System.nanoTime();
	timeForRunsInNanoSecs += (endTimeInNanoSecs - startTimeInNanoSecs);
	return new WorkerRunResult(successfulInteractions, timeForRunsInNanoSecs, configuration.getNumActualRuns(),
		numSuccessfulFrequentBookStoreInteraction, numTotalFrequentBookStoreInteraction);
    }

    /**
     * Runs the new stock acquisition interaction
     * 
     * @throws BookStoreException
     */
    private void runRareStockManagerInteraction() throws BookStoreException {
	// TODO: Add code for New Stock Acquisition Interaction
		StockManager stockManager = configuration.getStockManager();
		List<StockBook> stockBookList = stockManager.getBooks();
		BookSetGenerator bookSetGenerator = configuration.getBookSetGenerator();
		Set<StockBook> bookSet = bookSetGenerator
				.nextSetOfStockBooks(configuration.getNumBooksToAdd());

		Set<StockBook> bookToAdd = bookSet.stream().filter(book1 -> stockBookList.stream()
				.allMatch(book2 -> book2.getISBN() != book1.getISBN())).collect(Collectors.toSet());

		stockManager.addBooks(bookToAdd);
    }

    /**
     * Runs the stock replenishment interaction
     * 
     * @throws BookStoreException
     */
    private void runFrequentStockManagerInteraction() throws BookStoreException {
	// TODO: Add code for Stock Replenishment Interaction
		StockManager stockManager = configuration.getStockManager();
		List<StockBook> stockBookList = stockManager.getBooks();

		stockManager.addCopies(stockBookList.stream().sequential()
				.sorted(Comparator.comparingInt(StockBook::getNumCopies))
				.limit(configuration.getNumBooksWithLeastCopies())
				.map(bookCopy -> new BookCopy(bookCopy.getISBN(), configuration.getNumAddCopies()))
				.collect(Collectors.toSet()));
    }

    /**
     * Runs the customer interaction
     * 
     * @throws BookStoreException
     */
    private void runFrequentBookStoreInteraction() throws BookStoreException {
	// TODO: Add code for Customer Interaction
		BookSetGenerator bookSetGenerator = configuration.getBookSetGenerator();
		BookStore bookStore = configuration.getBookStore();

		Set<Integer> booksISBN = bookStore.getEditorPicks(configuration.getNumEditorPicksToGet())
				.stream().map(Book::getISBN).collect(Collectors.toSet());

		List<Book> bookSampled = bookStore.getBooks(bookSetGenerator
				.sampleFromSetOfISBNs(booksISBN, configuration.getNumBooksToBuy()));

		Set<BookCopy> bookToBuy = new  HashSet<>();
		for (Book book : bookSampled){
			bookToBuy.add(new BookCopy(book.getISBN(), configuration.getNumBookCopiesToBuy()));
		}
		bookStore.buyBooks(bookToBuy);
	}

}
