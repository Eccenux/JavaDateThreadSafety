package pl.enux.javadatethreadsafety;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main class.
 * @author Maciej Nux Jaros
 */
public class Main {

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		for (int i = 0; i < 10; i++) {
			try {
				//testSimpleDateFormatSafety();
				//testCalendarWithFormatterSafety();
				testFormatterSafety();
			} catch (Exception ex) {
				Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	/**
	 * Test SimpleDateFormat.parse (non) thread-safety.
	 *
	 * Original code by `dogbane` (CC-BY-SA).
	 * @see https://stackoverflow.com/a/4021932/333296
	 * 
	 * @throws Exception
	 */
	private static void testSimpleDateFormatSafety() throws Exception {
		final DateFormat format = new SimpleDateFormat("yyyyMMdd");

		Callable<Date> task = new Callable<Date>() {
			@Override
			public Date call() throws Exception {
				return format.parse("20101022");
			}
		};

		//pool with 5 threads
		ExecutorService exec = Executors.newFixedThreadPool(5);
		List<Future<Date>> results = new ArrayList<>();

		//perform 10 date conversions
		for (int i = 0; i < 10; i++) {
			results.add(exec.submit(task));
		}
		exec.shutdown();

		//look at the results
		for (Future<Date> result : results) {
			System.out.println(result.get());
		}
	}

	/**
	 * Test GregorianCalendar with SimpleDateFormat.format (non) thread-safety.
	 *
	 * @throws Exception
	 */
	private static void testCalendarWithFormatterSafety() throws Exception {
		final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		final Calendar calendar = new GregorianCalendar(2013,1,28,13,24,56);
		String expected = "2013-02-28 13:24:56";

		Callable<String> task = new Callable<String>() {
			@Override
			public String call() throws Exception {
				return format.format(calendar.getTime());
			}
		};
		Callable<String> task2 = new Callable<String>() {
			@Override
			public String call() throws Exception {
				return String.format("%d-%02d-%d %d:%d:%d"
					, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH)+1, calendar.get(Calendar.DATE)
					, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND)
				);
			}
		};

		//pool with X threads
		// note that using more then CPU-threads will not give you a performance boost
		ExecutorService exec = Executors.newFixedThreadPool(10);
		List<Future<String>> results = new ArrayList<>();

		//perform some date conversions
		for (int i = 0; i < 10000; i++) {
			results.add(exec.submit(task));
			results.add(exec.submit(task2));
		}
		exec.shutdown();

		//look at the results
		for (Future<String> result : results) {
			String formatted = result.get();
			if (!expected.equals(formatted)) {
				System.out.println("formatted: " + formatted);
				System.out.println("expected: " + expected);
				throw new Exception("formatted != expected");
			}
		}
	}

	/**
	 * Test SimpleDateFormat.format (non) thread-safety.
	 *
	 * @throws Exception
	 */
	private static void testFormatterSafety() throws Exception {
		final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		final Calendar calendar1 = new GregorianCalendar(2013,1,28,13,24,56);
		final Calendar calendar2 = new GregorianCalendar(2014,1,28,13,24,56);
		String expected[] = {"2013-02-28 13:24:56", "2014-02-28 13:24:56"};

		Callable<String> task1 = new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "0#" + format.format(calendar1.getTime());
			}
		};
		Callable<String> task2 = new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "1#" + format.format(calendar2.getTime());
			}
		};

		//pool with X threads
		// note that using more then CPU-threads will not give you a performance boost
		ExecutorService exec = Executors.newFixedThreadPool(5);
		List<Future<String>> results = new ArrayList<>();

		//perform some date conversions
		for (int i = 0; i < 1000; i++) {
			results.add(exec.submit(task1));
			results.add(exec.submit(task2));
		}
		exec.shutdown();

		//look at the results
		for (Future<String> result : results) {
			String answer = result.get();
			String[] split = answer.split("#");
			Integer calendarNo = Integer.parseInt(split[0]);
			String formatted = split[1];
			if (!expected[calendarNo].equals(formatted)) {
				System.out.println("formatted: " + formatted);
				System.out.println("expected: " + expected[calendarNo]);
				System.out.println("answer: " + answer);
				throw new Exception("formatted != expected");
			/**
			} else {
				System.out.println("OK answer: " + answer);
			/**/
			}
		}
		System.out.println("OK: Loop finished");
	}
}
