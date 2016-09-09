import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;

public class StockTrader {

	public static final int MILLIS_TO_WAIT = 1000 * 60 * 1;

	private double money = 100000;
	public ArrayList<TrackedStock> trackedStocks;
	private int netWorth;

	private StockPanel panel;

	public static void main(String[] args) {

		new StockTrader();
	}

	private StockTrader() {

		String[] loadOptions = new String[] { "Start New Simulation", "Load From File" };

		int input = JOptionPane.showOptionDialog(null, "What Do You Want To Do?", "Starting Stock Sim", 0,
				JOptionPane.QUESTION_MESSAGE, null, loadOptions, null);

		trackedStocks = new ArrayList<TrackedStock>();

		if (loadOptions[input].equals("Load From File")) {
			loadDataFromFile();
		}

		panel = new StockPanel(this);

		generateNewStocks();

		beginTrading();
	}

	private void updateNetWorth() {
		netWorth = 0;

		for (int i = 0; i < trackedStocks.size(); i++) {

			TrackedStock curStock = trackedStocks.get(i);

			netWorth += curStock.getPrice() * curStock.getNumOwnedShares();
		}

		netWorth += money;

		panel.printToWindow("Net Worth: " + netWorth);
	}

	private long timeToReach;

	private void beginTrading() {

		while (true) {

			timeToReach = System.currentTimeMillis() + MILLIS_TO_WAIT;

			while (timeToReach > System.currentTimeMillis())
				;

			panel.printToWindow("-----------------------------------------------");

			synchronized (trackedStocks) {

				panel.printToWindow("***BEGINING CALULATIONS*** Num of Stocks: " + trackedStocks.size());

				updateAllStocks();

				while (trackedStocks.size() > 50) {

					TrackedStock cheapest = trackedStocks.get(0);

					for (int i = 0; i < trackedStocks.size(); i++) {
						TrackedStock curStock = trackedStocks.get(i);

						if (curStock.getPrice() < cheapest.getPrice()) {
							cheapest = curStock;
						}
					}

					if (cheapest.getNumOwnedShares() > 0) {
						money += getCashChange(cheapest, -cheapest.getNumOwnedShares());
						reportTransaction(cheapest, -cheapest.getNumOwnedShares());
						cheapest.sellShares(cheapest.getNumOwnedShares());
					}
					trackedStocks.remove(cheapest);
				}

				for (int i = 0; i < trackedStocks.size(); i++) {

					TrackedStock curStock = trackedStocks.get(i);

					if (curStock.getGrowthIndex() <= 0) {
						if (curStock.getNumOwnedShares() > 0) {
							money += getCashChange(curStock, -curStock.getNumOwnedShares());
							reportTransaction(curStock, -curStock.getNumOwnedShares());
							curStock.sellShares(curStock.getNumOwnedShares());
						}
						trackedStocks.remove(curStock);
						i--;
					}
				}

				updateNetWorth();

				double avgMarketGrowth = 0;

				for (int i = 0; i < trackedStocks.size(); i++) {
					avgMarketGrowth += trackedStocks.get(i).getGrowthIndex();
				}

				avgMarketGrowth /= trackedStocks.size();

				double marketGrowthNormalizationFactor = 1 / avgMarketGrowth;

				double normalizedAssetsPerStock = netWorth / trackedStocks.size();

				for (int i = 0; i < trackedStocks.size(); i++) {

					TrackedStock curStock = trackedStocks.get(i);

					double normalizedGrowthFactor = curStock.getGrowthIndex() * marketGrowthNormalizationFactor;

					double amountToInvest = normalizedGrowthFactor * normalizedAssetsPerStock;

					int sharesToOwn = (int) (amountToInvest / curStock.getPrice());

					int shareDifference = sharesToOwn - curStock.getNumOwnedShares();

					if (shareDifference != 0) {
						money += getCashChange(curStock, shareDifference);
						reportTransaction(curStock, shareDifference);
						curStock.setShares(sharesToOwn);
					}
				}
			}
		}
	}

	public int getTimeUnillUpdate() {
		return (int) (timeToReach - System.currentTimeMillis());
	}

	private void reportTransaction(TrackedStock stock, int sharesToBuy) {

		String report = "";

		if (sharesToBuy > 0) {
			report += "Bought ";
		} else if (sharesToBuy < 0) {
			report += "Sold ";
		}

		report += Math.abs(sharesToBuy) + " ";

		report += "share of stock ";

		report += stock.getName() + " (" + stock.getSymbol() + ")";

		report += " at price " + stock.getPrice() + " New balence: " + money;

		panel.printToWindow(report);
	}

	private double getCashChange(TrackedStock stock, int amount) {
		return stock.getPrice() * -amount;
	}

	private void loadDataFromFile() {

		JFileChooser fileChooser = new JFileChooser();
		fileChooser.showOpenDialog(null);

		File selection = fileChooser.getSelectedFile();

		try {
			FileReader fileReader = new FileReader(selection);

			BufferedReader reader = new BufferedReader(fileReader);

			String curLine = reader.readLine();

			money = Double.parseDouble(curLine);
			
			System.out.println("New money: " + money);

			curLine = reader.readLine();

			while (curLine != null) {
				int sharesOwned = Integer.parseInt(curLine.substring(curLine.indexOf(":") + 1));

				trackedStocks.add(new TrackedStock(curLine.substring(0, curLine.indexOf(":")), sharesOwned));
				
				curLine = reader.readLine();
			}

			reader.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void saveDataToFile() {

		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.showSaveDialog(null);

		File saveDir = fileChooser.getSelectedFile();

		String fileName = JOptionPane.showInputDialog("Enter Save Name:");

		String fullName;
		try {

			fullName = saveDir.getCanonicalPath() + File.separator + fileName + ".stksim";

			System.out.println("Full name: " + fullName);
			
			FileWriter fileWriter = new FileWriter(fullName);
			BufferedWriter writer = new BufferedWriter(fileWriter);

			writer.write(money + "");

			writer.newLine();

			synchronized (trackedStocks) {
				for (int i = 0; i < trackedStocks.size(); i++) {

					TrackedStock curStock = trackedStocks.get(i);

					writer.write(curStock.getSymbol() + ":" + curStock.getNumOwnedShares());
					writer.newLine();
				}
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void generateNewStocks() {

		new Thread(new Runnable() {

			@Override
			public void run() {

				while (true) {

					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					String newTicker = getRandomStockSymbol();

					boolean foundMatch = false;

					for (int i = 0; i < trackedStocks.size(); i++) {
						if (trackedStocks.get(i).getSymbol().equals(newTicker)) {
							foundMatch = true;
							break;
						}
					}

					if (foundMatch) {
						continue;
					}

					TrackedStock newStock = new TrackedStock(newTicker, 0);

					if (!newStock.isValidStock()) {
						continue;
					}

					synchronized (trackedStocks) {
						trackedStocks.add(newStock);
					}

					try {
						Thread.sleep(4000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	private static String getRandomStockSymbol() {
		int symbolLength = (int) (Math.random() * 5) + 1;

		String symbol = "";

		for (int i = 0; i < symbolLength; i++) {
			symbol += (char) ((Math.random() * 26) + 65);
		}

		return symbol;
	}

	public int getNetWorth() {
		return netWorth;
	}

	public int getCash() {
		return (int) money;
	}

	private void updateAllStocks() {

		String[] stocksToUpdate = new String[trackedStocks.size()];

		for (int i = 0; i < trackedStocks.size(); i++) {
			stocksToUpdate[i] = trackedStocks.get(i).getSymbol();
		}

		try {
			Map<String, Stock> stocks = YahooFinance.get(stocksToUpdate);

			for (int i = 0; i < stocksToUpdate.length; i++) {
				trackedStocks.get(i).update(stocks.get(stocksToUpdate[i]));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
