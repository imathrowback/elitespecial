package elitespecial;

import java.util.Arrays;
import javax.swing.table.DefaultTableModel;

import org.joda.time.DateTime;

public class EventTableModel extends DefaultTableModel
{
	static final Class[] columnTypes = new Class[] { DateTime.class, String.class, String.class, Boolean.class,
			String.class };
	static final String[] columnNames = new String[] { "Date/Time", "Body", "Body Type", "Landable", "Alert" };

	public static int getColumIndex(final String string)
	{
		return Arrays.asList(columnNames).indexOf(string);
	}

	public static EventTableModel getTableModel()
	{
		return new EventTableModel();
	}

	EventTableModel()
	{
		super(new Object[][] {}, columnNames);
	}

	@Override
	public Class getColumnClass(final int columnIndex)
	{
		return columnTypes[columnIndex];
	}

	@Override
	public boolean isCellEditable(final int row, final int column)
	{
		return false;
	}

	public void clear()
	{
		dataVector = convertToVector(new Object[][] {});
		fireTableDataChanged();

	}

}