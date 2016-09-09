import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.DefaultCaret;

public class StockPanel extends JPanel implements ActionListener {

	private StockTrader t;

	private JFrame frame;

	private JTextArea output;

	private JList<TrackedStock> stockList;

	private JTextArea totalValue;

	private JButton saveButton;
	
	private JTextField userTickerEnter;
	private JButton userTickerButton;

	public StockPanel(StockTrader t) {

		this.t = t;

		frame = new JFrame("Stock Trader");

		this.setLayout(new BorderLayout());
		this.setPreferredSize(new Dimension(1000, 600));

		frame.add(this);

		output = new JTextArea();
		output.setEditable(false);

		JScrollPane scrollPane = new JScrollPane(output);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		DefaultCaret caret = (DefaultCaret) output.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		this.add(scrollPane, BorderLayout.CENTER);

		stockList = new JList<TrackedStock>();
		JScrollPane scrollPane2 = new JScrollPane(stockList);
		scrollPane2.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		this.add(scrollPane2, BorderLayout.LINE_END);

		totalValue = new JTextArea();
		totalValue.setEditable(false);

		this.add(totalValue, BorderLayout.PAGE_START);
		
		JPanel bottomPanel = new JPanel();
		
		userTickerEnter = new JTextField();
		userTickerEnter.setColumns(20);
		bottomPanel.add(userTickerEnter, BorderLayout.PAGE_END);
		
		userTickerButton = new JButton("Sumbit Ticker Symbol");
		userTickerButton.addActionListener(this);
		bottomPanel.add(userTickerButton, BorderLayout.PAGE_END);
		
		saveButton = new JButton("Save Simulation State");
		saveButton.addActionListener(this);
		bottomPanel.add(saveButton, BorderLayout.PAGE_END);
		
		this.add(bottomPanel, BorderLayout.PAGE_END);

		frame.pack();

		frame.setVisible(true);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		new Timer(true).schedule(new TimerTask() {

			@Override
			public void run() {
				TrackedStock[] displayList = t.trackedStocks.toArray(new TrackedStock[t.trackedStocks.size()]);

				if (displayList != null) {
					Arrays.sort(displayList);
					stockList.setListData(displayList);
				}

				int secTillTrade = t.getTimeUnillUpdate() / 1000;

				totalValue.setText("Net Worth: $" + t.getNetWorth() + "\nCash: $" + t.getCash() + "\nNext Trade In: "
						+ secTillTrade + " Seconds");

				frame.repaint();
			}
		}, 0, 250);
	}

	public void printToWindow(String s) {
		output.setText(output.getText() + s + "\n");

		TrackedStock[] displayList = t.trackedStocks.toArray(new TrackedStock[t.trackedStocks.size()]);
		Arrays.sort(displayList);
		stockList.setListData(displayList);

		totalValue.setText("$" + t.getNetWorth());

		frame.repaint();
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getSource().equals(saveButton)) {
			System.out.println("button");
			t.saveDataToFile();
		}
		
		if (arg0.getSource().equals(userTickerButton)) {
			String ticker = userTickerEnter.getText();
			userTickerEnter.setText("");
			
			boolean foundMatch = false;

			for (int i = 0; i < t.trackedStocks.size(); i++) {
				if (t.trackedStocks.get(i).getSymbol().equals(ticker)) {
					foundMatch = true;
					break;
				}
			}

			if (foundMatch) {
				return;
			}

			TrackedStock newStock = new TrackedStock(ticker, 0);

			if (!newStock.isValidStock()) {
				return;
			}

			synchronized (t.trackedStocks) {
				t.trackedStocks.add(newStock);
			}
		}
	}
}
