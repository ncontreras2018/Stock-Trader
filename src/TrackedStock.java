import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;
import yahoofinance.quotes.stock.StockQuote;

public class TrackedStock implements Comparable<TrackedStock> {

	private Stock stock;
	private StockQuote currentStockQuote;
	private final String symbol;
	private String name;

	private int sharesOwned;

	public TrackedStock(String symbol, int sharesOwned) {
		this.symbol = symbol;
		this.sharesOwned = sharesOwned;

		try {
			stock = YahooFinance.get(symbol);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (stock != null) {
			currentStockQuote = stock.getQuote();
			name = stock.getName();
		}
	}

	public boolean isValidStock() {

		if (stock == null) {
			return false;
		}

		try {
			if (!stock.getCurrency().equals("USD")) {
				return false;
			}

			currentStockQuote = stock.getQuote(true);

			getPrice();
			getGrowthIndex();

		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public String getName() {
		return name;
	}

	public String getSymbol() {
		return symbol;
	}

	public Stock getStock() {
		return stock;
	}

	public double getPrice() {
		return currentStockQuote.getPrice().doubleValue();
	}

	public double getGrowthIndex() {
		return currentStockQuote.getChangeFromAvg50().doubleValue();
	}

	public double getVolatility() {
		return currentStockQuote.getChangeInPercent().doubleValue();
	}

	public int getNumOwnedShares() {
		return sharesOwned;
	}

	public void buyShares(int amount) {
		sharesOwned += amount;
	}

	public void sellShares(int amount) {
		sharesOwned -= amount;
	}

	public void setShares(int amount) {
		sharesOwned = amount;
	}

	public String toString() {
		String result = symbol;

		int spaces = 8 - symbol.length();

		for (int i = 0; i < spaces; i++) {
			result += "   ";
		}

		String growthString = getGrowthIndex() + "";

		result += growthString;

		String value = (getPrice() * sharesOwned) + "";

		spaces = 20 - growthString.length();

		for (int i = 0; i < spaces; i++) {
			result += "   ";
		}

		result += "$" + value;

		return result;
	}

	@Override
	public int compareTo(TrackedStock o) {
		return (int) Math.round((o.sharesOwned * o.getPrice() * 100) - (sharesOwned * getPrice() * 100));
	}

	public void update(Stock stock) {
		this.stock = stock;

		this.currentStockQuote = stock.getQuote();
	}
}
