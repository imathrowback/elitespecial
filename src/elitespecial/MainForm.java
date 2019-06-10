package elitespecial;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import org.joda.time.DateTime;

import com.google.common.eventbus.Subscribe;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainForm extends JFrame
{
	Thread scanThread;
	String journalDirectory = "";
	int days = 0;
	EliteSpecial es;

	private final JPanel contentPane;
	private final JTable table;
	private final JPanel panel;
	private final JLabel lblNewLabel;
	private final JTextField dateTextField;
	private final JLabel lblNewLabel_1;
	private final JTextField journalTextField;

	void setTableSizes()
	{
		Preferences prefs = Preferences.userNodeForPackage(MainForm.class);
		TableColumnModel cModel = table.getColumnModel();
		for (int i = 0; i < cModel.getColumnCount(); i++)
		{
			TableColumn cM = cModel.getColumn(i);
			String key = "c." + table.getColumnName(i) + ".size";
			int value = prefs.getInt(key, cM.getPreferredWidth());
			cM.setPreferredWidth(value);
			//			System.out.println("set " + key + " = " + cM.getPreferredWidth());
		}
	}

	static String getFrontierSavedGAmesDirectory()
	{
		String homeDir = System.getProperty("user.home");
		String savedGames = "Saved Games";

		// try to find saved games for frontier
		for (File f : Arrays.asList(new File(homeDir).listFiles()))
		{
			if (f.isDirectory())
			{
				Path test = Paths.get(homeDir, f.getName(), "Frontier Developments",
						"Elite Dangerous");
				if (test.toFile().exists())
				{
					savedGames = f.getName();
					break;
				}
			}
		}
		return Paths.get(homeDir, savedGames, "Frontier Developments",
				"Elite Dangerous").toString();
	}

	void readPreferences()
	{
		Preferences prefs = Preferences.userNodeForPackage(MainForm.class);

		journalDirectory = prefs.get("journalDirectory", getFrontierSavedGAmesDirectory());
		days = prefs.getInt("days", 365);

	}

	public void storePreferences()
	{
		Preferences prefs = Preferences.userNodeForPackage(MainForm.class);
		prefs.put("journalDirectory", journalDirectory);
		prefs.putInt("days", days);

		TableColumnModel cModel = table.getColumnModel();
		for (int i = 0; i < cModel.getColumnCount(); i++)
		{
			TableColumn cM = cModel.getColumn(i);
			String key = "c." + table.getColumnName(i) + ".size";
			Integer value = cM.getWidth();
			//System.out.println("put [" + key + "] = " + value);
			prefs.putInt(key, value);
		}

		try
		{
			prefs.flush();
		} catch (BackingStoreException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Launch the application.
	 */
	public static void main(final String[] args)
	{

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run()
			{
				try
				{
					UIManager.setLookAndFeel(
							UIManager.getSystemLookAndFeelClassName());
					MainForm frame = new MainForm();
					SpecialEventBus.eventBus.register(frame);

					frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
					frame.addWindowListener(new WindowAdapter() {
						@Override
						public void windowClosing(final WindowEvent e)
						{
							frame.storePreferences();
							frame.setVisible(false);
							frame.dispose();
						}
					});
					frame.setVisible(true);
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run()
						{
							frame.setTableSizes();
						}
					});
				} catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
	}

	public void addEvent(final EventData ed)
	{
		EventTableModel tm = (EventTableModel) table.getModel();

		// don't add if we already have one with the same date and body
		if (exists(ed, tm))
		{
			// no need to readd it
		} else
		{
			tm.addRow(new Object[] { ed.dateTime, ed.body, ed.bodyType, ed.landable, ed.text });
			table.changeSelection(table.getRowCount() - 1, 0, false, false);
		}
	}

	private boolean exists(final EventData ed, final EventTableModel tm)
	{
		boolean found = false;
		int bodyI = tm.findColumn("Body");
		int dateTimeI = tm.findColumn("Date/Time");

		for (int i = 0; i < tm.getRowCount(); i++)
		{
			if (ed.body.equals(tm.getValueAt(i, bodyI)) && ed.dateTime.equals(tm.getValueAt(i, dateTimeI)))
			{
				return true;
			}
		}
		return false;

	}

	public void clearEvents()
	{
		((EventTableModel) table.getModel()).clear();
		//table.setModel(EventTableModel.getTableModel());
	}

	Runnable scanRunnable = () -> {

		try
		{

			es.goHistoryMode(journalDirectory, days, (e) -> addEvent(e));
		} catch (Exception e)
		{
			e.printStackTrace();
			addEvent(new EventData(new DateTime(), "ERROR", "", e + "", false));
		}
	};

	private final JButton btnRescan;
	private final JLabel lblNewLabel_2;
	private final JButton browseFolderButton;
	private final JPanel panel_1;
	private final JLabel lblNewLabel_3;
	private final JTextField alertSearch;
	private String currentSystem;
	private DateTime currentSystemTime;
	private final JCheckBox currentSystemCheck;

	void beginScan()
	{
		clearEvents();

		if (scanThread != null)
		{
			es.stop();
			try
			{
				Thread.sleep(100);
				if (scanThread.isAlive())
					Thread.sleep(2000);
			} catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			scanThread.interrupt();

		}
		es = new EliteSpecial();

		scanThread = new Thread(scanRunnable);
		scanThread.setDaemon(true);
		scanThread.start();

	}

	String lastFilter;

	private void refreshSystemFilter(final String system)
	{

		//if (days == 0)
		//	system = "";

		TableRowSorter<EventTableModel> sorter = (TableRowSorter<EventTableModel>) table.getRowSorter();
		try
		{
			String filter = "";
			if (currentSystemCheck.isSelected())
				filter = system;
			/** avoid redundant calls to set row filter, it causes "exceptions" in the message pump if otherwise */
			if (filter.equals(lastFilter))
				return;
			lastFilter = filter;
			System.out.println("set body filter to '" + filter + "' [" + system + "]");

			sorter.setRowFilter(RowFilter.regexFilter(filter, EventTableModel.getColumIndex("Body")));
		} catch (Exception ex)
		{
			// ignore errors
		}

	}

	private void setAlertFilter(final String text)
	{
		TableRowSorter<EventTableModel> sorter = (TableRowSorter<EventTableModel>) table.getRowSorter();
		try
		{
			sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, EventTableModel.getColumIndex("Alert")));
		} catch (Exception ex)
		{
			// ignore errors
		}
	}

	/**
	 * Create the frame.
	 */
	public MainForm()
	{
		setTitle("Elite Special");

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		JScrollPane scrollPane = new JScrollPane();
		contentPane.add(scrollPane, BorderLayout.CENTER);

		//table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		EventTableModel tableModel = EventTableModel.getTableModel();

		TableRowSorter<EventTableModel> sorter = new TableRowSorter<>(tableModel);
		table = new JTable(tableModel);

		List<RowSorter.SortKey> sortKeys = new LinkedList<>();
		sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
		table.setRowSorter(sorter);

		scrollPane.setViewportView(table);

		readPreferences();

		panel = new JPanel();
		contentPane.add(panel, BorderLayout.NORTH);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[] { 112, 212, 0 };
		gbl_panel.rowHeights = new int[] { 20, 20, 0, 0, 0, 0 };
		gbl_panel.columnWeights = new double[] { 0.0, 1.0, 1.0 };
		gbl_panel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE };
		panel.setLayout(gbl_panel);

		lblNewLabel = new JLabel("Days old");
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.fill = GridBagConstraints.BOTH;
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		panel.add(lblNewLabel, gbc_lblNewLabel);

		dateTextField = new JTextField();
		dateTextField.setText(days + "");
		dateTextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(final KeyEvent e)
			{
				System.out.println("key typed in date field:" + dateTextField.getText());
				try
				{
					days = Integer.parseInt(dateTextField.getText());
					storePreferences();
				} catch (Exception ex)
				{
					dateTextField.setText(days + "");
				}
			}
		});

		GridBagConstraints gbc_dateTextField = new GridBagConstraints();
		gbc_dateTextField.fill = GridBagConstraints.BOTH;
		gbc_dateTextField.insets = new Insets(0, 0, 5, 5);
		gbc_dateTextField.gridx = 1;
		gbc_dateTextField.gridy = 0;
		panel.add(dateTextField, gbc_dateTextField);
		dateTextField.setColumns(10);

		currentSystemCheck = new JCheckBox("Current System");
		currentSystemCheck.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				refreshSystemFilter(currentSystem);
			}
		});
		GridBagConstraints gbc_currentSystemCheck = new GridBagConstraints();
		gbc_currentSystemCheck.insets = new Insets(0, 0, 5, 0);
		gbc_currentSystemCheck.gridx = 2;
		gbc_currentSystemCheck.gridy = 0;
		panel.add(currentSystemCheck, gbc_currentSystemCheck);

		lblNewLabel_1 = new JLabel("Journal Directory");
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.fill = GridBagConstraints.BOTH;
		gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 1;
		panel.add(lblNewLabel_1, gbc_lblNewLabel_1);

		journalTextField = new JTextField();
		journalTextField.setText(journalDirectory);
		journalTextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(final KeyEvent e)
			{
				System.out.println("key typed in journal field:" + journalTextField.getText());
				journalDirectory = (journalTextField.getText());
				storePreferences();
			}
		});
		GridBagConstraints gbc_journalTextField = new GridBagConstraints();
		gbc_journalTextField.anchor = GridBagConstraints.NORTH;
		gbc_journalTextField.insets = new Insets(0, 0, 5, 5);
		gbc_journalTextField.fill = GridBagConstraints.BOTH;
		gbc_journalTextField.gridx = 1;
		gbc_journalTextField.gridy = 1;
		panel.add(journalTextField, gbc_journalTextField);
		journalTextField.setColumns(10);

		btnRescan = new JButton("Restart Scan");
		btnRescan.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				EventQueue.invokeLater(new Runnable() {
					@Override
					public void run()
					{
						Path p = Paths.get(journalTextField.getText());
						if (!p.toFile().exists())
						{
							JOptionPane.showMessageDialog(MainForm.this, p.toString() + " is not a valid directory",
									"Invalid Directory",
									JOptionPane.ERROR_MESSAGE);
						} else
						{
							clearEvents();
							beginScan();
						}
					}
				});

			}
		});
		GridBagConstraints gbc_btnRescan = new GridBagConstraints();
		gbc_btnRescan.insets = new Insets(0, 0, 5, 5);
		gbc_btnRescan.gridx = 0;
		gbc_btnRescan.gridy = 2;
		panel.add(btnRescan, gbc_btnRescan);

		lblNewLabel_2 = new JLabel("...Automatic Refresh Enabled...");
		GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
		gbc_lblNewLabel_2.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_2.gridx = 1;
		gbc_lblNewLabel_2.gridy = 2;
		panel.add(lblNewLabel_2, gbc_lblNewLabel_2);

		browseFolderButton = new JButton("Browse...");
		browseFolderButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				JFileChooser chooser = new JFileChooser(System.getProperty("user.home"));
				chooser.setDialogTitle("Choose Frontier Games/Elite Dangerous directory");
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				if (chooser.showOpenDialog(browseFolderButton) == JFileChooser.APPROVE_OPTION)
				{
					journalDirectory = chooser.getSelectedFile().toString();
					journalTextField.setText(journalDirectory);
				}

			}
		});
		browseFolderButton.setVerticalAlignment(SwingConstants.TOP);
		GridBagConstraints gbc_browseFolderButton = new GridBagConstraints();
		gbc_browseFolderButton.fill = GridBagConstraints.BOTH;
		gbc_browseFolderButton.insets = new Insets(0, 0, 5, 0);
		gbc_browseFolderButton.gridx = 2;
		gbc_browseFolderButton.gridy = 1;
		panel.add(browseFolderButton, gbc_browseFolderButton);

		panel_1 = new JPanel();
		GridBagConstraints gbc_panel_1 = new GridBagConstraints();
		gbc_panel_1.insets = new Insets(0, 0, 5, 5);
		gbc_panel_1.fill = GridBagConstraints.BOTH;
		gbc_panel_1.gridx = 1;
		gbc_panel_1.gridy = 3;
		panel.add(panel_1, gbc_panel_1);

		lblNewLabel_3 = new JLabel("Alert search");
		panel_1.add(lblNewLabel_3);

		alertSearch = new JTextField();
		alertSearch.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(final KeyEvent e)
			{
				setAlertFilter(alertSearch.getText());
			}

		});
		panel_1.add(alertSearch);
		alertSearch.setColumns(10);

		if (new File(journalDirectory).isDirectory())
		{
			EventQueue.invokeLater(new Runnable() {
				@Override
				public void run()
				{
					beginScan();
				}
			});
		} else
		{
			JOptionPane.showMessageDialog(MainForm.this, journalDirectory + " is not a valid directory",
					"Invalid Directory",
					JOptionPane.ERROR_MESSAGE);
		}

	}

	@Subscribe
	public void onScanParseEvent(final ScanParse event)
	{
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run()
			{
				if (event.type.contains("ERROR"))
					addEvent(new EventData(new DateTime(), "ERROR", "", event.type, false));
				if (event.totalSize == 0)
					lblNewLabel_2.setText(event.type);
				else
					lblNewLabel_2.setText(event.type + " " + event.index + "/" + event.totalSize);
			}
		});
	}

	@Subscribe
	public void onEventDataEvent(final EventData event)
	{
		addEvent(event);

	}

	@Subscribe
	public void onSystemChange(final CurrentSystemEvent evt)
	{
		if (currentSystemTime == null || evt.time.isAfter(currentSystemTime))
		{
			currentSystem = evt.system;
			currentSystemTime = evt.time;
			refreshSystemFilter(evt.system);
		}

	}

}
