package elitespecial;

import javax.swing.table.DefaultTableModel;

public final class EventTableModel
{
	public static DefaultTableModel getTableModel()
	{
		return new DefaultTableModel(
				new Object[][] {
				},
				new String[] {
						"Date/Time", "Body", "Body Type", "Landable", "Alert"
				}) {
			Class[] columnTypes = new Class[] {
					Object.class, String.class, String.class, Boolean.class, String.class
			};

			@Override
			public Class getColumnClass(final int columnIndex)
			{
				return columnTypes[columnIndex];
			}

			boolean[] columnEditables = new boolean[] {
					false, false, false, false, false
			};

			@Override
			public boolean isCellEditable(final int row, final int column)
			{
				return columnEditables[column];
			}
		};
	}
}