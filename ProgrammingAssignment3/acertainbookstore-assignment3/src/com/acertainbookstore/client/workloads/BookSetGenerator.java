package com.acertainbookstore.client.workloads;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;

/**
 * Helper class to generate stockbooks and isbns modelled similar to Random
 * class
 */
public class BookSetGenerator {

	private static int LATESTBOOKISBN;
	// Configuration for initial book generation
	private static int NUM_COPIES = 1000;
	private static int TITLE_LENGTH = 50;
	private static int AUTHOR_LENGTH = 10;

	//Generate random string
	private static String getRandomString(int length){
		String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < length; i++) {
			int choice = ThreadLocalRandom.current().nextInt(str.length());
			sb.append(str.charAt(choice));
		}
		return sb.toString();
	}

	public BookSetGenerator() {
		// TODO Auto-generated constructor stub
		LATESTBOOKISBN = ThreadLocalRandom.current()
				.ints(1, 1, 2000).findAny().getAsInt();
	}

	/**
	 * Returns num randomly selected isbns from the input set
	 * 
	 * @param num
	 * @return
	 */
	public Set<Integer> sampleFromSetOfISBNs(Set<Integer> isbns, int num) {
		ArrayList<Integer> list = new ArrayList<>();
		if (isbns.size() <= num) {
			return isbns;
		}
		else {
			isbns.iterator().forEachRemaining(list::add);
			Collections.shuffle(list);
			return new HashSet<>(list.subList(0,num));
		}
//		return null;
	}

	/**
	 * Return num stock books. For now return an ImmutableStockBook
	 * 
	 * @param num
	 * @return
	 */
	public Set<StockBook> nextSetOfStockBooks(int num) {
//		return null;
		String title, author;
		float price;
		long numSaleMisses, numTimesRated, totalRating;
		boolean editorPick;

		HashSet<StockBook> bookSet = new HashSet<>();
		for (int i = 0; i < num; i++) {
			LATESTBOOKISBN++;
			title = getRandomString(TITLE_LENGTH);
			author = getRandomString(AUTHOR_LENGTH);
			price = (float) (ThreadLocalRandom.current().nextFloat()*(1000 - 0.1)-0.2);
			numSaleMisses = ThreadLocalRandom.current().nextLong();
			numTimesRated = ThreadLocalRandom.current().nextLong();
			totalRating = ThreadLocalRandom.current().nextLong();
			editorPick = ThreadLocalRandom.current().nextBoolean();
			StockBook book = new ImmutableStockBook(LATESTBOOKISBN,
					title, author, price, NUM_COPIES,
					numSaleMisses, numTimesRated, totalRating, editorPick);
			bookSet.add(book);
		}
		return bookSet;
	}

}
